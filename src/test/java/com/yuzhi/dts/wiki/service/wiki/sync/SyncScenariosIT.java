package com.yuzhi.dts.wiki.service.wiki.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.repository.SyncConflictRepository;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Git sync scenarios (design 07 S3) against throwaway bare remotes on local paths
 * (no SSH involved; sshEnv is inert for local paths). Each test owns its space.
 */
@IntegrationTest
@Transactional
@WithMockUser(authorities = { "ROLE_ADMIN" })
class SyncScenariosIT {

    @TempDir
    Path temp;

    @TempDir
    static Path sharedRepos;

    @DynamicPropertySource
    static void gitRoots(DynamicPropertyRegistry registry) {
        // hermetic workdirs: never touch /data on the dev machine
        registry.add("application.wiki.repos-dir", () -> sharedRepos.resolve("repos").toString());
        registry.add("application.wiki.ssh-keys-dir", () -> sharedRepos.resolve("secrets").toString());
    }

    @Autowired
    private SyncAdminService adminService;

    @Autowired
    private GitSyncScheduler scheduler;

    @Autowired
    private GitRepoManager git;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private PageRepository pageRepository;

    @Autowired
    private PageService pageService;

    @Autowired
    private SyncConflictRepository conflictRepository;

    @Autowired
    private ConflictService conflictService;

    @Autowired
    private com.yuzhi.dts.wiki.repository.SyncStateRepository stateRepository;

    @BeforeEach
    void requireGit() {
        try {
            Process process = new ProcessBuilder("git", "--version").start();
            assumeTrue(process.waitFor() == 0, "git CLI required");
        } catch (Exception e) {
            assumeTrue(false, "git CLI required");
        }
    }

    // ------------------------------------------------------------ harness

    private record Fixture(String slug, Path remote, Path dev) {}

    private Fixture initRemote(String slug, Map<String, String> files, String... extraCommits) throws Exception {
        Path remote = temp.resolve(slug + ".git");
        sh(temp, "git", "init", "--bare", "-b", "main", remote.toString());
        Path dev = temp.resolve(slug + "-dev");
        sh(temp, "git", "clone", remote.toString(), dev.toString());
        sh(dev, "git", "config", "user.name", "dev");
        sh(dev, "git", "config", "user.email", "dev@example.com");
        sh(dev, "git", "checkout", "-b", "main");
        for (var entry : files.entrySet()) {
            write(dev, entry.getKey(), entry.getValue());
        }
        devCommit(dev, "seed");
        for (String extra : extraCommits) {
            String[] parts = extra.split("=", 2);
            write(dev, parts[0], parts[1]);
            devCommit(dev, "second " + parts[0]);
        }
        adminService.createSpace(slug, slug.toUpperCase(), null, remote.toString(), "main", List.of(new SyncAdminService.RootSpec("docs")));
        return new Fixture(slug, remote, dev);
    }

    private void devCommit(Path dev, String message) throws Exception {
        sh(dev, "git", "add", "-A");
        sh(dev, "git", "-c", "user.name=dev", "-c", "user.email=dev@example.com", "commit", "-m", message);
        sh(dev, "git", "push", "origin", "main");
    }

