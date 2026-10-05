package com.yuzhi.dts.wiki.web.rest.wiki;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.repository.*;
import com.yuzhi.dts.wiki.service.wiki.*;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username="collab-alice", authorities="ROLE_ADMIN")
class WikiCollaborationIT {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PageService pages;
    @Autowired PageRepository pageRepository;
    @Autowired SpaceRepository spaces;
    @Autowired UserRepository users;
    @Autowired org.springframework.jdbc.core.JdbcTemplate sql;
    @MockitoBean CurrentIdentityService identity;
    long page;
    @BeforeEach void seed() {
        for (String login : new String[]{"collab-alice", "collab-bob", "collab-outsider"}) {
            User user = new User(); user.setId(login); user.setLogin(login); user.setActivated(true); user.setFirstName(login); users.saveAndFlush(user);
        }
        spaces.saveAndFlush(new Space().slug("collab-team").name("Collaboration").archived(false));
        long root = pages.createPage("collab-team", new PageDtos.CreatePageRequest(null, "Root", "FOLDER", null)).id();
        page = pages.createPage("collab-team", new PageDtos.CreatePageRequest(root, "Knowledge", "NATIVE", "# Knowledge\n")).id();
        when(identity.lookup("collab-bob")).thenReturn(new CurrentIdentityService.Identity(true, Set.of("ROLE_SPACE_COLLAB_TEAM"), null));
        when(identity.lookup("collab-alice")).thenReturn(new CurrentIdentityService.Identity(true, Set.of("ROLE_SPACE_COLLAB_TEAM"), null));
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor bob() { return user("collab-bob").authorities(() -> "ROLE_SPACE_COLLAB_TEAM"); }
    @Test void favoritesAreIdempotentAndScopeBeforeCounting() throws Exception {
        for (int i=0;i<2;i++) mvc.perform(put("/api/wiki/me/favorites/{id}", page).with(bob()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/me/favorites").with(bob())).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/wiki/me/favorites").with(user("collab-bob").roles("USER"))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(put("/api/wiki/me/favorites/{id}", page).with(user("collab-outsider").roles("USER")).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/wiki/me/favorites/{id}", page).with(bob()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/me/favorites").with(bob())).andExpect(jsonPath("$.total").value(0));
    }
    @Test void recentVisitsUseServerTimeAndTrimToFifty() throws Exception {
        // Preseed 51 valid pages; one visit atomically upserts and trims the user's list.
        long root = pageRepository.findById(page).orElseThrow().getParent().getId();
        for (int i=0;i<51;i++) {
            long id = pages.createPage("collab-team", new PageDtos.CreatePageRequest(root,"Page " + i,"NATIVE",null)).id();
            sql.update("INSERT INTO page_view(user_id,page_id,viewed_at) VALUES('collab-bob',?,CURRENT_TIMESTAMP-interval '1 day')", id);
        }
        mvc.perform(post("/api/wiki/pages/{id}/view", page).with(bob()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/me/recent-pages").with(bob())).andExpect(jsonPath("$.total").value(50)).andExpect(jsonPath("$.items[0].pageId").value(page));
    }
    @Test void gitCommentsAreWikiOnlyAndRepliesHaveOneLevel() throws Exception {
        var git = pageRepository.findById(page).orElseThrow(); git.setKind(PageKind.GIT); pageRepository.saveAndFlush(git);
        long root = json.readTree(mvc.perform(post("/api/wiki/pages/{id}/comments", page).with(bob()).with(csrf()).contentType("application/json")
            .content("{\"bodyMd\":\"Hello @collab-alice <script>bad()</script>\"}")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        long reply = json.readTree(mvc.perform(post("/api/wiki/pages/{id}/comments", page).with(bob()).with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(java.util.Map.of("bodyMd","Reply","parentId",root)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(post("/api/wiki/pages/{id}/comments", page).with(bob()).with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(java.util.Map.of("bodyMd","Deep","parentId",reply)))).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/wiki/comments/{id}", root).with(user("collab-alice").authorities(() -> "ROLE_SPACE_COLLAB_TEAM")).with(csrf()).contentType("application/json").content("{\"bodyMd\":\"Hijack\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/wiki/comments/{id}", root).with(bob()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/pages/{id}/comments", page).with(bob())).andExpect(jsonPath("$.items[0].deleted").value(true)).andExpect(jsonPath("$.items[0].bodyMd").value(""))
            .andExpect(jsonPath("$.items[0].replies[0].bodyMd").value("Reply"));
        assertThat(sql.queryForObject("SELECT count(*) FROM sync_outbox WHERE page_id=?", Long.class, page)).isZero();
        assertThat(pageRepository.findById(page).orElseThrow().getCurrentVersion().getVersionNo()).isEqualTo(1);
    }
    @Test void mutePersistsButMentionIntentStillExists() throws Exception {
        mvc.perform(delete("/api/wiki/pages/{id}/watch", page).with(bob()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post("/api/wiki/pages/{id}/comments", page).with(bob()).with(csrf()).contentType("application/json").content("{\"bodyMd\":\"Comment\"}"))
            .andExpect(status().isCreated());
        assertThat(sql.queryForObject("SELECT muted FROM page_watch WHERE page_id=? AND user_id='collab-bob'", Boolean.class, page)).isTrue();
        mvc.perform(post("/api/wiki/pages/{id}/comments", page).with(user("collab-alice").roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"bodyMd\":\"Hi @collab-bob\"}"))
            .andExpect(status().isCreated());
        assertThat(sql.queryForObject("SELECT count(*) FROM notification WHERE page_id=? AND recipient_id='collab-bob' AND type='MENTION'", Long.class, page)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT count(*) FROM notification WHERE page_id=? AND recipient_id='collab-bob' AND type='PAGE_UPDATED'", Long.class, page)).isZero();
        mvc.perform(put("/api/wiki/pages/{id}/watch", page).with(bob()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/pages/{id}/personal", page).with(bob())).andExpect(jsonPath("$.watching").value(true));
    }
    @Test void mentionDirectoryAndNotificationsRecheckIdentity() throws Exception {
        mvc.perform(get("/api/wiki/users/mention").param("spaceSlug","collab-team").param("q","collab-bob")).andExpect(jsonPath("$[0].id").value("collab-bob"))
            .andExpect(jsonPath("$[0].email").doesNotExist());
        when(identity.lookup("collab-bob")).thenReturn(new CurrentIdentityService.Identity(true, Set.of(), null));
        mvc.perform(get("/api/wiki/users/mention").param("spaceSlug","collab-team").param("q","collab-bob")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/wiki/me/notifications").with(bob())).andExpect(jsonPath("$.unread").value(0));
        when(identity.lookup("collab-bob")).thenThrow(new IdentityUnavailableException());
        mvc.perform(get("/api/wiki/users/mention").param("spaceSlug","collab-team").param("q","collab-bob")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/wiki/me/notifications/unread-count").with(bob())).andExpect(status().isServiceUnavailable());
        clearInvocations(identity);
        mvc.perform(get("/api/wiki/users/mention").param("spaceSlug","collab-team")).andExpect(jsonPath("$.length()").value(0));
        verifyNoInteractions(identity);
    }
}
