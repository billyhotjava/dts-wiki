package com.yuzhi.dts.wiki.web.rest.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
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
class WikiContentResourceIT {
    @Autowired MockMvc mvc;
    @Autowired PageService pages;
    @Autowired SpaceRepository spaces;
    @Autowired PageRepository pageRepository;
    private long task;
    private java.util.Map<String, Long> roots;

    @BeforeEach
    void seed() {
        spaces.saveAndFlush(new Space().slug("query-team").name("Query team").description("Team notes").archived(false));
        spaces.saveAndFlush(new Space().slug("private-team").name("Private team").archived(false));
        roots = java.util.Map.of(
            "query-team", pages.createPage("query-team", new PageDtos.CreatePageRequest(null, "Root", "FOLDER", null)).id(),
            "private-team", pages.createPage("private-team", new PageDtos.CreatePageRequest(null, "Root", "FOLDER", null)).id());
        task = create("query-team", "Delivery task", "DONE", "P0", "delivery", "alice", "S8", "F2");
        create("query-team", "Other task", "READY", "P1", "other", "bob", "S9", "F3");
        create("private-team", "Private task", "DONE", "P0", "delivery", "alice", "S8", "F2");
    }

    private long create(String space, String title, String status, String priority, String tag, String owner, String sprint, String feature) {
        String md = "---\ntype: task\nid: " + sprint + "/" + feature + "/T01\ntitle: " + title + "\nstatus: " + status
            + "\npriority: " + priority + "\nsprint: " + sprint + "\nfeature: " + feature + "\nowner: " + owner
            + "\ntags: [" + tag + "]\n---\n# " + title + "\n\n中文交付知识。\n";
        return pages.createPage(space, new PageDtos.CreatePageRequest(roots.get(space), title, "NATIVE", md)).id();
    }

    @Test
    void queryCombinesFiltersAndProvidesStablePagination() throws Exception {
        mvc.perform(get("/api/wiki/query").param("space", "query-team").param("type", "task")
            .param("status", "DONE,IN_PROGRESS").param("priority", "P0").param("owner", "alice")
            .param("sprint", "S8").param("feature", "F2").param("tag", "delivery").param("q", "Delivery").param("size", "1"))
            .andExpect(status().isOk()).andExpect(header().string("X-Total-Count", "1"))
            .andExpect(jsonPath("$[0].pageId").value(task)).andExpect(jsonPath("$[0].docId").value("S8/F2/T01"))
            .andExpect(jsonPath("$[0].url").value("/s/query-team/p/" + task));
        mvc.perform(get("/api/wiki/query").param("space", "query-team").param("page", "1").param("size", "1"))
            .andExpect(status().isOk()).andExpect(header().string("X-Total-Count", "2")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/wiki/query").param("space", "query-team").param("q", "%' OR true --"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/wiki/query").param("space", "query-team").param("size", "201")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/wiki/query").param("space", "query-team").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/wiki/query")).andExpect(status().isBadRequest());
    }

    @Test
    void rawMarkdownUsesVersionHeadersAndStopsServingDeletedPages() throws Exception {
        mvc.perform(get("/api/wiki/pages/{id}/markdown", task)).andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"v1\"")).andExpect(header().string("X-Wiki-Version", "1"))
            .andExpect(header().string("X-Wiki-Doc-Type", "task")).andExpect(content().contentTypeCompatibleWith("text/markdown"));
        mvc.perform(put("/api/wiki/pages/{id}/content", task).with(csrf()).contentType("application/json")
            .header("X-Wiki-Agent", "team-agent").content("{\"baseVersionNo\":1,\"contentMd\":\"# Revised\\n\",\"message\":\"Update\"}"))
            .andExpect(status().isOk());
        mvc.perform(get("/api/wiki/pages/{id}/markdown", task).header("If-None-Match", "\"v1\""))
            .andExpect(status().isOk()).andExpect(header().string("ETag", "\"v2\""));
        mvc.perform(get("/api/wiki/pages/{id}/markdown", task).header("If-None-Match", "\"v2\""))
            .andExpect(status().isNotModified());
        mvc.perform(delete("/api/wiki/pages/{id}", task).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/pages/{id}/markdown", task).header("If-None-Match", "\"v2\""))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/query").param("space", "query-team"))
            .andExpect(status().isOk()).andExpect(header().string("X-Total-Count", "1"));
    }

    @Test
    void currentAuthorizationPrecedesConditionalResponsesAndIndexes() throws Exception {
        var outsider = user("outsider").authorities(() -> "ROLE_SPACE_PRIVATE_TEAM");
        mvc.perform(get("/api/wiki/query").param("space", "query-team").with(outsider)).andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/pages/{id}/markdown", task).header("If-None-Match", "\"v1\"").with(outsider))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/spaces/query-team/llms.txt").with(outsider)).andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/spaces/query-team/markdown").param("path", "anything.md").with(outsider))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/spaces/query-team/llms.txt")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "private, max-age=60, no-cache"));
        String index = mvc.perform(get("/api/wiki/spaces/query-team/llms.txt")).andReturn().getResponse().getContentAsString();
        assertThat(index).contains("Query team", "Delivery task", "/api/wiki/pages/" + task + "/markdown").doesNotContain("Private task");
        mvc.perform(delete("/api/wiki/pages/{id}", task).with(csrf())).andExpect(status().isNoContent());
        assertThat(mvc.perform(get("/api/wiki/spaces/query-team/llms.txt")).andReturn().getResponse().getContentAsString())
            .doesNotContain("Delivery task");
    }

    @Test
    void exactGitPathAndDirectoryReadTheSameRawPage() throws Exception {
        var page = pageRepository.findById(task).orElseThrow();
        page.setGitPath("notes/review");
        page.setKind(com.yuzhi.dts.wiki.domain.enumeration.PageKind.FOLDER);
        pageRepository.saveAndFlush(page);
        mvc.perform(get("/api/wiki/spaces/query-team/markdown").param("path", "notes/review/README.md"))
            .andExpect(status().isOk()).andExpect(header().string("X-Wiki-Git-Path", "notes/review"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Delivery task")));
        mvc.perform(get("/api/wiki/spaces/query-team/markdown").param("path", "../notes/review"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/api/wiki/spaces/query-team/markdown").param("path", "missing.md"))
            .andExpect(status().isNotFound());
    }
}
