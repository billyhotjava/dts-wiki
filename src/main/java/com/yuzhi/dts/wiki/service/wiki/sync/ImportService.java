package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.domain.SyncState;
import com.yuzhi.dts.wiki.domain.enumeration.SyncRunStatus;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.repository.SyncRootRepository;
import com.yuzhi.dts.wiki.repository.SyncStateRepository;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * First import of a space (design 04 S8): clone, create mount FOLDER pages, walk files
 * per 02 S5, import recent history (default 20 versions) with original authors and dates.
 * Byte-faithful: file bytes go into versions untouched (no normalization, v1.1).
 */
@Service
public class ImportService {

    private static final Logger LOG = LoggerFactory.getLogger(ImportService.class);

    private final GitRepoManager git;
    private final WikiProperties properties;
    private final SpaceRepository spaceRepository;
    private final PageRepository pageRepository;
    private final SyncRootRepository rootRepository;
    private final SyncStateRepository stateRepository;
    private final PageService pageService;
    private final GitAttachmentImporter assetImporter;

    public ImportService(
        GitRepoManager git,
        WikiProperties properties,
        SpaceRepository spaceRepository,
        PageRepository pageRepository,
        SyncRootRepository rootRepository,
        SyncStateRepository stateRepository,
        PageService pageService,
        GitAttachmentImporter assetImporter
    ) {
        this.git = git;
        this.properties = properties;
        this.spaceRepository = spaceRepository;
        this.pageRepository = pageRepository;
        this.rootRepository = rootRepository;
        this.stateRepository = stateRepository;
        this.pageService = pageService;
        this.assetImporter = assetImporter;
    }

    public record ImportReport(int spaces, int pages, int versions, int attachments, int skipped, List<String> notes) {}

    @Transactional
    public ImportReport importSpace(String slug, boolean importHistory, int historyLimit) {
        return importSpaceAt(slug, null, importHistory, historyLimit);
    }

    @Transactional
    public ImportReport importSpaceAt(String slug, String revision, boolean importHistory, int historyLimit) {
        Space space = spaceRepository.findOneBySlug(slug).orElseThrow(() -> new IllegalArgumentException("No such space: " + slug));
        String branch = space.getGitBranch() == null ? "main" : space.getGitBranch();
        ensureClone(space, branch);
        git.run(slug, List.of("fetch", "origin", branch), 60);
        String head = revision == null ? git.run(slug, List.of("rev-parse", "origin/" + branch), 30) : revision;
        List<String> notes = new ArrayList<>();
        int pages = 0;
        int versions = 0;
        int attachments = 0;
        int skipped = 0;
        for (SyncRoot root : enabledRoots(space)) {
            String repoPath = root.getRepoPath().replaceAll("/+$", "");
            Page mount = ensureMount(space, root);
            Map<String, List<String>> tree = listFiles(slug, head, repoPath);
            List<String> mdFiles = tree.getOrDefault("md", List.of());
            // natural sort: numbers numeric, Chinese pinyin (CLDR), files before README handling below
            mdFiles = mdFiles.stream().sorted(naturalOrder()).toList();
            for (String path : mdFiles) {
                int[] counts = importMarkdown(space, root, mount, path, head, head, importHistory, historyLimit, notes);
                pages += counts[0];
                versions += counts[1];
            }
            for (String path : tree.getOrDefault("bin", List.of())) {
                if (assetImporter.ingest(mount, head, path)) { attachments++; } else { skipped++; }
            }
            for (Page existing : pageRepository.findLiveBySpace(space.getId())) {
                String path = existing.getGitPath();
                if (path == null || !(path.equals(repoPath) || path.startsWith(repoPath + "/"))) { continue; }
                if (existing.getKind() == PageKind.GIT && !mdFiles.contains(path)) { pageService.ingestGitDeletion(existing, head); }
                else if (existing.getKind() == PageKind.FOLDER && existing.getCurrentVersion() != null && !mdFiles.contains(path + "/README.md")) {
                    pageService.ingestGitVersion(existing, "", head, null, null);
                }
            }
            assetImporter.reconcile(mount, tree.getOrDefault("bin", List.of()));
            skipped += tree.getOrDefault("skip", List.of()).size();
            stateRepository.findOneBySyncRootId(root.getId()).ifPresentOrElse(
                state -> {
                    state.setLastSyncedCommit(head);
                    state.setStatus(SyncRunStatus.OK);
                    state.setLastFetchAt(Instant.now());
                    if (properties.isOutboundEnabled() && !space.isManifestManaged()) { state.setLastPushAt(Instant.now()); }
                },
                () -> {
                    SyncState state = new SyncState();
                    state.setSyncRoot(root);
                    state.setLastSyncedCommit(head);
                    state.setStatus(SyncRunStatus.OK);
                    state.setLastFetchAt(Instant.now());
                    if (properties.isOutboundEnabled() && !space.isManifestManaged()) { state.setLastPushAt(Instant.now()); }
                    stateRepository.save(state);
                }
            );
        }
        return new ImportReport(1, pages, versions, attachments, skipped, notes);
    }

