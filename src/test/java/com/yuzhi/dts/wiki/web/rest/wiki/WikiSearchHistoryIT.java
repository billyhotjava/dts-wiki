package com.yuzhi.dts.wiki.web.rest.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Attachment;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.repository.AttachmentRepository;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = "ROLE_ADMIN")
class WikiSearchHistoryIT {
    @Autowired MockMvc mvc;
    @Autowired PageService pages;
    @Autowired PageRepository pageRepository;
    @Autowired PageVersionRepository versions;
    @Autowired SpaceRepository spaces;
    @Autowired AttachmentRepository attachments;
    long id;
    long root;

    @BeforeEach
    void seed() {
        spaces.saveAndFlush(new Space().slug("search-team").name("Search team").archived(false));
        spaces.saveAndFlush(new Space().slug("private-team").name("Private team").archived(false));
        root = pages.createPage("search-team", new PageDtos.CreatePageRequest(null, "Root", "FOLDER", null)).id();
        id = pages.createPage("search-team", new PageDtos.CreatePageRequest(root, "中文交付", "NATIVE", "# 中文交付\n\n旧版专用词，交付说明。\n")).id();
        pages.createPage("private-team", new PageDtos.CreatePageRequest(null, "Private secret", "NATIVE", "# Private\n中文交付秘密\n"));
    }

    @Test
    void chineseRetrievalFiltersByCallerSpacesAndLiteralQuery() throws Exception {
        var reader = user("reader").authorities(() -> "ROLE_SPACE_SEARCH_TEAM");
        mvc.perform(get("/api/wiki/search").param("q", "中文交付").with(reader))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].pageId").value(id)).andExpect(jsonPath("$.items[0].spaceSlug").value("search-team"));
        mvc.perform(get("/api/wiki/search").param("q", "中文").param("space", "private-team").with(reader))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/search").param("q", "中文").with(user("outsider").authorities(() -> "ROLE_USER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/wiki/search").param("q", "%' OR true --"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/wiki/search").param("q", "")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/wiki/search").param("q", "中文").param("size", "201")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/wiki/search").param("q", "中文").param("since", "2099-01-01T00:00:00Z"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void onlyCurrentContentAndLiveAttachmentNamesAreSearchable() throws Exception {
        var owner = pageRepository.findById(root).orElseThrow();
        Attachment attachment = new Attachment().page(owner).fileName("验收附件.pdf")
            .mimeType("application/pdf").size(10L).sha256("a".repeat(64))
            .createdAt(Instant.now());
        attachments.saveAndFlush(attachment);
        mvc.perform(get("/api/wiki/search").param("q", "验收附件").param("space", "search-team"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].pageId").value(root));
        attachment.setDeletedAt(Instant.now()); attachments.saveAndFlush(attachment);
        mvc.perform(get("/api/wiki/search").param("q", "验收附件")).andExpect(jsonPath("$.total").value(0));
        save("# 新版标题\n新版本专用词\n", "local-agent");
        mvc.perform(get("/api/wiki/search").param("q", "旧版专用词")).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/wiki/search").param("q", "新版本专用词")).andExpect(jsonPath("$.total").value(1));
        mvc.perform(delete("/api/wiki/pages/{id}", id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/search").param("q", "新版本专用词")).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void restoreAppendsAnImmutableVersionAndRequiresCurrentBase() throws Exception {
        save("# Version two\nChanged\n", "local-agent");
        mvc.perform(get("/api/wiki/pages/{id}/versions", id).param("size", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items[0].versionNo").value(2)).andExpect(jsonPath("$.items[0].viaAgent").value("local-agent"))
            .andExpect(jsonPath("$.items[0].contentMd").isEmpty());
        mvc.perform(post("/api/wiki/pages/{id}/versions/1/restore", id).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":2}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.versionNo").value(3));
        mvc.perform(get("/api/wiki/pages/{id}/versions/3", id)).andExpect(jsonPath("$.source").value("RESTORE"));
        mvc.perform(get("/api/wiki/pages/{id}/versions/1", id))
            .andExpect(jsonPath("$.contentMd").value("# 中文交付\n\n旧版专用词，交付说明。\n"));
        mvc.perform(post("/api/wiki/pages/{id}/versions/1/restore", id).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":2}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.currentVersionNo").value(3));
        mvc.perform(post("/api/wiki/pages/{id}/versions/1/restore", id).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":3}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.versionNo").value(3));
        assertThat(versions.findByPageIdOrderByVersionNoDesc(id)).hasSize(3);
    }

    @Test
    void historyAndActivityFollowSpaceAuthorizationAndGitOwnership() throws Exception {
        var reader = user("reader").authorities(() -> "ROLE_SPACE_SEARCH_TEAM");
        var outsider = user("outsider").authorities(() -> "ROLE_USER");
        mvc.perform(get("/api/wiki/pages/{id}/versions", id).with(outsider)).andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/pages/{id}/versions/1", id).with(outsider)).andExpect(status().isNotFound());
        mvc.perform(post("/api/wiki/pages/{id}/versions/1/restore", id).with(reader).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":1}"))
            .andExpect(status().isForbidden());
        String activity = mvc.perform(get("/api/wiki/activity").with(reader)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(activity).contains("中文交付").doesNotContain("Private secret");
        mvc.perform(get("/api/wiki/activity").param("space", "private-team").with(reader)).andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/activity").param("author", "nobody")).andExpect(jsonPath("$.length()").value(0));
        var page = pageRepository.findById(id).orElseThrow();
        page.setKind(com.yuzhi.dts.wiki.domain.enumeration.PageKind.GIT);
        pageRepository.saveAndFlush(page);
        mvc.perform(post("/api/wiki/pages/{id}/versions/1/restore", id).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":1}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.errorKey").value("GIT_PAGE_READ_ONLY"));
    }

    private void save(String markdown, String agent) throws Exception {
        String body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
            java.util.Map.of("baseVersionNo", 1, "contentMd", markdown));
        mvc.perform(put("/api/wiki/pages/{id}/content", id).with(csrf()).header("X-Wiki-Agent", agent).contentType("application/json").content(body))
            .andExpect(status().isOk());
    }
}
