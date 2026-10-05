package com.yuzhi.dts.wiki.service.wiki.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.repository.*;
import com.yuzhi.dts.wiki.service.wiki.AttachmentService;
import com.yuzhi.dts.wiki.service.wiki.GitPageReadOnlyException;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Acceptance against disposable PostgreSQL and local bare Git repositories. */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = "ROLE_ADMIN")
class ManifestInboundIT {
    @TempDir static Path storage;
    @TempDir Path temp;
    @Autowired WikiProperties properties;
    @Autowired GitRepoManager git;
    @Autowired ContentManifestService manifest;
    @Autowired GitSyncScheduler scheduler;
    @Autowired SpaceRepository spaces;
    @Autowired SyncRootRepository roots;
    @Autowired SyncStateRepository states;
    @Autowired PageRepository pages;
    @Autowired PageVersionRepository versions;
    @Autowired SyncOutboxRepository outbox;
    @Autowired AttachmentRepository attachments;
    @Autowired AttachmentService attachmentService;
    @Autowired PageService pageService;
    @Autowired MockMvc mvc;
    private Path developer;
    private Path remote;

    @DynamicPropertySource
    static void paths(DynamicPropertyRegistry registry) {
        registry.add("application.wiki.repos-dir", () -> storage.resolve("repos").toString());
        registry.add("application.wiki.ssh-keys-dir", () -> storage.resolve("secrets").toString());
        registry.add("application.wiki.attachments-dir", () -> storage.resolve("blobs").toString());
        registry.add("application.wiki.outbound-enabled", () -> false);
    }

    @BeforeEach
    void fixture() throws Exception {
        remote = temp.resolve("content.git");
        developer = temp.resolve("developer");
        git.runIn(temp, List.of("init", "--bare", "-b", "main", remote.toString()), Map.of(), 10);
        git.runIn(temp, List.of("clone", remote.toString(), developer.toString()), Map.of(), 10);
        git.runIn(developer, List.of("config", "user.name", "Fixture author"), Map.of(), 10);
        git.runIn(developer, List.of("config", "user.email", "fixture@example.test"), Map.of(), 10);
        properties.setOutboundEnabled(false);
        properties.getContent().setRepoUrl(remote.toString());
        properties.getContent().setBranch("main");
        properties.getContent().setManifestPath("inventory/spaces.yml");
        properties.getContent().setDeployKeyPath(storage.resolve("secrets/content.key").toString());
        write("inventory/spaces.yml", inventory(false));
        write("docs/alpha/README.md", "# Team notes\n");
        write("docs/alpha/page.md", " \r\n# 历史\r\nFirst\r\n\r\n");
        write("docs/alpha/.ignored.md", "# Hidden\n");
        write("docs/alpha/checksums.json", "{}");
        write("docs/outside.md", "# Outside\n");
        Files.createSymbolicLink(developer.resolve("docs/alpha/link.md"), Path.of("../outside.md"));
        commit("initial", "2026-01-01T10:00:00Z");
        String firstCommit = git.runIn(developer, List.of("rev-parse", "HEAD"), Map.of(), 10);
        write("docs/alpha/page.md", " \r\n# 历史\r\nLatest\r\n\r\n");
        write("docs/alpha/bad.md", "---\ntype: task\nid: broken\nstatus: INVALID\n---\n# Lenient import\n");
        write("docs/alpha/assets/proof.png", "fixture-image-bytes");
        git.runIn(developer, List.of("add", "-A"), Map.of(), 10);
        git.runIn(developer, List.of("update-index", "--add", "--cacheinfo", "160000," + firstCommit + ",docs/alpha/foreign"), Map.of(), 10);
        commitStaged("latest", "2026-02-02T10:00:00Z");
        assertThat(git.runIn(developer, List.of("ls-tree", "HEAD", "--", "docs/alpha/foreign"), Map.of(), 10)).startsWith("160000 ");
    }

