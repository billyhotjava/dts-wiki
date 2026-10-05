package com.yuzhi.dts.wiki.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yuzhi.dts.wiki.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP-level subset of the 07 S2 permission matrix that is testable before the
 * business APIs exist (Sprint-6 F2/T02). Matrix users:
 * A = ROLE_USER + ROLE_SPACE_PRS, B = A + ROLE_EDITOR, C = ROLE_ADMIN.
 * Wiki-endpoint rows (spaces list, pages, comments, search, history) are covered
 * when those endpoints land (W4/W7); service-level semantics are covered by
 * {@code SpaceAccessServiceTest}.
 */
@IntegrationTest
@AutoConfigureMockMvc
class SecurityPathIT {

    @Autowired
    private MockMvc mockMvc;

    // NOTE: /management/health permitAll is not assertable here: the test profile
    // disables the health endpoint (see test application.yml). It is verified at
    // deploy time by the compose healthcheck (W3 it/baseline.md).
    @Test
    void accountRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_USER", "ROLE_SPACE_PRS" })
    void userA_isForbiddenOnEntityApis() throws Exception {
        mockMvc.perform(get("/api/spaces")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/pages")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/sync")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_USER", "ROLE_EDITOR", "ROLE_SPACE_PRS" })
    void userB_isForbiddenOnEntityApis() throws Exception {
        mockMvc.perform(get("/api/spaces")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/sync")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_USER" })
    void userD_isForbiddenOnEntityApis() throws Exception {
        mockMvc.perform(get("/api/spaces")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_ADMIN" })
    void adminC_passesSecurityOnEntityApis() throws Exception {
        mockMvc.perform(get("/api/spaces")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/pages")).andExpect(status().isNotFound());
        // no such mapping: security passes, dispatcher returns 404
        mockMvc.perform(get("/api/admin/sync")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_ADMIN" })
    void bootstrapReturnsAccountSpacesAndFlag() throws Exception {
        mockMvc
            .perform(get("/api/wiki/bootstrap"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.account").exists())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.spaces").isArray())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.canCreateSpace").value(true));
    }

    @Test
    void bootstrapRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/wiki/bootstrap")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_ADMIN" })
    void mentionListsUsers() throws Exception {
        mockMvc
            .perform(get("/api/wiki/users/mention").param("spaceSlug", "prs"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(authorities = { "ROLE_USER" })
    void mentionWithoutSpaceAccessIs404() throws Exception {
        mockMvc.perform(get("/api/wiki/users/mention").param("spaceSlug", "prs")).andExpect(status().isNotFound());
    }
}
