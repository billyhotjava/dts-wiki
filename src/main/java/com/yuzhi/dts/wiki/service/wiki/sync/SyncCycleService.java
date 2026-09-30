package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncOutbox;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.domain.SyncState;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxStatus;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import com.yuzhi.dts.wiki.domain.enumeration.SyncRunStatus;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.repository.SyncConflictRepository;
import com.yuzhi.dts.wiki.repository.SyncOutboxRepository;
import com.yuzhi.dts.wiki.repository.SyncRootRepository;
import com.yuzhi.dts.wiki.repository.SyncStateRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One atomic sync cycle per space (design 04 S4, v1.1 checkpoint rules):
 * fetch → inbound → reset-hard → outbound → push → atomic confirm.
 * A push rejection never advances the checkpoint: re-fetch, full inbound on the new
 * interval, rebuild from outbox, push again (≤3 tries, then next cycle).
 */
@Service
public class SyncCycleService {

    private static final Logger LOG = LoggerFactory.getLogger(SyncCycleService.class);
    private static final int PUSH_RETRIES = 3;

    private final GitRepoManager git;
    private final SpaceRepository spaceRepository;
    private final SyncRootRepository rootRepository;
    private final SyncStateRepository stateRepository;
    private final SyncOutboxRepository outboxRepository;
    private final SyncConflictRepository conflictRepository;
    private final PageRepository pageRepository;
    private final InboundSyncService inbound;
    private final OutboundSyncService outbound;

    public SyncCycleService(
        GitRepoManager git,
        SpaceRepository spaceRepository,
        SyncRootRepository rootRepository,
        SyncStateRepository stateRepository,
        SyncOutboxRepository outboxRepository,
        SyncConflictRepository conflictRepository,
        PageRepository pageRepository,
        InboundSyncService inbound,
        OutboundSyncService outbound
    ) {
        this.git = git;
        this.spaceRepository = spaceRepository;
        this.rootRepository = rootRepository;
        this.stateRepository = stateRepository;
        this.outboxRepository = outboxRepository;
        this.conflictRepository = conflictRepository;
        this.pageRepository = pageRepository;
        this.inbound = inbound;
        this.outbound = outbound;
    }

    public void cycleSpace(String slug) {
        Space space = spaceRepository.findOneBySlug(slug).orElse(null);
        if (space == null || space.getGitRepoUrl() == null || space.getGitRepoUrl().isBlank()) {
            return;
        }
        String branch = space.getGitBranch() == null ? "main" : space.getGitBranch();
        // W6: query roots fresh (inverse in-memory collections go stale within a session).
        List<SyncRoot> roots = rootRepository.findBySpaceWithMount(space.getId()).stream().filter(r -> Boolean.TRUE.equals(r.getEnabled())).toList();
        LOG.info("CYCLE-DBG {} branch={} roots={} url={}", slug, branch, roots.size(), space.getGitRepoUrl());
        if (roots.isEmpty()) {
            return;
        }
        try {
            ensureClone(space, branch);
            cycle(space, branch, roots);
        } catch (GitCommandException e) {
            markAll(space, roots, SyncRunStatus.OFFLINE, e.getMessage());
            LOG.warn("Sync offline for {}: {}", slug, e.getMessage());
        } catch (RuntimeException e) {
            markAll(space, roots, SyncRunStatus.ERROR, e.getMessage());
            LOG.error("Sync error for {}", slug, e);
        }
    }

    @Transactional
    public void cycle(Space space, String branch, List<SyncRoot> roots) {
        String slug = space.getSlug();
        String lastSynced = checkpoint(roots);
        git.run(slug, List.of("fetch", "origin", branch), 60);
        setFetchTime(roots);
        String remoteHead = git.run(slug, List.of("rev-parse", "origin/" + branch), 30);

        InboundSyncService.InboundReport inboundReport = inbound.inbound(space, branch, lastSynced, remoteHead);
        LOG.info("Inbound {}: base={} complete={} applied={} conflicts={} skipped={} head={}", slug, lastSynced, inboundReport.complete(), inboundReport.applied(), inboundReport.conflicts(), inboundReport.skipped(), inboundReport.remoteHead());
        if (!inboundReport.complete()) {
            markAll(space, roots, SyncRunStatus.ERROR, "Inbound incomplete, checkpoint held at " + lastSynced);
            return;
        }
        // Rebuild the workdir from the absorbed remote: every local commit is an outbox
        // replay, so discarding unpushed commits is safe (v1.1: only after inbound).
        git.run(slug, List.of("reset", "--hard", "origin/" + branch), 60);

        List<SyncOutbox> materialized = outbound.materializePending(space, branch);
        for (int attempt = 0; attempt < PUSH_RETRIES; attempt++) {
            try {
                git.run(slug, List.of("push", "origin", "HEAD:" + branch), 60);
                confirm(space, roots, remoteHead, materialized);
                setPushTime(roots);
                return;
            } catch (GitCommandException e) {
                if (!isPushRejection(e.getMessage()) || attempt == PUSH_RETRIES - 1) {
                    if (isPushRejection(e.getMessage())) {
                        LOG.warn("Push rejected {} times for {}, deferring to next cycle", PUSH_RETRIES, slug);
                    }
                    markAll(space, roots, SyncRunStatus.PUSH_FAILED, e.getMessage());
                    return;
                }
                LOG.info("Push rejected for {}, re-fetch + full inbound (attempt {}/{})", slug, attempt + 1, PUSH_RETRIES);
                git.run(slug, List.of("fetch", "origin", branch), 60);
                String newHead = git.run(slug, List.of("rev-parse", "origin/" + branch), 30);
                InboundSyncService.InboundReport retry = inbound.inbound(space, branch, remoteHead, newHead);
                if (!retry.complete()) {
                    markAll(space, roots, SyncRunStatus.ERROR, "Re-inbound incomplete, checkpoint held");
                    return;
                }
                remoteHead = newHead;
                git.run(slug, List.of("reset", "--hard", "origin/" + branch), 60);
                materialized = outbound.materializePending(space, branch);
            }
        }
    }