    @Test void diagramBundleImportsFromGitAndRemainsReadOnly() throws Exception {
        write("docs/alpha/diagram.md", "# Diagram\n\n```archify src=\"./diagrams/flow.archify.json\" height=\"560\"\n![Flow](diagrams/flow.png)\n```\n");
        write("docs/alpha/diagrams/flow.archify.json", "{\"title\":\"Flow\"}");
        write("docs/alpha/diagrams/flow.html", "<html><script>window.fixture=true</script></html>");
        Files.write(developer.resolve("docs/alpha/diagrams/flow.png"), java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jXnQAAAAASUVORK5CYII="));
        commit("diagram bundle", "2026-03-03T10:00:00Z"); scheduler.run();
        var space = spaces.findOneBySlug("team-alpha").orElseThrow();
        var diagram = pages.findLiveBySpace(space.getId()).stream().filter(p -> "docs/alpha/diagram.md".equals(p.getGitPath())).findFirst().orElseThrow();
        assertThat(attachmentService.resolveRaw(diagram.getId(), "diagrams/flow.archify.json").getFileName()).isEqualTo("flow.archify.json");
        assertThat(attachmentService.resolveRaw(diagram.getId(), "diagrams/flow.png").getMimeType()).isEqualTo("image/png");
        mvc.perform(get("/api/wiki/pages/{id}/raw/diagrams/flow.html", diagram.getId()).with(user("reader").roles("SPACE_TEAM_READERS")))
            .andExpect(status().isOk()).andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("sandbox allow-scripts")));
        mvc.perform(get("/api/wiki/pages/{id}", diagram.getId()).with(user("reader").roles("SPACE_TEAM_READERS")))
            .andExpect(jsonPath("$.gitReadOnly").value(true));
        mvc.perform(get("/api/wiki/pages/{id}/raw/diagrams/flow.html", diagram.getId()).with(user("outsider").roles("USER")))
            .andExpect(status().isNotFound());
        write("docs/alpha/diagrams/flow.archify.json", "{\"title\":\"Revised\"}");
        commit("diagram revision", "2026-04-04T10:00:00Z"); scheduler.run();
        mvc.perform(get("/api/wiki/pages/{id}/raw/diagrams/flow.archify.json", diagram.getId()).with(user("reader").roles("SPACE_TEAM_READERS")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Revised")));
        Files.delete(developer.resolve("docs/alpha/diagrams/flow.html"));
        commit("diagram HTML removed", "2026-05-05T10:00:00Z"); scheduler.run();
        mvc.perform(get("/api/wiki/pages/{id}/raw/diagrams/flow.html", diagram.getId()).with(user("reader").roles("SPACE_TEAM_READERS")))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/pages/{id}/raw/diagrams/flow.png", diagram.getId()).with(user("reader").roles("SPACE_TEAM_READERS")))
            .andExpect(status().isOk());
    }

    private String inventory(boolean second) {
        return "version: 1\nspaces:\n  - slug: team-alpha\n    name: Team Alpha\n    role: space-team-readers\n    roots: [docs/alpha]\n"
            + (second ? "  - slug: team-beta\n    name: Team Beta\n    role: space-beta-readers\n    roots: [docs/beta]\n" : "");
    }

    private void write(String path, String content) throws Exception {
        Path target = developer.resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }

    private void commit(String message, String date) {
        git.runIn(developer, List.of("add", "-A"), Map.of(), 10);
        commitStaged(message, date);
    }

    private void commitStaged(String message, String date) {
        git.runIn(developer, List.of("commit", "-m", message), Map.of("GIT_AUTHOR_DATE", date, "GIT_COMMITTER_DATE", date), 10);
        git.runIn(developer, List.of("push", "origin", "main"), Map.of(), 10);
    }

    private Page page(String slug, String path) {
        var space = spaces.findOneBySlug(slug).orElseThrow();
        return pages.findLiveBySpace(space.getId()).stream().filter(p -> path.equals(p.getGitPath())).findFirst().orElseThrow();
    }

    @Test
    void initialImportPreservesRawLatestHistoryAssetsAndRootIsolation() throws Exception {
        scheduler.run();
        Page page = page("team-alpha", "docs/alpha/page.md");
        assertThat(page.getCurrentVersion().getContentMd()).isEqualTo(" \r\n# 历史\r\nLatest\r\n\r\n");
        var history = versions.findByPageIdOrderByVersionNoDesc(page.getId());
        assertThat(history).hasSize(2);
        assertThat(history.getFirst().getCreatedAt()).isEqualTo(java.time.Instant.parse("2026-02-02T10:00:00Z"));
        assertThat(history.getLast().getCreatedAt()).isEqualTo(java.time.Instant.parse("2026-01-01T10:00:00Z"));
        assertThat(pageService.getPage(page.getId()).gitReadOnly()).isTrue();
        assertThat(pageService.getPage(page("team-alpha", "docs/alpha/bad.md").getId()).meta().valid()).isFalse();
        var visible = pages.findLiveBySpace(page.getSpace().getId());
        assertThat(visible).extracting(Page::getGitPath).doesNotContain("docs/outside.md", "docs/alpha/.ignored.md", "docs/alpha/link.md", "docs/alpha/foreign", "docs/alpha/checksums.json");
        var attachment = attachmentService.resolveRaw(page.getId(), "./assets/proof.png");
        assertThat(attachment.getGitPath()).isEqualTo("docs/alpha/assets/proof.png");
        assertThat(attachment.getSize()).isEqualTo("fixture-image-bytes".length());
        assertThat(attachmentService.list(page.getId())).hasSize(1);
        assertThat(outbox.count()).isZero();
        long count = versions.count();
        scheduler.run();
        assertThat(versions.count()).isEqualTo(count);
    }

    @Test
    void invalidInventoryBlocksAllChangesAndNewSpacesImportOnTheNextValidCycle() throws Exception {
        scheduler.run();
        String checkpoint = manifest.snapshot().commit();
        long count = versions.count();
        write("docs/alpha/page.md", "# Must wait for valid inventory\n");
        write("inventory/spaces.yml", inventory(false).replace("version: 1", "version: 2"));
        commit("invalid inventory", "2026-03-01T10:00:00Z");
        assertThatThrownBy(manifest::refresh).isInstanceOf(IllegalArgumentException.class);
        scheduler.run();
        assertThat(versions.count()).isEqualTo(count);
        assertThat(manifest.snapshot().commit()).isEqualTo(checkpoint);
        write("docs/beta/new.md", "# New space\n");
        write("inventory/spaces.yml", inventory(true));
        commit("add a space", "2026-03-02T10:00:00Z");
        scheduler.run();
        assertThat(page("team-beta", "docs/beta/new.md").getCurrentVersion().getContentMd()).isEqualTo("# New space\n");
        assertThat(page("team-alpha", "docs/alpha/page.md").getCurrentVersion().getContentMd()).isEqualTo("# Must wait for valid inventory\n");
        assertThat(git.keyPath("team-alpha")).isEqualTo(git.keyPath("team-beta"));
        Long retainedId = page("team-beta", "docs/beta/new.md").getId();
        write("inventory/spaces.yml", inventory(false));
        write("docs/beta/new.md", "# Removed space must stop syncing\n");
        commit("remove a space", "2026-03-03T10:00:00Z");
        scheduler.run();
        assertThat(pageService.getPage(retainedId).contentMd()).isEqualTo("# New space\n");
        assertThat(roots.findBySpaceWithMount(spaces.findOneBySlug("team-beta").orElseThrow().getId())).allMatch(root -> !root.getEnabled());
        assertThat(outbox.count()).isZero();
    }

    @Test
    void gitMutationsReturn409NativeContentRemainsEditableAndUnknownRolesGet404() throws Exception {
        scheduler.run();
        var source = page("team-alpha", "docs/alpha/page.md");
        Long id = source.getId();
        Long rootId = pageService.getSpace("team-alpha").rootPageId();
        long before = versions.count();
        mvc.perform(get("/api/wiki/pages/{id}", id).with(user("reader").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SPACE_TEAM_READERS"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.gitReadOnly").value(true)).andExpect(jsonPath("$.editable").value(false));
        mvc.perform(get("/api/wiki/pages/{id}", id).with(user("stranger").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_SPACE_TEAM_ALPHA"))))
            .andExpect(status().isNotFound());
        mvc.perform(put("/api/wiki/pages/{id}/content", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"baseVersionNo\":2,\"contentMd\":\"Changed\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.errorKey").value("GIT_PAGE_READ_ONLY"));
        mvc.perform(patch("/api/wiki/pages/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Renamed\"}"))
            .andExpect(status().isConflict());
        mvc.perform(delete("/api/wiki/pages/{id}", id).with(csrf())).andExpect(status().isConflict());
        mvc.perform(post("/api/wiki/pages/{id}/copy", id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"targetParentId\":" + rootId + "}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/api/wiki/pages/{id}/restore", id).with(csrf())).andExpect(status().isConflict());
        mvc.perform(multipart("/api/wiki/pages/{id}/attachments", id).file(new MockMultipartFile("file", "proof.png", "image/png", new byte[]{1})).with(csrf()))
            .andExpect(status().isConflict());
        adminContext();
        var asset = attachmentService.resolveRaw(id, "assets/proof.png");
        mvc.perform(delete("/api/wiki/attachments/{id}", asset.getId()).with(csrf())).andExpect(status().isConflict());
        adminContext();
        assertThatThrownBy(() -> pageService.deletePage(rootId)).isInstanceOf(GitPageReadOnlyException.class);
        assertThat(versions.count()).isEqualTo(before);
        var nativePage = pageService.createPage("team-alpha", new PageDtos.CreatePageRequest(rootId, "Native notes", "NATIVE", "# Native\n"));
        assertThat(nativePage.gitReadOnly()).isFalse();
        assertThat(nativePage.editable()).isTrue();
        pageService.saveContent(nativePage.id(), new PageDtos.SaveContentRequest(1, "# Native edited\n", null));
        assertThat(pageService.getPage(nativePage.id()).contentMd()).isEqualTo("# Native edited\n");
        assertThat(outbox.count()).isZero();
    }

    private static void adminContext() {
        org.springframework.security.test.context.TestSecurityContextHolder.setAuthentication(
            new org.springframework.security.authentication.TestingAuthenticationToken("admin", "unused", "ROLE_ADMIN"));
    }

    @Test
    void directorySymlinksAndGitlinksCannotBeManifestRoots() throws Exception {
        scheduler.run();
        write("inventory/spaces.yml", inventory(false).replace("roots: [docs/alpha]", "roots: [docs/alpha/foreign]"));
        commit("gitlink root", "2026-03-01T10:00:00Z");
        assertThatThrownBy(manifest::refresh).isInstanceOf(IllegalArgumentException.class);
        assertThat(roots.findBySpaceWithMount(spaces.findOneBySlug("team-alpha").orElseThrow().getId())).extracting(root -> root.getRepoPath()).containsExactly("docs/alpha");
    }

    @Test
    void newRootsAndReenabledRootsImportFilesAddedWhilePaused() throws Exception {
        scheduler.run();
        write("docs/extra/new.md", "# Extra root\n");
        write("inventory/spaces.yml", inventory(false).replace("roots: [docs/alpha]", "roots: [docs/alpha, docs/extra]"));
        commit("add root", "2026-03-01T10:00:00Z");
        scheduler.run();
        assertThat(page("team-alpha", "docs/extra/new.md").getCurrentVersion().getContentMd()).isEqualTo("# Extra root\n");
        write("inventory/spaces.yml", inventory(false));
        commit("pause root", "2026-03-02T10:00:00Z");
        scheduler.run();
        Files.delete(developer.resolve("docs/extra/new.md"));
        write("docs/extra/while-paused.md", "# Added while paused\n");
        write("inventory/spaces.yml", inventory(false).replace("roots: [docs/alpha]", "roots: [docs/alpha, docs/extra]"));
        commit("resume root", "2026-03-03T10:00:00Z");
        scheduler.run();
        assertThat(page("team-alpha", "docs/extra/while-paused.md").getCurrentVersion().getContentMd()).isEqualTo("# Added while paused\n");
        assertThat(pages.findLiveBySpace(spaces.findOneBySlug("team-alpha").orElseThrow().getId())).extracting(Page::getGitPath).doesNotContain("docs/extra/new.md");
    }

    @Test
    void historicalRenamesAndIncrementalRenamesKeepVersionsAndPageIdentity() throws Exception {
        git.runIn(developer, List.of("mv", "docs/alpha/page.md", "docs/alpha/renamed.md"), Map.of(), 10);
        commit("rename before import", "2026-03-01T10:00:00Z");
        scheduler.run();
        Page page = page("team-alpha", "docs/alpha/renamed.md");
        assertThat(versions.findByPageIdOrderByVersionNoDesc(page.getId())).hasSize(2);
        Long id = page.getId();
        git.runIn(developer, List.of("mv", "docs/alpha/renamed.md", "docs/alpha/最终版本.md"), Map.of(), 10);
        commit("rename after import", "2026-03-02T10:00:00Z");
        scheduler.run();
        assertThat(page("team-alpha", "docs/alpha/最终版本.md").getId()).isEqualTo(id);
        assertThat(versions.findByPageIdOrderByVersionNoDesc(id)).hasSize(2);
    }
}
