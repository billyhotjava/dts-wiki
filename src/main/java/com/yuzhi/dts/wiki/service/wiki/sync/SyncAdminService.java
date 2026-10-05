package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxStatus;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.repository.SyncConflictRepository;
import com.yuzhi.dts.wiki.repository.SyncOutboxRepository;
import com.yuzhi.dts.wiki.repository.SyncRootRepository;
import com.yuzhi.dts.wiki.repository.SyncStateRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sync administration backing /api/wiki/admin (design 03 S4, F4/T02 + F5/T06 admin part).
 * Deploy keys: one ed25519 keypair per space slug under the secrets dir; the public
 * half is shown in the UI for GitHub registration (write access required for push).
 */
@Service
public class SyncAdminService {

    private final GitRepoManager git;
    private final WikiProperties properties;
    private final SpaceRepository spaceRepository;
    private final SyncRootRepository rootRepository;
    private final SyncStateRepository stateRepository;
    private final SyncOutboxRepository outboxRepository;
    private final SyncConflictRepository conflictRepository;
    private final PageRepository pageRepository;
    private final GitSyncScheduler scheduler;
    private final ImportService importService;

    public SyncAdminService(
        GitRepoManager git,
        WikiProperties properties,
        SpaceRepository spaceRepository,
        SyncRootRepository rootRepository,
        SyncStateRepository stateRepository,
        SyncOutboxRepository outboxRepository,
        SyncConflictRepository conflictRepository,
        PageRepository pageRepository,
        GitSyncScheduler scheduler,
        ImportService importService
    ) {
        this.git = git;
        this.properties = properties;
        this.spaceRepository = spaceRepository;
        this.rootRepository = rootRepository;
        this.stateRepository = stateRepository;
        this.outboxRepository = outboxRepository;
        this.conflictRepository = conflictRepository;
        this.pageRepository = pageRepository;
        this.scheduler = scheduler;
        this.importService = importService;
    }

    public record RootStatus(String repoPath, Long mountPageId, boolean enabled, String lastSyncedCommit, String status) {}

    public record SpaceSyncStatus(
        String slug,
        String gitRepoUrl,
        String gitBranch,
        boolean cloned,
        boolean hasKey,
        List<RootStatus> roots,
        long pendingOutbox,
        long failedOutbox,
        long openConflicts,
        String message
    ) {}

    @Transactional(readOnly = true)
    public List<SpaceSyncStatus> statuses() {
        List<SpaceSyncStatus> out = new ArrayList<>();
        for (Space space : spaceRepository.findAll()) {
            List<RootStatus> roots = new ArrayList<>();
            String message = null;
            // W6: query roots fresh (inverse in-memory collections go stale within a session).
            for (SyncRoot root : rootRepository.findBySpaceWithMount(space.getId())) {
                var state = stateRepository.findOneBySyncRootId(root.getId());
                roots.add(
                    new RootStatus(
                        root.getRepoPath(),
                        root.getMountPage() == null ? null : root.getMountPage().getId(),
                        Boolean.TRUE.equals(root.getEnabled()),
                        state.map(s -> s.getLastSyncedCommit()).orElse(null),
                        state.map(s -> s.getStatus() == null ? null : s.getStatus().name()).orElse(null)
                    )
                );
                if (state.isPresent() && state.orElseThrow().getMessage() != null && message == null) {
                    message = state.orElseThrow().getMessage();
                }
            }
            out.add(
                new SpaceSyncStatus(
                    space.getSlug(),
                    space.getGitRepoUrl(),
                    space.getGitBranch(),
                    git.hasClone(space.getSlug()),
                    git.hasKey(space.getSlug()),
                    roots,
                    outboxRepository.countBySpaceIdAndStatus(space.getId(), OutboxStatus.PENDING),
                    outboxRepository.countBySpaceIdAndStatus(space.getId(), OutboxStatus.FAILED),
                    conflictRepository.countOpenBySpace(space.getId()),
                    message
                )
            );
        }
        return out;
    }

    public void runNow(String slug) {
        scheduler.syncSpace(slug);
    }

    /** Connectivity check: ls-remote plus push dry-run (design 04, T02 acceptance). */
    public String testConnection(String slug) {
        Space space = spaceRepository.findOneBySlug(slug).orElseThrow(() -> new IllegalArgumentException("No such space: " + slug));
        if (!git.hasKey(slug)) {
            throw new IllegalStateException("No deploy key for space " + slug + "; generate one first");
        }
        String branch = space.getGitBranch() == null ? "main" : space.getGitBranch();
        git.run(slug, List.of("ls-remote", space.getGitRepoUrl(), branch), 60);
        if (git.hasClone(slug)) {
            git.run(slug, List.of("push", "--dry-run", "origin", "HEAD:" + branch), 60);
        }
        return "OK";
    }

    /** Returns the existing public key, generating a keypair on first use. */
    public synchronized String publicKey(String slug) {
        Path key = git.keyPath(slug);
        Path pub = key.resolveSibling(key.getFileName() + ".pub");
        try {
            if (!Files.isRegularFile(pub)) {
                Files.createDirectories(key.getParent());
                ProcessBuilder builder = new ProcessBuilder("ssh-keygen", "-t", "ed25519", "-N", "", "-f", key.toAbsolutePath().toString(), "-C", "dts-wiki-" + slug);
                Process process = builder.start();
                int exit = process.waitFor();
                if (exit != 0) {
                    throw new IllegalStateException("ssh-keygen failed with exit " + exit);
                }
                Files.setPosixFilePermissions(key, PosixFilePermissions.fromString("rw-------"));
            }
            return Files.readString(pub, StandardCharsets.UTF_8).strip();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot provide deploy key for " + slug, e);
        }
    }

    @Transactional
    public Space createSpace(String slug, String name, String description, String gitRepoUrl, String gitBranch, List<RootSpec> roots) {
        if (spaceRepository.findOneBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("Space already exists: " + slug);
        }
        if (!slug.matches("^[a-z][a-z0-9-]{1,30}$")) {
            throw new IllegalArgumentException("Bad slug: " + slug);
        }
        Space space = new Space();
        space.setSlug(slug);
        space.setName(name);
        space.setDescription(description);
        space.setGitRepoUrl(gitRepoUrl);
        space.setGitBranch(gitBranch == null ? "main" : gitBranch);
        space.setArchived(false);
        space = spaceRepository.save(space);
        Page rootPage = new Page();
        rootPage.setSpace(space);
        rootPage.setTitle(name);
        rootPage.setKind(PageKind.FOLDER);
        rootPage.setPosition(1000);
        rootPage.setSyncStatus(PageSyncStatus.LOCAL_ONLY);
        rootPage.setCreatedAt(Instant.now());
        rootPage.setUpdatedAt(Instant.now());
        rootPage = pageRepository.save(rootPage);
        for (RootSpec spec : roots == null ? List.<RootSpec>of() : roots) {
            SyncRoot root = new SyncRoot();
            root.setSpace(space);
            root.setRepoPath(spec.repoPath());
            root.setEnabled(true);
            root.setMountPage(rootPage);
            rootRepository.save(root);
        }
        return space;
    }

    public record RootSpec(String repoPath) {}

    public ImportService.ImportReport importSpace(String slug, boolean importHistory) {
        return importService.importSpace(slug, importHistory, 20);
    }
}
