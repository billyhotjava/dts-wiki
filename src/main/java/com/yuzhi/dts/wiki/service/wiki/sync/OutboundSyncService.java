package com.yuzhi.dts.wiki.service.wiki.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Attachment;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncOutbox;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxOp;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxStatus;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import com.yuzhi.dts.wiki.repository.AttachmentRepository;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import com.yuzhi.dts.wiki.repository.SyncOutboxRepository;
import com.yuzhi.dts.wiki.service.wiki.BlobStore;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * wiki → git outbound (design 04 S6, v1.1 checkpoint rules).
 * Every outbox entry becomes one commit with a stable {@code Wiki-Operation-Id} trailer
 * ({@code outbox-<id>}); the trailer makes crash recovery idempotent (a push that
 * succeeded but was never confirmed is detected on the remote instead of replayed).
 */
@Service
public class OutboundSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(OutboundSyncService.class);

    private final GitRepoManager git;
    private final WikiProperties properties;
    private final SyncOutboxRepository outboxRepository;
    private final PageRepository pageRepository;
    private final PageVersionRepository versionRepository;
    private final AttachmentRepository attachmentRepository;
    private final BlobStore blobStore;
    private final ObjectMapper objectMapper;
    private final com.yuzhi.dts.wiki.repository.SyncRootRepository rootRepository;

    public OutboundSyncService(
        GitRepoManager git,
        WikiProperties properties,
        SyncOutboxRepository outboxRepository,
        PageRepository pageRepository,
        PageVersionRepository versionRepository,
        AttachmentRepository attachmentRepository,
        BlobStore blobStore,
        ObjectMapper objectMapper,
        com.yuzhi.dts.wiki.repository.SyncRootRepository rootRepository
    ) {
        this.git = git;
        this.properties = properties;
        this.outboxRepository = outboxRepository;
        this.pageRepository = pageRepository;
        this.versionRepository = versionRepository;
        this.attachmentRepository = attachmentRepository;
        this.blobStore = blobStore;
        this.objectMapper = objectMapper;
        this.rootRepository = rootRepository;
    }

    public record OutboundReport(int committed, int done, int failed, boolean pushed) {}

    @Transactional
    public OutboundReport drain(Space space, String branch) {
        List<SyncOutbox> materialized = materializePending(space, branch);
        for (SyncOutbox entry : materialized) {
            markDone(entry);
        }
        return new OutboundReport(materialized.size(), materialized.size(), 0, false);
    }

    /**
     * Replay all PENDING entries into local commits WITHOUT marking them done.
     * The caller (push flow) confirms only after a verified push, so a crash between
     * local commit and DB confirm never loses work and never double-pushes
     * (recovery via {@code Wiki-Operation-Id} lookup on the remote).
     */
    @Transactional
    public List<SyncOutbox> materializePending(Space space, String branch) {
        if (!properties.isOutboundEnabled() || space.isManifestManaged()) { return List.of(); }
        List<SyncOutbox> pending = outboxRepository.findBySpaceIdAndStatusOrderByIdAsc(space.getId(), OutboxStatus.PENDING);
        List<SyncOutbox> materialized = new ArrayList<>();
        for (SyncOutbox entry : pending) {
            try {
                if (materialize(space, branch, entry)) {
                    materialized.add(entry);
                } else {
                    markDone(entry);
                    materialized.add(entry);
                }
            } catch (GitCommandException | IOException | IllegalArgumentException e) {
                fail(entry, e.getMessage());
            } catch (RuntimeException e) {
                fail(entry, e.getMessage());
            }
        }
        return materialized;
    }

    @Transactional
    public void confirmPushed(List<Long> outboxIds) {
        for (Long id : outboxIds) {
            outboxRepository.findById(id).ifPresent(this::markDone);
        }
    }

    /**
     * @return true when a new local commit was created, false when the remote already
     * satisfies the operation (idempotent confirm, no duplicate commit).
     */
    private boolean materialize(Space space, String branch, SyncOutbox entry) throws IOException {
        String slug = space.getSlug();
        String opId = "outbox-" + entry.getId();
        if (isAlreadyPushed(slug, branch, opId)) {
            return false;
        }
        Map<String, String> payload = payload(entry);
        String gitPath = payload.get("gitPath");
        if (gitPath == null || gitPath.isBlank() || gitPath.equals("null")) {
            throw new IllegalArgumentException("Outbox entry has no gitPath: " + entry.getId());
        }
        assertInRoots(space, gitPath);
        Path workdir = git.repoDir(slug);
        switch (entry.getOp()) {
            case WRITE, RESTORE -> writeVersionFile(space, entry, payload, gitPath, workdir);
            case MOVE -> movePath(space, entry, payload, gitPath, workdir);
            case DELETE -> deletePath(space, entry, payload, gitPath, workdir);
            case ATTACH -> writeAttachmentFile(space, entry, payload, gitPath, workdir);
            case DETACH -> deletePath(space, entry, payload, gitPath, workdir);
            default -> throw new IllegalArgumentException("Unknown outbox op: " + entry.getOp());
        }
        String message = subject(entry) + "\n\nWiki-Operation-Id: " + opId + actorTrailer(entry);
        Map<String, String> env = Map.of(
            "GIT_AUTHOR_NAME",
            displayName(entry),
            "GIT_AUTHOR_EMAIL",
            emailOf(entry),
            "GIT_COMMITTER_NAME",
            properties.getCommitterName(),
            "GIT_COMMITTER_EMAIL",
            properties.getCommitterEmail()
        );
        git.run(slug, List.of("add", "-A", "--", gitPath), env, 60);
        String status = git.run(slug, List.of("status", "--porcelain", "--", gitPath), Map.of(), 30);
        if (status.isBlank()) {
            return false;
        }
        git.run(slug, List.of("commit", "-m", message), env, 60);
        String commit = git.run(slug, List.of("rev-parse", "HEAD"), Map.of(), 30);
        payload.put("commit", commit);
        entry.setPayload(toJson(payload));
        return true;
    }

    private void writeVersionFile(Space space, SyncOutbox entry, Map<String, String> payload, String gitPath, Path workdir) throws IOException {
        Long versionId = payload.containsKey("versionId") ? Long.valueOf(payload.get("versionId")) : null;
        PageVersion version = versionId == null ? currentVersion(entry) : versionRepository.findById(versionId).orElse(null);
        if (version == null) {
            throw new IllegalArgumentException("Version gone for outbox entry " + entry.getId());
        }
        Path target = workdir.resolve(gitPath);
        Files.createDirectories(target.getParent());
        Files.writeString(target, version.getContentMd() == null ? "" : version.getContentMd(), StandardCharsets.UTF_8);
    }

    private void movePath(Space space, SyncOutbox entry, Map<String, String> payload, String gitPath, Path workdir) {
        String fromPath = payload.get("fromPath");
        if (fromPath == null || fromPath.isBlank() || fromPath.equals("null")) {
            throw new IllegalArgumentException("MOVE entry has no fromPath: " + entry.getId());
        }
        assertInRoots(space, fromPath);
        git.run(space.getSlug(), List.of("mv", fromPath, gitPath), Map.of(), 60);
    }

    private void deletePath(Space space, SyncOutbox entry, Map<String, String> payload, String gitPath, Path workdir) {
        git.run(space.getSlug(), List.of("rm", "-r", "--ignore-unmatch", gitPath), Map.of(), 60);
    }

    private void writeAttachmentFile(Space space, SyncOutbox entry, Map<String, String> payload, String gitPath, Path workdir) throws IOException {
        String attachmentId = payload.get("attachmentId");
        Attachment attachment = attachmentId == null
            ? null
            : attachmentRepository.findById(Long.valueOf(attachmentId)).orElse(null);
        if (attachment == null || attachment.getDeletedAt() != null) {
            throw new IllegalArgumentException("Attachment gone for outbox entry " + entry.getId());
        }
        Path target = workdir.resolve(gitPath);
        Files.createDirectories(target.getParent());
        try (var in = blobStore.load(attachment.getSha256())) {
            Files.copy(in, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private PageVersion currentVersion(SyncOutbox entry) {
        if (entry.getPage() == null) {
            return null;
        }
        return pageRepository.findById(entry.getPage().getId()).map(Page::getCurrentVersion).orElse(null);
    }

    private boolean isAlreadyPushed(String slug, String branch, String opId) {
        try {
            String found = git.run(slug, List.of("log", "origin/" + branch, "--grep=^Wiki-Operation-Id: " + opId + "$", "--format=%H"), Map.of(), 30);
            return !found.isBlank();
        } catch (GitCommandException e) {
            return false;
        }
    }

    private void assertInRoots(Space space, String gitPath) {
        for (SyncRoot root : rootRepository.findBySpaceWithMount(space.getId())) {
            if (!Boolean.TRUE.equals(root.getEnabled())) {
                continue;
            }
            String prefix = root.getRepoPath().replaceAll("/+$", "");
            if (gitPath.equals(prefix) || gitPath.startsWith(prefix + "/")) {
                return;
            }
        }
        throw new IllegalArgumentException("Path outside sync roots: " + gitPath);
    }

    private String subject(SyncOutbox entry) {
        String op = switch (entry.getOp()) {
            case WRITE -> "edit";
            case MOVE -> "move";
            case DELETE -> "delete";
            case RESTORE -> "restore";
            case ATTACH -> "upload";
            case DETACH -> "remove";
        };
        Page page = entry.getPage() == null ? null : pageRepository.findById(entry.getPage().getId()).orElse(null);
        String path = page == null || page.getGitPath() == null ? "(unknown path)" : page.getGitPath();
        return "wiki: " + op + " " + path;
    }

    private String displayName(SyncOutbox entry) {
        if (entry.getActorName() != null && !entry.getActorName().isBlank()) {
            return entry.getActorName();
        }
        return entry.getActorLogin() == null ? properties.getCommitterName() : entry.getActorLogin();
    }

    private String emailOf(SyncOutbox entry) {
        if (entry.getActorEmail() != null && !entry.getActorEmail().isBlank()) {
            return entry.getActorEmail();
        }
        String login = entry.getActorLogin() == null ? "wiki" : entry.getActorLogin();
        return login + "@users.noreply.yuzhicloud.com";
    }

    private String actorTrailer(SyncOutbox entry) {
        Map<String, String> payload = payload(entry);
        String via = payload.get("viaAgent");
        if (via == null || via.isBlank()) {
            return "";
        }
        return " (via " + via + ")";
    }

    private void markDone(SyncOutbox entry) {
        entry.setStatus(OutboxStatus.DONE);
        entry.setProcessedAt(Instant.now());
    }

    private void fail(SyncOutbox entry, String error) {
        entry.setAttempts(entry.getAttempts() == null ? 1 : entry.getAttempts() + 1);
        entry.setLastError(error == null ? "unknown" : error.substring(0, Math.min(500, error.length())));
        if (entry.getAttempts() >= 10) {
            entry.setStatus(OutboxStatus.FAILED);
            LOG.error("Outbox entry {} failed permanently: {}", entry.getId(), entry.getLastError());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> payload(SyncOutbox entry) {
        try {
            Map<String, Object> raw = objectMapper.readValue(entry.getPayload(), Map.class);
            Map<String, String> normalized = new HashMap<>();
            for (var item : raw.entrySet()) {
                Object value = item.getValue();
                if (value == null) { continue; }
                if (!(value instanceof String || value instanceof Number || value instanceof Boolean)) { throw new IllegalArgumentException("Invalid outbox field"); }
                normalized.put(item.getKey(), value.toString());
            }
            return normalized;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Bad outbox payload: " + entry.getId());
        }
    }

    private String toJson(Map<String, String> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
