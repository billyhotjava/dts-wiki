package com.yuzhi.dts.wiki.service.wiki.sync;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
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

@IntegrationTest
@Transactional
@WithMockUser(authorities = { "ROLE_ADMIN" })
class SyncDebugIT {

    @TempDir
    Path temp;

    @TempDir
    static Path sharedRepos;

    @DynamicPropertySource
    static void gitRoots(DynamicPropertyRegistry registry) {
        registry.add("application.wiki.repos-dir", () -> sharedRepos.resolve("repos").toString());
        registry.add("application.wiki.ssh-keys-dir", () -> sharedRepos.resolve("secrets").toString());
    }

    @Autowired
    private SyncAdminService adminService;

    @Autowired
    private ImportService importService;

    @Autowired
    private GitRepoManager git;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired
    private PageRepository pageRepository;

    @BeforeEach
    void requireGit() {
        try {
            Process process = new ProcessBuilder("git", "--version").start();
            assumeTrue(process.waitFor() == 0, "git CLI required");
        } catch (Exception e) {
            assumeTrue(false, "git CLI required");
        }
    }

    private static void sh(Path dir, String... args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.directory(dir.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.waitFor() != 0) {
            throw new IllegalStateException("git failed: " + String.join(" ", args) + "\n" + output);
        }
    }

    @Test
    void debugImport() throws Exception {
        Path remote = temp.resolve("dbg.git");
        sh(temp, "git", "init", "--bare", "-b", "main", remote.toString());
        Path dev = temp.resolve("dbg-dev");
        sh(temp, "git", "clone", remote.toString(), dev.toString());
        sh(dev, "git", "config", "user.name", "dev");
        sh(dev, "git", "config", "user.email", "dev@example.com");
        sh(dev, "git", "checkout", "-b", "main");
        Files.createDirectories(dev.resolve("docs"));
        Files.writeString(dev.resolve("docs/a.md"), "# A\n", StandardCharsets.UTF_8);
        sh(dev, "git", "add", "-A");
        sh(dev, "git", "-c", "user.name=dev", "-c", "user.email=dev@example.com", "commit", "-m", "seed");
        sh(dev, "git", "push", "origin", "main");
        adminService.createSpace("dbg", "DBG", null, remote.toString(), "main", List.of(new SyncAdminService.RootSpec("docs")));
        var space = spaceRepository.findOneBySlug("dbg").orElseThrow();
        System.out.println("DBG roots=" + space.getSyncRootses().size());
        System.out.println("DBG sync_root rows=" + jdbc.queryForObject("SELECT count(*) FROM sync_root", Long.class));
        var report = importService.importSpace("dbg", false, 20);
        System.out.println("DBG report=" + report);
        var pages = pageRepository.findLiveBySpace(space.getId());
        for (var p : pages) {
            System.out.println("DBG page id=" + p.getId() + " title=" + p.getTitle() + " kind=" + p.getKind() + " gitPath=" + p.getGitPath());
        }
    }
}
