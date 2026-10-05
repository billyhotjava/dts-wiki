package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.SyncConflict;
import com.yuzhi.dts.wiki.domain.enumeration.ConflictResolution;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxOp;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxStatus;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import com.yuzhi.dts.wiki.domain.enumeration.VersionSource;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import com.yuzhi.dts.wiki.repository.SyncConflictRepository;
import com.yuzhi.dts.wiki.repository.SyncOutboxRepository;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sync conflict query + manual resolution (design 04 S7). Auto-merge itself
 * ({@link #merge}) is pure git merge-file.
 */
@Service
public class ConflictService {

    private final SyncConflictRepository conflictRepository;
    private final PageRepository pageRepository;
    private final PageVersionRepository versionRepository;
    private final SyncOutboxRepository outboxRepository;
    private final PageService pageService;
    private final com.yuzhi.dts.wiki.service.wiki.PageWritePolicy writePolicy;
    private final com.yuzhi.dts.wiki.service.wiki.SpaceAccessService access;

    public ConflictService(
        SyncConflictRepository conflictRepository,
        PageRepository pageRepository,
        PageVersionRepository versionRepository,
        SyncOutboxRepository outboxRepository,
        PageService pageService,
        com.yuzhi.dts.wiki.service.wiki.PageWritePolicy writePolicy,
        com.yuzhi.dts.wiki.service.wiki.SpaceAccessService access
    ) {
        this.conflictRepository = conflictRepository;
        this.pageRepository = pageRepository;
        this.versionRepository = versionRepository;
        this.outboxRepository = outboxRepository;
        this.pageService = pageService;
        this.writePolicy = writePolicy;
        this.access = access;
    }

    public record MergeResult(boolean clean, String merged) {}

    public record ConflictView(Long id, Long pageId, String pageTitle, String base, String wiki, String git, String gitCommit, Instant detectedAt) {}

    /** Three-way merge via {@code git merge-file}. Exit 0 means clean. */
    public static MergeResult merge(String base, String wiki, String git) throws IOException {
        Path dir = Files.createTempDirectory("wikimerge");
        try {
            Path baseFile = Files.writeString(dir.resolve("base.md"), base, StandardCharsets.UTF_8);
            Path wikiFile = Files.writeString(dir.resolve("wiki.md"), wiki, StandardCharsets.UTF_8);
            Path gitFile = Files.writeString(dir.resolve("git.md"), git, StandardCharsets.UTF_8);
            ProcessBuilder builder = new ProcessBuilder("git", "merge-file", "-p", "-L", "wiki", "-L", "base", "-L", "git", wikiFile.toString(), baseFile.toString(), gitFile.toString());
            builder.directory(dir.toFile());
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit;
            try {
                exit = process.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("merge-file interrupted", e);
            }
            return new MergeResult(exit == 0, output);
        } finally {
            deleteTree(dir);
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            for (Path path : stream.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<SyncConflict> openConflicts(String spaceSlug) {
        return conflictRepository.findAll().stream().filter(c -> c.getResolvedAt() == null).filter(c -> spaceSlug == null || (c.getPage() != null && c.getPage().getSpace() != null && spaceSlug.equals(c.getPage().getSpace().getSlug()))).toList();
    }

    @Transactional(readOnly = true)
    public ConflictView view(Long conflictId) {
        SyncConflict conflict = conflictRepository.findById(conflictId).orElseThrow(() -> new IllegalArgumentException("No such conflict: " + conflictId));
        Page page = conflict.getPage();
        String base = conflict.getBaseVersion() == null
            ? ""
            : versionRepository.findById(conflict.getBaseVersion().getId()).map(PageVersion::getContentMd).orElse("");
        String wiki = conflict.getWikiVersion() == null ? "" : conflict.getWikiVersion().getContentMd();
        return new ConflictView(
            conflict.getId(),
            page == null ? null : page.getId(),
            page == null ? "" : page.getTitle(),
            base == null ? "" : base,
            wiki == null ? "" : wiki,
            conflict.getGitContentMd() == null ? "" : conflict.getGitContentMd(),
            conflict.getGitCommit(),
            conflict.getDetectedAt()
        );
    }

    /**
     * Manual resolution: creates a MERGE/KEPT version, returns the page to PENDING_PUSH
     * with a WRITE outbox entry, and closes the conflict record.
     */
    @Transactional
    public void resolve(Long conflictId, String contentMd, ConflictResolution resolution, String actorLogin) {
        SyncConflict conflict = conflictRepository.findById(conflictId).orElseThrow(() -> new IllegalArgumentException("No such conflict: " + conflictId));
        if (conflict.getResolvedAt() != null) {
            throw new IllegalArgumentException("Conflict already resolved: " + conflictId);
        }
        Page page = pageRepository.findById(conflict.getPage().getId()).orElseThrow(() -> new IllegalArgumentException("Page gone: " + conflict.getPage().getId()));
        access.requireWrite(page);
        writePolicy.requireWritable(page);
        PageVersion wikiVersion = conflict.getWikiVersion();
        if (wikiVersion != null && page.getCurrentVersion() != null && !wikiVersion.getId().equals(page.getCurrentVersion().getId())) {
            throw new AccessDeniedException("Page changed since the conflict was recorded; re-merge required");
        }
        String login = actorLogin == null ? "unknown" : actorLogin;
        PageVersion version = new PageVersion();
        version.setPage(page);
        version.setVersionNo(page.getCurrentVersion() == null ? 1 : page.getCurrentVersion().getVersionNo() + 1);
        version.setContentMd(contentMd);
        version.setContentSha256(PageService.sha256(contentMd));
        version.setAuthorName(login);
        version.setSource(VersionSource.MERGE);
        version.setMessage("Merge conflict " + conflict.getId() + " (" + resolution.name() + ")");
        version.setCreatedAt(Instant.now());
        version = versionRepository.save(version);
        page.setCurrentVersion(version);
        page.setUpdatedAt(Instant.now());
        if (page.getSyncStatus() == PageSyncStatus.CONFLICT) {
            page.setSyncStatus(PageSyncStatus.PENDING_PUSH);
        }
        pageRepository.save(page);
        if (page.getKind() == PageKind.GIT) {
            pageService.markGitOutbox(page, OutboxOp.WRITE, Map.of("gitPath", String.valueOf(page.getGitPath()), "merged", "true"));
        }
        // unblock any outbox entries held by this conflict
        for (var entry : outboxRepository.findBySpaceIdAndStatusOrderByIdAsc(page.getSpace().getId(), OutboxStatus.BLOCKED_BY_CONFLICT)) {
            if (entry.getPage() != null && entry.getPage().getId().equals(page.getId())) {
                entry.setStatus(OutboxStatus.PENDING);
            }
        }
        conflict.setResolution(resolution);
        conflict.setResolvedAt(Instant.now());
    }
}