    private List<SyncRoot> enabledRoots(Space space) {
        // W6: query fresh (inverse in-memory collections go stale within a session).
        return rootRepository.findBySpaceWithMount(space.getId()).stream().filter(r -> Boolean.TRUE.equals(r.getEnabled())).toList();
    }

    private void ensureClone(Space space, String branch) {
        if (git.hasClone(space.getSlug())) {
            git.run(space.getSlug(), List.of("remote", "set-url", "origin", space.getGitRepoUrl()), 10);
            return;
        }
        if (space.getGitRepoUrl() == null || space.getGitRepoUrl().isBlank()) {
            throw new IllegalArgumentException("Space has no gitRepoUrl: " + space.getSlug());
        }
        try {
            Path parent = git.repoDir(space.getSlug()).getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            git.runIn(parent == null ? Path.of(".") : parent, List.of("clone", "--branch", branch, "--single-branch", space.getGitRepoUrl(), git.repoDir(space.getSlug()).toString()), git.sshEnv(space.getSlug()), 300);
        } catch (Exception e) {
            throw new IllegalStateException("Clone failed for space " + space.getSlug(), e);
        }
    }

    private Page ensureMount(Space space, SyncRoot root) {
        List<Page> pages = pageRepository.findLiveBySpace(space.getId());
        String mountTitle = root.getRepoPath().replaceAll("/+$", "");
        if (root.getMountPage() != null) {
            Page mount = pageRepository.findById(root.getMountPage().getId()).orElseThrow();
            // adopt a pre-configured mount as the root's FOLDER instead of duplicating it
            if (mount.getGitPath() == null) {
                mount.setGitPath(mountTitle);
                mount.setTitle(mountTitle);
            }
            return mount;
        }
        Page rootPage = pages.stream().filter(p -> p.getParent() == null).findFirst().orElse(null);
        if (rootPage == null) {
            throw new IllegalStateException("Space has no root page: " + space.getSlug());
        }
        Page mount = new Page();
        mount.setSpace(space);
        mount.setParent(rootPage);
        mount.setTitle(mountTitle);
        mount.setKind(PageKind.FOLDER);
        mount.setGitPath(mountTitle);
        mount.setPosition(1000);
        mount.setSyncStatus(com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus.SYNCED);
        mount.setCreatedAt(Instant.now());
        mount.setUpdatedAt(Instant.now());
        mount = pageRepository.save(mount);
        root.setMountPage(mount);
        return mount;
    }

