package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncConflict;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxStatus;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import com.yuzhi.dts.wiki.repository.SyncConflictRepository;
import com.yuzhi.dts.wiki.repository.SyncOutboxRepository;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * git → wiki inbound (design 04 S5, v1.1 checkpoint rules).
 * Consumes one remote interval {@code (lastSyncedCommit, remoteHead]} per space per cycle.
 * The checkpoint advances only after every change in the interval is applied, persisted
 * as a conflict, or explicitly skipped — never on IO/DB failure.
 */
@Service
public class InboundSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(InboundSyncService.class);

    private final GitRepoManager git;
    private final WikiProperties properties;
    private final PageService pageService;
    private final PageRepository pageRepository;
    private final PageVersionRepository pageVersionRepository;
    private final SyncConflictRepository conflictRepository;
    private final SyncOutboxRepository outboxRepository;
    private final GitAttachmentImporter assetImporter;

    public InboundSyncService(
        GitRepoManager git,
        WikiProperties properties,
        PageService pageService,
        PageRepository pageRepository,
        PageVersionRepository pageVersionRepository,
        SyncConflictRepository conflictRepository,
        SyncOutboxRepository outboxRepository,
        GitAttachmentImporter assetImporter
    ) {
        this.git = git;
        this.properties = properties;
        this.pageService = pageService;
        this.pageRepository = pageRepository;
        this.pageVersionRepository = pageVersionRepository;
        this.conflictRepository = conflictRepository;
        this.outboxRepository = outboxRepository;
        this.assetImporter = assetImporter;
    }

    public record InboundReport(boolean complete, int applied, int conflicts, int skipped, String remoteHead) {}

    @Transactional
    public InboundReport inbound(Space space, List<SyncRoot> roots, String branch, String lastSyncedCommit, String remoteHead) {
        String slug = space.getSlug();
        List<SyncRoot> enabled = roots.stream().filter(r -> Boolean.TRUE.equals(r.getEnabled())).toList();
        if (enabled.isEmpty()) {
            return new InboundReport(true, 0, 0, 0, remoteHead);
        }
        List<String> rootPaths = enabled.stream().map(r -> r.getRepoPath().replaceAll("/+$", "")).toList();
        if (lastSyncedCommit == null) { throw new IllegalArgumentException("Initial content requires a full import"); }
        List<GitRepoManager.Change> changes = git.changes(slug, lastSyncedCommit, remoteHead, rootPaths);
        LOG.debug("Inbound {} interval {}..{}", slug, lastSyncedCommit, remoteHead);
        int applied = 0;
        int conflicts = 0;
        int skipped = 0;
        for (var change : changes) {
            if (change.path().chars().anyMatch(Character::isISOControl)
                || change.fromPath() != null && change.fromPath().chars().anyMatch(Character::isISOControl)) { skipped++; continue; }
            String line = change.status() + "\t" + (change.fromPath() == null ? "" : change.fromPath() + "\t") + change.path();
            try {
                ChangeOutcome outcome = applyChange(space, roots, line, lastSyncedCommit, remoteHead, branch);
                switch (outcome) {
                    case APPLIED -> applied++;
                    case CONFLICT -> conflicts++;
                    case SKIPPED -> skipped++;
                }
            } catch (GitCommandException | IOException e) {
                LOG.warn("Inbound change failed, checkpoint held: {}: {}", line, e.getMessage());
                return new InboundReport(false, applied, conflicts, skipped, remoteHead);
            }
        }
        return new InboundReport(true, applied, conflicts, skipped, remoteHead);
    }

    private enum ChangeOutcome {
        APPLIED,
        CONFLICT,
        SKIPPED
    }

    private ChangeOutcome applyChange(Space space, List<SyncRoot> roots, String line, String lastSyncedCommit, String remoteHead, String branch) throws IOException {
        String[] parts = line.split("\t");
        String status = parts[0];
        char kind = status.charAt(0);
        if (kind == 'R') {
            return applyRename(space, roots, parts[1], parts[2], lastSyncedCommit, remoteHead, branch);
        }
        String path = parts.length > 1 ? parts[1] : "";
        if (kind == 'D') {
            return applyDelete(space, roots, path, remoteHead, branch);
        }
        if (kind == 'A' || kind == 'M' || kind == 'T') {
            return applyAddOrModify(space, roots, path, lastSyncedCommit, remoteHead, branch);
        }
        LOG.debug("Ignoring unknown change kind: {}", line);
        return ChangeOutcome.SKIPPED;
    }

    private ChangeOutcome applyAddOrModify(Space space, List<SyncRoot> roots, String path, String lastSyncedCommit, String remoteHead, String branch) throws IOException {
        SyncRoot root = owningRoot(roots, path);
        if (root == null) {
            return ChangeOutcome.SKIPPED;
        }
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        if (!git.isRegularFile(space.getSlug(), remoteHead, path) || java.util.Arrays.stream(path.split("/")).anyMatch(p -> p.startsWith(".") || p.startsWith("_"))) {
            return ChangeOutcome.SKIPPED;
        }
        if (!isSyncableFile(path)) {
            return importBinary(space, root, path, remoteHead, branch);
        }
        if (fileSize(space.getSlug(), remoteHead, path) > properties.getMaxSyncFileSize()) { return ChangeOutcome.SKIPPED; }
        String gitContent = git.fileAt(space.getSlug(), remoteHead, path);
        List<Page> pages = pageRepository.findLiveBySpace(space.getId());
        Optional<Page> existing = findPageForPath(space, pages, path);
        String[] author = commitAuthor(space.getSlug(), remoteHead, path);
        if (existing.isEmpty()) {
            createFromGit(space, root, pages, path, gitContent, remoteHead, author, branch);
            return ChangeOutcome.APPLIED;
        }
        Page page = existing.orElseThrow();
        String currentSha = page.getCurrentVersion() == null ? "" : page.getCurrentVersion().getContentSha256();
        if (PageService.sha256(gitContent).equals(currentSha)) {
            return ChangeOutcome.APPLIED; // e.g. our own pushed commit
        }
        if (!properties.isOutboundEnabled() || space.isManifestManaged() || page.getSyncStatus() == PageSyncStatus.SYNCED) {
            pageService.ingestGitVersion(page, gitContent, fileCommit(space.getSlug(), remoteHead, path), author[0], author[1], fileTime(space.getSlug(), remoteHead, path));
            page.setSyncStatus(PageSyncStatus.SYNCED);
            return ChangeOutcome.APPLIED;
        }
        if (page.getSyncStatus() == PageSyncStatus.PENDING_PUSH) {
            String base = lastSyncedCommit == null ? "" : readBase(space.getSlug(), lastSyncedCommit, path);
            String wikiContent = page.getCurrentVersion() == null ? "" : page.getCurrentVersion().getContentMd();
            ConflictService.MergeResult merge = ConflictService.merge(base, wikiContent, gitContent);
            if (merge.clean()) {
                pageService.ingestGitVersion(page, merge.merged(), fileCommit(space.getSlug(), remoteHead, path), author[0], author[1]);
                page.setSyncStatus(PageSyncStatus.PENDING_PUSH);
                // v1.1: the merged result gets its own outbound op; the replaced WRITE keeps
                // its payload so a stale replay can never overwrite the merge (see OutboundSyncService).
                pageService.markGitOutbox(page, com.yuzhi.dts.wiki.domain.enumeration.OutboxOp.WRITE, Map.of("gitPath", String.valueOf(page.getGitPath()), "merged", "true"));
                return ChangeOutcome.APPLIED;
            }
            createConflict(page, base, wikiContent, gitContent, remoteHead);
            page.setSyncStatus(PageSyncStatus.CONFLICT);
            blockOutbox(page);
            return ChangeOutcome.CONFLICT;
        }
        // already CONFLICT: refresh the git side, keep base/wiki for traceability (v1.1)
        updateConflictGit(page, gitContent, remoteHead);
        return ChangeOutcome.CONFLICT;
    }

    private ChangeOutcome applyDelete(Space space, List<SyncRoot> roots, String path, String remoteHead, String branch) {
        if (owningRoot(roots, path) == null) {
            return ChangeOutcome.SKIPPED;
        }
        if (!isSyncableFile(path)) { assetImporter.delete(space.getId(), path); return ChangeOutcome.APPLIED; }
        List<Page> pages = pageRepository.findLiveBySpace(space.getId());
        Optional<Page> existing = findPageForPath(space, pages, path);
        if (existing.isEmpty()) {
            return ChangeOutcome.APPLIED;
        }
        Page page = existing.orElseThrow();
        if (!properties.isOutboundEnabled() || space.isManifestManaged() || page.getSyncStatus() == PageSyncStatus.SYNCED) {
            pageService.ingestGitDeletion(page, remoteHead);
            return ChangeOutcome.APPLIED;
        }
        // git deleted while wiki has unpushed changes -> conflict with "accept deletion" option
        createConflict(page, "", page.getCurrentVersion() == null ? "" : page.getCurrentVersion().getContentMd(), "", remoteHead);
        page.setSyncStatus(PageSyncStatus.CONFLICT);
        blockOutbox(page);
        return ChangeOutcome.CONFLICT;
    }

    private ChangeOutcome applyRename(Space space, List<SyncRoot> roots, String from, String to, String lastSyncedCommit, String remoteHead, String branch) throws IOException {
        if (owningRoot(roots, to) == null) {
            return owningRoot(roots, from) == null ? ChangeOutcome.SKIPPED : applyDelete(space, roots, from, remoteHead, branch);
        }
        if (!git.isRegularFile(space.getSlug(), remoteHead, to)) { return ChangeOutcome.SKIPPED; }
        List<Page> pages = pageRepository.findLiveBySpace(space.getId());
        Optional<Page> existing = pages.stream().filter(p -> from.equals(p.getGitPath())).findFirst();
        if (existing.isPresent()) {
            Page page = existing.orElseThrow();
            page.setGitPath(to);
            if (page.getKind() == PageKind.FOLDER) {
                repathChildren(page, from, to);
            }
        }
        // content may have changed along with the rename: fall through to M handling
        if (!isSyncableFile(to)) {
            assetImporter.delete(space.getId(), from);
            return importBinary(space, owningRoot(roots, to), to, remoteHead, branch);
        }
        try {
            String gitContent = git.fileAt(space.getSlug(), remoteHead, to);
            if (existing.isPresent()) {
                Page page = existing.orElseThrow();
                String currentSha = page.getCurrentVersion() == null ? "" : page.getCurrentVersion().getContentSha256();
                if (!PageService.sha256(gitContent).equals(currentSha)) {
                    String[] author = commitAuthor(space.getSlug(), remoteHead, to);
                    pageService.ingestGitVersion(page, gitContent, fileCommit(space.getSlug(), remoteHead, to), author[0], author[1], fileTime(space.getSlug(), remoteHead, to));
                }
                return ChangeOutcome.APPLIED;
            }
        } catch (GitCommandException e) {
            return ChangeOutcome.APPLIED; // pure rename, no content to absorb
        }
        return existing.isEmpty() ? applyAddOrModify(space, roots, to, lastSyncedCommit, remoteHead, branch) : ChangeOutcome.APPLIED;
    }

    private void repathChildren(Page folder, String fromDir, String toDir) {
        List<Page> pages = pageRepository.findLiveBySpace(folder.getSpace().getId());
        for (Page page : pages) {
            if (page.getGitPath() != null && (page.getGitPath().equals(fromDir) || page.getGitPath().startsWith(fromDir + "/")) && !page.getId().equals(folder.getId())) {
                page.setGitPath(toDir + page.getGitPath().substring(fromDir.length()));
            }
        }
    }

    private void createFromGit(Space space, SyncRoot root, List<Page> pages, String path, String gitContent, String remoteHead, String[] author, String branch) {
        if (isReadme(path)) {
            String dir = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
            Page folder = ensureFolderChain(space, pages, dir, root.getMountPage());
            pageService.ingestGitVersion(folder == null ? root.getMountPage() : folder, gitContent, fileCommit(space.getSlug(), remoteHead, path), author[0], author[1], fileTime(space.getSlug(), remoteHead, path));
            return;
        }
        String dir = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
        Page parent = ensureFolderChain(space, pages, dir, root.getMountPage());
        String title = firstHeading(gitContent, path.substring(path.lastIndexOf('/') + 1));
        Page created = pageService.ingestGitPage(space, parent == null ? root.getMountPage() : parent, title, path, PageKind.GIT, null, null, null, null);
        pageService.ingestGitVersion(created, gitContent, fileCommit(space.getSlug(), remoteHead, path), author[0], author[1], fileTime(space.getSlug(), remoteHead, path));
        pages.add(created);
    }

    private Page ensureFolderChain(Space space, List<Page> pages, String dir, Page mount) {
        if (dir == null || dir.isEmpty()) {
            return null;
        }
        Optional<Page> existing = pages.stream().filter(p -> p.getKind() == PageKind.FOLDER && dir.equals(p.getGitPath())).findFirst();
        if (existing.isPresent()) {
            return existing.orElseThrow();
        }
        String parentDir = dir.contains("/") ? dir.substring(0, dir.lastIndexOf('/')) : "";
        Page parent = ensureFolderChain(space, pages, parentDir, mount);
        String title = dir.contains("/") ? dir.substring(dir.lastIndexOf('/') + 1) : dir;
        Page created = pageService.ingestGitPage(space, parent == null ? mount : parent, title, dir, PageKind.FOLDER, null, null, null, null);
        pages.add(created);
        return created;
    }

    private ChangeOutcome importBinary(Space space, SyncRoot root, String path, String remoteHead, String branch) throws IOException {
        if (root == null || !isBinarySyncable(path)) {
            return ChangeOutcome.SKIPPED;
        }
        if (fileSize(space.getSlug(), remoteHead, path) > properties.getMaxSyncFileSize()) {
            LOG.warn("Skipping oversized sync file {} (>{} bytes)", path, properties.getMaxSyncFileSize());
            return ChangeOutcome.SKIPPED;
        }
        return assetImporter.ingest(root.getMountPage(), remoteHead, path) ? ChangeOutcome.APPLIED : ChangeOutcome.SKIPPED;
    }

    private void createConflict(Page page, String base, String wikiContent, String gitContent, String remoteHead) {
        if (!conflictRepository.findByPageIdAndResolvedAtIsNull(page.getId()).isEmpty()) {
            updateConflictGit(page, gitContent, remoteHead);
            return;
        }
        SyncConflict conflict = new SyncConflict();
        conflict.setPage(page);
        conflict.setGitContentMd(gitContent);
        conflict.setGitCommit(remoteHead);
        conflict.setDetectedAt(Instant.now());
        // baseline = predecessor version when available (best-effort; the merge UI
        // falls back to empty baseline).
        List<com.yuzhi.dts.wiki.domain.PageVersion> versions = pageVersionRepository.findByPageIdOrderByVersionNoDesc(page.getId());
        com.yuzhi.dts.wiki.domain.PageVersion current = page.getCurrentVersion();
        com.yuzhi.dts.wiki.domain.PageVersion baseVersion = null;
        for (int i = 0; i < versions.size(); i++) {
            if (current != null && versions.get(i).getId().equals(current.getId()) && i + 1 < versions.size()) {
                baseVersion = versions.get(i + 1);
                break;
            }
        }
        conflict.setBaseVersion(baseVersion);
        conflict.setWikiVersion(current);
        conflictRepository.save(conflict);
    }

    private void updateConflictGit(Page page, String gitContent, String remoteHead) {
        for (SyncConflict conflict : conflictRepository.findByPageIdAndResolvedAtIsNull(page.getId())) {
            conflict.setGitContentMd(gitContent);
            conflict.setGitCommit(remoteHead);
        }
    }

    private void blockOutbox(Page page) {
        for (var entry : outboxRepository.findBySpaceIdAndStatusOrderByIdAsc(page.getSpace().getId(), OutboxStatus.PENDING)) {
            if (entry.getPage() != null && entry.getPage().getId().equals(page.getId())) {
                entry.setStatus(OutboxStatus.BLOCKED_BY_CONFLICT);
            }
        }
    }

    /** Page for a repo path: exact gitPath match, or the FOLDER for README paths. */
    private Optional<Page> findPageForPath(Space space, List<Page> pages, String path) {
        Optional<Page> exact = pages.stream().filter(p -> path.equals(p.getGitPath())).findFirst();
        if (exact.isPresent()) {
            return exact;
        }
        if (isReadme(path)) {
            String dir = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
            if (path.equals("README.md")) {
                dir = "";
            }
            final String folderPath = dir;
            return pages.stream().filter(p -> p.getKind() == PageKind.FOLDER && folderPath.equals(p.getGitPath())).findFirst();
        }
        return Optional.empty();
    }

    private String[] commitAuthor(String slug, String remoteHead, String path) {        try {
            String raw = git.commitAuthor(slug, remoteHead, path);
            String[] parts = raw.split("\0", -1);
            return new String[] { parts.length > 0 ? parts[0] : "", parts.length > 1 ? parts[1] : "" };
        } catch (GitCommandException e) {
            return new String[] { "", "" };
        }
    }

    private Instant fileTime(String slug, String head, String path) {
        return java.time.OffsetDateTime.parse(git.run(slug, List.of("log", "-1", "--format=%cI", head, "--", path), 30)).toInstant();
    }

    private String fileCommit(String slug, String remoteHead, String path) {
        try {
            return git.run(slug, List.of("log", "-1", "--format=%H", remoteHead, "--", path), 30);
        } catch (GitCommandException e) {
            return remoteHead;
        }
    }

    private String readBase(String slug, String lastSyncedCommit, String path) {
        try {
            return git.fileAt(slug, lastSyncedCommit, path);
        } catch (GitCommandException e) {
            return "";
        }
    }

    private long fileSize(String slug, String revision, String path) {
        try {
            String size = git.run(slug, List.of("cat-file", "-s", revision + ":" + path), 30);
            return Long.parseLong(size.strip());
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    private SyncRoot owningRoot(List<SyncRoot> roots, String path) {
        SyncRoot best = null;
        for (SyncRoot root : roots) {
            if (!Boolean.TRUE.equals(root.getEnabled())) { continue; }
            String prefix = root.getRepoPath().replaceAll("/+$", "");
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                if (best == null || prefix.length() > best.getRepoPath().length()) {
                    best = root;
                }
            }
        }
        return best;
    }

    private boolean isSyncableFile(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        return lower.endsWith(".md");
    }

    private boolean isBinarySyncable(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        for (String ext : List.of(".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".pdf", ".pptx", ".docx", ".xlsx")) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReadme(String path) {
        return path.endsWith("/README.md") || path.equals("README.md");
    }

    private String firstHeading(String markdown, String fallback) {
        if (markdown == null) {
            return fallback;
        }
        for (String line : markdown.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).strip();
            }
        }
        int dot = fallback.lastIndexOf('.');
        return dot < 0 ? fallback : fallback.substring(0, dot);
    }

    private String stripExtension(String path) {
        int dot = path.lastIndexOf('.');
        int slash = path.lastIndexOf('/');
        return dot > slash ? path.substring(0, dot) : path;
    }
}