    private void confirm(Space space, List<SyncRoot> roots, String inboundBase, List<SyncOutbox> materialized) {
        String slug = space.getSlug();
        String head = git.head(slug);
        List<Long> pushedIds = materialized.stream().map(SyncOutbox::getId).toList();
        outbound.confirmPushed(pushedIds);
        for (SyncOutbox entry : materialized) {
            if (entry.getPage() == null || entry.getPage().getId() == null) {
                continue;
            }
            pageRepository
                .findById(entry.getPage().getId())
                .ifPresent(page -> {
                    if (page.getSyncStatus() != PageSyncStatus.PENDING_PUSH) {
                        return;
                    }
                    boolean hasPending = !outboxRepository
                        .findBySpaceIdAndStatusOrderByIdAsc(space.getId(), OutboxStatus.PENDING)
                        .stream()
                        .filter(o -> o.getPage() != null && o.getPage().getId().equals(page.getId()))
                        .toList()
                        .isEmpty()
                        || !outboxRepository
                            .findBySpaceIdAndStatusOrderByIdAsc(space.getId(), OutboxStatus.BLOCKED_BY_CONFLICT)
                            .stream()
                            .filter(o -> o.getPage() != null && o.getPage().getId().equals(page.getId()))
                            .toList()
                            .isEmpty();
                    boolean conflictOpen = !conflictRepository.findByPageIdAndResolvedAtIsNull(page.getId()).isEmpty();
                    if (!hasPending && !conflictOpen) {
                        page.setSyncStatus(PageSyncStatus.SYNCED);
                    }
                });
        }
        // v1.1: checkpoint advances only now — HEAD is the absorbed base plus our pushed batch.
        for (SyncRoot root : roots) {
            stateRepository
                .findOneBySyncRootId(root.getId())
                .ifPresent(state -> {
                    state.setLastSyncedCommit(head);
                    state.setStatus(SyncRunStatus.OK);
                    state.setMessage(null);
                });
        }
        LOG.debug("Sync confirmed for {} at {} (base {})", slug, head, inboundBase);
    }

    private boolean isPushRejection(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("rejected") || lower.contains("non-fast-forward") || lower.contains("fetch first");
    }

    private String checkpoint(List<SyncRoot> roots) {
        for (SyncRoot root : roots) {
            var state = stateRepository.findOneBySyncRootId(root.getId());
            if (state.isPresent() && state.get().getLastSyncedCommit() != null) {
                return state.get().getLastSyncedCommit();
            }
        }
        return null;
    }

    private void setFetchTime(List<SyncRoot> roots) {
        for (SyncRoot root : roots) {
            stateRepository
                .findOneBySyncRootId(root.getId())
                .ifPresentOrElse(
                    state -> state.setLastFetchAt(Instant.now()),
                    () -> {
                        SyncState state = new SyncState();
                        state.setSyncRoot(root);
                        state.setStatus(SyncRunStatus.IDLE);
                        state.setLastFetchAt(Instant.now());
                        stateRepository.save(state);
                    }
                );
        }
    }

    private void setPushTime(List<SyncRoot> roots) {
        for (SyncRoot root : roots) {
            stateRepository.findOneBySyncRootId(root.getId()).ifPresent(state -> state.setLastPushAt(Instant.now()));
        }
    }

    private void markAll(Space space, List<SyncRoot> roots, SyncRunStatus status, String message) {
        for (SyncRoot root : roots) {
            stateRepository
                .findOneBySyncRootId(root.getId())
                .ifPresentOrElse(
                    state -> {
                        state.setStatus(status);
                        state.setMessage(message == null ? null : message.substring(0, Math.min(500, message.length())));
                    },
                    () -> {
                        SyncState state = new SyncState();
                        state.setSyncRoot(root);
                        state.setStatus(status);
                        state.setMessage(message == null ? null : message.substring(0, Math.min(500, message.length())));
                        stateRepository.save(state);
                    }
                );
        }
    }

    private void ensureClone(Space space, String branch) {
        if (git.hasClone(space.getSlug())) {
            return;
        }
        try {
            Path parent = git.repoDir(space.getSlug()).getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            git.runIn(
                parent == null ? Path.of(".") : parent,
                List.of("clone", "--branch", branch, "--single-branch", space.getGitRepoUrl(), git.repoDir(space.getSlug()).toString()),
                git.sshEnv(space.getSlug()),
                300
            );
        } catch (Exception e) {
            throw new IllegalStateException("Clone failed for space " + space.getSlug(), e);
        }
    }
}