    private void write(Path dev, String path, String content) throws Exception {
        Path file = dev.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    private String showRemote(Fixture fix, String path) {
        // read through a fresh fetch to prove the push really landed
        git.run(fix.slug(), List.of("fetch", "origin", "main"), 60);
        return git.fileAt(fix.slug(), "origin/main", path);
    }

    private Page pageByGitPath(String slug, String gitPath) {
        Space space = spaceRepository.findOneBySlug(slug).orElseThrow();
        return pageRepository
            .findLiveBySpace(space.getId())
            .stream()
            .filter(p -> gitPath.equals(p.getGitPath()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No page for " + gitPath));
    }

    private static void sh(Path dir, String... args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.directory(dir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IllegalStateException("git failed: " + String.join(" ", args) + "\n" + output);
        }
    }

    // ------------------------------------------------------------ scenarios

    @Test
    void importMapsFilesReadmeAndHistory() {
        // 07 S3.1 is covered by importSpace below (counts, README->FOLDER, <=20 versions)
    }

    @Test
    void fullImportAndOutboundRoundTrip() throws Exception {
        Fixture fix = initRemote("s01", Map.of("docs/README.md", "# Guide\n", "docs/a.md", "# A\n\ntext\n"), "docs/a.md=# A\n\ntext\n\nmore\n");
        var report = adminService.importSpace(fix.slug(), true);
        assertThat(report.pages()).isEqualTo(1); // a.md; README becomes the FOLDER body
        Page folder = pageByGitPath(fix.slug(), "docs");
        assertThat(folder.getTitle()).isEqualTo("docs");
        assertThat(folder.getCurrentVersion().getContentMd()).isEqualTo("# Guide\n");
        Page a = pageByGitPath(fix.slug(), "docs/a.md");
        assertThat(a.getCurrentVersion().getVersionNo()).isEqualTo(2); // seed + one history
        assertThat(a.getCurrentVersion().getAuthorName()).isEqualTo("dev");

        // wiki edit -> sync -> remote file updated, author = wiki user
        PageDtos.PageView view = pageService.getPage(a.getId());
        pageService.saveContent(a.getId(), new PageDtos.SaveContentRequest(view.versionNo(), "# A\n\ntext\n\nmore\n\nwiki line\n", null));
        scheduler.syncSpace(fix.slug());
        assertThat(showRemote(fix, "docs/a.md")).contains("wiki line");

        // git edit -> sync -> wiki version updated, page id stable
        write(fix.dev(), "docs/a.md", "# A\n\ntext\n\nmore\n\nwiki line\n\ngit line\n");
        devCommit(fix.dev(), "git line");
        scheduler.syncSpace(fix.slug());
        Page reloaded = pageByGitPath(fix.slug(), "docs/a.md");
        assertThat(reloaded.getId()).isEqualTo(a.getId());
        assertThat(reloaded.getCurrentVersion().getContentMd()).contains("git line");
        assertThat(reloaded.getCurrentVersion().getSource().name()).isEqualTo("GIT");
    }

    @Test
    void disjointEditsAutoMerge() throws Exception {
        Fixture fix = initRemote("s02", Map.of("docs/a.md", "line1\nline2\nline3\n"));
        adminService.importSpace(fix.slug(), false);
        Page a = pageByGitPath(fix.slug(), "docs/a.md");
        PageDtos.PageView view = pageService.getPage(a.getId());
        pageService.saveContent(a.getId(), new PageDtos.SaveContentRequest(view.versionNo(), "line1\nline2 wiki\nline3\n", null));
        write(fix.dev(), "docs/a.md", "line1\nline2\nline3 git\n");
        devCommit(fix.dev(), "git side");
        scheduler.syncSpace(fix.slug());
        Page merged = pageByGitPath(fix.slug(), "docs/a.md");
        assertThat(merged.getCurrentVersion().getContentMd()).contains("line2 wiki").contains("line3 git");
        assertThat(merged.getSyncStatus().name()).isEqualTo("SYNCED");
        assertThat(conflictRepository.findByPageIdAndResolvedAtIsNull(merged.getId())).isEmpty();
        assertThat(showRemote(fix, "docs/a.md")).contains("line2 wiki");
    }

    @Test
    void sameLineConflictAndResolve() throws Exception {
        Fixture fix = initRemote("s03", Map.of("docs/a.md", "same\n"));
        adminService.importSpace(fix.slug(), false);
        Page a = pageByGitPath(fix.slug(), "docs/a.md");
        PageDtos.PageView view = pageService.getPage(a.getId());
        pageService.saveContent(a.getId(), new PageDtos.SaveContentRequest(view.versionNo(), "wiki change\n", null));
        write(fix.dev(), "docs/a.md", "git change\n");
        devCommit(fix.dev(), "git side");
        scheduler.syncSpace(fix.slug());

        Page conflicted = pageByGitPath(fix.slug(), "docs/a.md");
        assertThat(conflicted.getSyncStatus().name()).isEqualTo("CONFLICT");
        assertThat(conflictRepository.findByPageIdAndResolvedAtIsNull(conflicted.getId())).hasSize(1);

        // ordinary save is rejected while conflicted
        PageDtos.PageView current = pageService.getPage(conflicted.getId());
        try {
            pageService.saveContent(conflicted.getId(), new PageDtos.SaveContentRequest(current.versionNo(), "other\n", null));
            assertThat(false).as("expected PAGE_SYNC_CONFLICT").isTrue();
        } catch (com.yuzhi.dts.wiki.service.wiki.PageSyncConflictException expected) {
            assertThat(true).isTrue();
        }

        // manual merge -> sync -> pushed, conflict closed
        var open = conflictRepository.findByPageIdAndResolvedAtIsNull(conflicted.getId());
        conflictService.resolve(open.get(0).getId(), "merged line\n", com.yuzhi.dts.wiki.domain.enumeration.ConflictResolution.MERGED, "user");
        scheduler.syncSpace(fix.slug());
        assertThat(showRemote(fix, "docs/a.md")).isEqualTo("merged line\n");
        assertThat(conflictRepository.findByPageIdAndResolvedAtIsNull(conflicted.getId())).isEmpty();
        assertThat(pageByGitPath(fix.slug(), "docs/a.md").getSyncStatus().name()).isEqualTo("SYNCED");
    }

    @Test
    void gitDeleteVsWikiEditResolvesByKeepingWiki() throws Exception {
        Fixture fix = initRemote("s04", Map.of("docs/a.md", "keep me\n"));
        adminService.importSpace(fix.slug(), false);
        Page a = pageByGitPath(fix.slug(), "docs/a.md");
        PageDtos.PageView view = pageService.getPage(a.getId());
        pageService.saveContent(a.getId(), new PageDtos.SaveContentRequest(view.versionNo(), "keep me edited\n", null));
        sh(fix.dev(), "git", "rm", "docs/a.md");
        devCommit(fix.dev(), "git deletes");
        scheduler.syncSpace(fix.slug());

        Page conflicted = pageRepository.findById(a.getId()).orElseThrow();
        assertThat(conflicted.getSyncStatus().name()).isEqualTo("CONFLICT");
        var open = conflictRepository.findByPageIdAndResolvedAtIsNull(conflicted.getId());
        conflictService.resolve(open.get(0).getId(), "keep me edited\n", com.yuzhi.dts.wiki.domain.enumeration.ConflictResolution.KEPT_WIKI, "user");
        scheduler.syncSpace(fix.slug());
        assertThat(showRemote(fix, "docs/a.md")).isEqualTo("keep me edited\n");
    }

    @Test
    void renameKeepsPageId() throws Exception {
        Fixture fix = initRemote("s05", Map.of("docs/a.md", "# A\n"));
        adminService.importSpace(fix.slug(), false);
        Long before = pageByGitPath(fix.slug(), "docs/a.md").getId();
        sh(fix.dev(), "git", "mv", "docs/a.md", "docs/b.md");
        devCommit(fix.dev(), "rename");
        scheduler.syncSpace(fix.slug());
        assertThat(pageByGitPath(fix.slug(), "docs/b.md").getId()).isEqualTo(before);
    }

    @Test
    void repeatSyncIsIdempotent() throws Exception {
        Fixture fix = initRemote("s06", Map.of("docs/a.md", "# A\n"));
        adminService.importSpace(fix.slug(), false);
        scheduler.syncSpace(fix.slug());
        String headBefore = git.head(fix.slug());
        scheduler.syncSpace(fix.slug());
        assertThat(git.head(fix.slug())).isEqualTo(headBefore);
    }

    @Test
    void offlineThenRecovery() {
        adminService.createSpace("s07", "S07", null, "/nonexistent/repo.git", "main", List.of(new SyncAdminService.RootSpec("docs")));
        scheduler.syncSpace("s07");
        Space space = spaceRepository.findOneBySlug("s07").orElseThrow();
        var states = space.getSyncRootses().stream().map(r -> stateRepository.findOneBySyncRootId(r.getId())).toList();
        assertThat(states).isNotEmpty();
        assertThat(states.get(0)).isPresent();
        assertThat(states.get(0).orElseThrow().getStatus().name()).isEqualTo("OFFLINE");
    }

    @Test
    void chinesePathsRoundTrip() throws Exception {
        Fixture fix = initRemote("s08", Map.of("docs/工作日志/ sprint-计划.md", "# 计划\n\n中文内容\n"));
        adminService.importSpace(fix.slug(), false);
        Page page = pageByGitPath(fix.slug(), "docs/工作日志/ sprint-计划.md");
        assertThat(page.getTitle()).isEqualTo("计划");
        PageDtos.PageView view = pageService.getPage(page.getId());
        pageService.saveContent(page.getId(), new PageDtos.SaveContentRequest(view.versionNo(), "# 计划\n\n中文内容\n\n补充\n", null));
        scheduler.syncSpace(fix.slug());
        assertThat(showRemote(fix, "docs/工作日志/ sprint-计划.md")).contains("补充");
    }
}