    /** Lists tracked files under repoPath at branch: md / binaries / skipped. */
    private Map<String, List<String>> listFiles(String slug, String branch, String repoPath) {
        Map<String, List<String>> out = new TreeMap<>();
        out.put("md", new ArrayList<>());
        out.put("bin", new ArrayList<>());
        out.put("skip", new ArrayList<>());
        for (var entry : git.trackedFiles(slug, branch, repoPath)) {
            String path = entry.path();
            if (!(entry.mode().equals("100644") || entry.mode().equals("100755"))
                || path.chars().anyMatch(Character::isISOControl)
                || java.util.Arrays.stream(path.split("/")).anyMatch(part -> part.startsWith(".") || part.startsWith("_"))) {
                out.get("skip").add(path);
                continue;
            }
            long size = Long.parseLong(git.run(slug, List.of("cat-file", "-s", branch + ":" + path), 30));
            if (size > properties.getMaxSyncFileSize()) { out.get("skip").add(path); continue; }
            String lower = path.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".md")) { out.get("md").add(path); }
            else if (lower.matches(".*\\.(png|jpg|jpeg|gif|webp|svg|pdf|pptx|docx|xlsx)$")) { out.get("bin").add(path); }
            else { out.get("skip").add(path); }
        }
        return out;
    }

    private int[] importMarkdown(Space space, SyncRoot root, Page mount, String path, String revision, String head, boolean importHistory, int historyLimit, List<String> notes) {
        List<Page> pages = pageRepository.findLiveBySpace(space.getId());
        String content = readFile(space.getSlug(), revision, path);
        Page target;
        int created = 0;
        if (isReadme(path)) {
            String dir = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
            target = ensureFolderChain(space, pages, relativeDir(root, dir), mount);
            if (target == null) { target = mount; }
        } else {
            target = pages.stream().filter(page -> path.equals(page.getGitPath())).findFirst().orElse(null);
            if (target == null) {
                String dir = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
                Page parent = ensureFolderChain(space, pages, relativeDir(root, dir), mount);
                String fileName = path.substring(path.lastIndexOf('/') + 1);
                target = pageService.ingestGitPage(space, parent == null ? mount : parent, firstHeading(content, fileName), path, PageKind.GIT, null, null, null, null);
                created = 1;
            }
        }
        int count = importFileHistory(target, space.getSlug(), path, revision, content, importHistory && target.getCurrentVersion() == null, historyLimit);
        target.setSyncStatus(com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus.SYNCED);
        return new int[] { created, count };
    }

    private int importFileHistory(Page page, String slug, String path, String revision, String currentContent, boolean importHistory, int historyLimit) {
        int count = 0;
        int limit = importHistory ? Math.max(1, Math.min(historyLimit, 100)) : 1;
        String log = git.run(slug, List.of("log", "--follow", "--name-status", "--format=%H%x00%an%x00%ae%x00%cI", "-n", String.valueOf(limit), revision, "--", path), 60);
        List<HistoryEntry> history = new ArrayList<>();
        String historicalPath = path;
        for (String line : log.split("\n")) {
            String[] fields = line.split("\0", -1);
            if (fields.length == 4) { history.add(new HistoryEntry(fields[0], fields[1], fields[2], fields[3], historicalPath)); }
            else if (line.startsWith("R")) {
                String[] rename = line.split("\t", -1);
                if (rename.length == 3) { historicalPath = rename[1]; }
            }
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            HistoryEntry entry = history.get(i);
            String content = i == 0 ? currentContent : git.fileAt(slug, entry.commit(), entry.path());
            Long before = page.getCurrentVersion() == null ? null : page.getCurrentVersion().getId();
            var version = pageService.ingestGitVersion(page, content, entry.commit(), entry.author(), entry.email(), java.time.OffsetDateTime.parse(entry.date()).toInstant());
            if (before == null || !before.equals(version.getId())) { count++; }
        }
        return count;
    }

    private record HistoryEntry(String commit, String author, String email, String date, String path) {}

    private Page ensureFolderChain(Space space, List<Page> pages, String dir, Page mount) {
        if (dir != null && dir.equals(mount.getGitPath())) { return mount; }
        if (dir == null || dir.isEmpty()) {
            return null;
        }
        for (Page page : pages) {
            if (page.getKind() == PageKind.FOLDER && dir.equals(page.getGitPath())) {
                return page;
            }
        }
        String parentDir = dir.contains("/") ? dir.substring(0, dir.lastIndexOf('/')) : "";
        Page parent = ensureFolderChain(space, pages, parentDir, mount);
        String title = dir.contains("/") ? dir.substring(dir.lastIndexOf('/') + 1) : dir;
        // dir here is repo-relative within the sync root; the FOLDER gitPath is the full path
        Page created = pageService.ingestGitPage(space, parent == null ? mount : parent, title, dir, PageKind.FOLDER, null, null, null, null);
        pages.add(created);
        return created;
    }

    private String relativeDir(SyncRoot root, String dir) {
        String prefix = root.getRepoPath().replaceAll("/+$", "");
        if (dir.equals(prefix)) {
            return prefix;
        }
        if (dir.startsWith(prefix + "/")) {
            return dir;
        }
        return prefix.isEmpty() ? dir : prefix + "/" + dir;
    }

    private String readFile(String slug, String branch, String path) {
        String content = git.fileAt(slug, branch, path);
        if (content.getBytes(StandardCharsets.UTF_8).length > properties.getMaxSyncFileSize()) {
            throw new IllegalStateException("File too large, skipped: " + path);
        }
        return content;
    }

    private String[] fileAuthor(String slug, String head, String path) {
        try {
            String raw = git.commitAuthor(slug, head, path);
            String[] parts = raw.split("\0", -1);
            return new String[] { parts.length > 0 ? parts[0] : "", parts.length > 1 ? parts[1] : "" };
        } catch (GitCommandException e) {
            return new String[] { "", "" };
        }
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

    private boolean isReadme(String path) {
        return path.endsWith("/README.md") || path.equals("README.md");
    }

    private Comparator<String> naturalOrder() {
        Collator collator = Collator.getInstance(Locale.CHINA);
        return (a, b) -> {
            // numeric-aware segment compare, then pinyin-aware full compare
            String[] pa = a.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            String[] pb = b.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
            int n = Math.min(pa.length, pb.length);
            for (int i = 0; i < n; i++) {
                int c;
                try {
                    c = Long.compare(Long.parseLong(pa[i]), Long.parseLong(pb[i]));
                } catch (NumberFormatException e) {
                    c = collator.compare(pa[i], pb[i]);
                }
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(pa.length, pb.length);
        };
    }
}
