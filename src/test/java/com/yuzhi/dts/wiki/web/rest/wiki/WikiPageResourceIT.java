package com.yuzhi.dts.wiki.web.rest.wiki;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * HTTP contract for /api/wiki (Sprint-6 W4): status codes, 404/403/409 semantics,
 * errorKey envelope. Business flows are covered in PageServiceIT.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = { "ROLE_ADMIN" })
class WikiPageResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpaceRepository spaceRepository;

    @BeforeEach
    void initSpace() {
        if (spaceRepository.findOneBySlug("w4http").isEmpty()) {
            spaceRepository.saveAndFlush(new Space().slug("w4http").name("W4 HTTP").archived(false));
        }
    }

    private String slug = "w4http";

    @Test
    void fullPageFlow() throws Exception {
        // create root
        String root = mockMvc
            .perform(post("/api/wiki/spaces/{slug}/pages", slug).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Root\",\"kind\":\"FOLDER\",\"contentMd\":\"# root\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Root"))
            .andReturn()
            .getResponse()
            .getContentAsString();
        long rootId = ((Number) JsonPath.read(root, "$.id")).longValue();

        // tree + read
        mockMvc.perform(get("/api/wiki/spaces/{slug}/tree", slug)).andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Root"));
        mockMvc.perform(get("/api/wiki/pages/{id}", rootId)).andExpect(status().isOk()).andExpect(jsonPath("$.versionNo").value(1));

        // create child, save v2, stale save -> 409 with errorKey
        String child = mockMvc
            .perform(post("/api/wiki/spaces/{slug}/pages", slug).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"parentId\":" + rootId + ",\"title\":\"Child\",\"contentMd\":\"v1\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        long childId = ((Number) JsonPath.read(child, "$.id")).longValue();
        mockMvc
            .perform(put("/api/wiki/pages/{id}/content", childId).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"baseVersionNo\":1,\"contentMd\":\"v2\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionNo").value(2));
        mockMvc
            .perform(put("/api/wiki/pages/{id}/content", childId).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"baseVersionNo\":1,\"contentMd\":\"v3\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorKey").value("PAGE_VERSION_CONFLICT"))
            .andExpect(jsonPath("$.currentVersionNo").value(2));

        // delete -> 404 on read, visible in trash; restore -> readable
        mockMvc.perform(delete("/api/wiki/pages/{id}", childId).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/wiki/pages/{id}", childId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/wiki/spaces/{slug}/trash", slug)).andExpect(status().isOk()).andExpect(jsonPath("$").value(hasItem((int) childId)));
        mockMvc.perform(post("/api/wiki/pages/{id}/restore", childId).with(csrf())).andExpect(status().isOk());
        mockMvc.perform(get("/api/wiki/pages/{id}", childId)).andExpect(status().isOk());
    }

    @Test
    void unknownSpaceReadsAs404() throws Exception {
        mockMvc.perform(get("/api/wiki/spaces/nope/tree")).andExpect(status().isNotFound()).andExpect(jsonPath("$.errorKey").value("SPACE_NOT_VISIBLE"));
    }

    @Test
    @WithMockUser(authorities = { "ROLE_USER" })
    void nonMemberSees404() throws Exception {
        mockMvc.perform(get("/api/wiki/spaces/w4http/tree")).andExpect(status().isNotFound());
        mockMvc
            .perform(post("/api/wiki/spaces/w4http/pages").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"X\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_USER", "ROLE_SPACE_W4HTTP" })
    void readOnlyMemberGets403OnWrite() throws Exception {
        mockMvc.perform(get("/api/wiki/spaces/w4http/tree")).andExpect(status().isOk());
        mockMvc
            .perform(post("/api/wiki/spaces/w4http/pages").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"X\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void spacesListOnlyShowsMine() throws Exception {
        mockMvc.perform(get("/api/wiki/spaces")).andExpect(status().isOk()).andExpect(jsonPath("$[*].slug").value(hasItem("w4http")));
    }
}
