package com.yuzhi.dts.wiki.web.rest.wiki;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.*;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.repository.*;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest @AutoConfigureMockMvc @Transactional
@WithMockUser(username="edit-alice",authorities="ROLE_ADMIN")
class WikiEditingIT {
    @Autowired MockMvc mvc;
    @Autowired PageService pages;
    @Autowired PageRepository pageRepository;
    @Autowired UserRepository users;
    @Autowired SpaceRepository spaces;
    long page;
    @BeforeEach void fixture() {
        for(String login:new String[]{"edit-alice","edit-bob"}) { User user=new User(); user.setId(login);user.setLogin(login);user.setActivated(true);users.saveAndFlush(user); }
        spaces.saveAndFlush(new Space().slug("edit-team").name("Editing").archived(false));
        long root=pages.createPage("edit-team",new PageDtos.CreatePageRequest(null,"Root","FOLDER",null)).id();
        page=pages.createPage("edit-team",new PageDtos.CreatePageRequest(root,"Knowledge","NATIVE","# Knowledge\n")).id();
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor alice(){return user("edit-alice").roles("ADMIN");}
    @Test void privateDraftDoesNotPublishAndSuccessfulContentSaveClearsItAtomically() throws Exception {
        mvc.perform(put("/api/wiki/pages/{id}/draft",page).with(alice()).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":1,\"contentMd\":\"Changed\"}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/pages/{id}/draft",page).with(user("edit-bob").roles("ADMIN"))).andExpect(status().isNoContent());
        assertThat(pageRepository.findById(page).orElseThrow().getCurrentVersion().getVersionNo()).isEqualTo(1);
        mvc.perform(put("/api/wiki/pages/{id}/content",page).with(alice()).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":1,\"contentMd\":\"Changed\",\"title\":\"Revised title\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.versionNo").value(2));
        mvc.perform(get("/api/wiki/pages/{id}/draft",page).with(alice())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/pages/{id}",page).with(alice())).andExpect(jsonPath("$.title").value("Revised title"));
        mvc.perform(put("/api/wiki/pages/{id}/draft",page).with(alice()).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":1,\"contentMd\":\"Changed\"}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/pages/{id}/draft",page).with(alice())).andExpect(status().isNoContent());
    }
    @Test void staleDraftKeepsBaseAndCannotOverwriteWithoutConflict() throws Exception {
        mvc.perform(put("/api/wiki/pages/{id}/draft",page).with(alice()).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":0,\"contentMd\":\"Older edits\"}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/pages/{id}/draft",page).with(alice())).andExpect(jsonPath("$.baseVersionNo").value(0));
        mvc.perform(put("/api/wiki/pages/{id}/content",page).with(alice()).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":0,\"contentMd\":\"Older edits\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.currentVersionNo").value(1));
        mvc.perform(delete("/api/wiki/pages/{id}/draft",page).with(alice()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/pages/{id}/draft",page).with(alice())).andExpect(status().isNoContent());
    }
    @Test void presenceIsSoftScopedAndGitWritesRemainBlocked() throws Exception {
        mvc.perform(post("/api/wiki/pages/{id}/editing",page).with(alice()).with(csrf())).andExpect(status().isOk());
        mvc.perform(post("/api/wiki/pages/{id}/editing",page).with(user("edit-bob").roles("ADMIN")).with(csrf())).andExpect(jsonPath("$[0].login").value("edit-alice"));
        mvc.perform(delete("/api/wiki/pages/{id}/editing",page).with(alice()).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/wiki/pages/{id}/editing",page).with(user("edit-bob").roles("ADMIN"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/wiki/pages/{id}/editing",page).with(user("reader").roles("SPACE_EDIT_TEAM")).with(csrf())).andExpect(status().isForbidden());
        var git=pageRepository.findById(page).orElseThrow();git.setKind(PageKind.GIT);pageRepository.saveAndFlush(git);
        mvc.perform(post("/api/wiki/pages/{id}/editing",page).with(alice()).with(csrf())).andExpect(status().isConflict());
        mvc.perform(put("/api/wiki/pages/{id}/draft",page).with(alice()).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":1,\"contentMd\":\"Forbidden\"}"))
            .andExpect(status().isConflict());
    }

    @Test void trashListsScopedTitlesAndReadOnlyItemsBeforePagination() throws Exception {
        pages.deletePage(page);
        Space space=spaces.findOneBySlug("edit-team").orElseThrow();
        long removed=pages.createPage("edit-team",new PageDtos.CreatePageRequest(pageRepository.findById(page).orElseThrow().getParent().getId(),"Removed source","NATIVE","Source")).id();
        var source=pageRepository.findById(removed).orElseThrow(); source.setKind(PageKind.GIT); source.setDeletedAt(java.time.Instant.now()); pageRepository.saveAndFlush(source);
        mvc.perform(get("/api/wiki/spaces/edit-team/trash/items").param("size","500").with(alice()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50)).andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.items[0].title").value("Removed source")).andExpect(jsonPath("$.items[0].restorable").value(false))
            .andExpect(jsonPath("$.items[1].title").value("Knowledge")).andExpect(jsonPath("$.items[1].restorable").value(true));
        mvc.perform(get("/api/wiki/spaces/edit-team/trash/items").with(user("reader").roles("SPACE_EDIT_TEAM")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[1].restorable").value(false));
        mvc.perform(get("/api/wiki/spaces/edit-team/trash/items").with(user("outsider").roles("USER"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/wiki/pages/{id}/restore",page).with(alice()).with(csrf())).andExpect(status().isOk());
        mvc.perform(get("/api/wiki/pages/{id}",page).with(alice())).andExpect(jsonPath("$.title").value("Knowledge"));
    }

    @Test void rejectsFutureDraftBaseAndOversizedUtf8WithoutPublishing() throws Exception {
        mvc.perform(put("/api/wiki/pages/{id}/draft",page).with(alice()).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":2,\"contentMd\":\"Future\"}"))
            .andExpect(status().isBadRequest());
        String oversized="界".repeat(666667);
        mvc.perform(put("/api/wiki/pages/{id}/draft",page).with(alice()).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":1,\"contentMd\":\""+oversized+"\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/wiki/pages/{id}/content",page).with(alice()).with(csrf()).contentType("application/json").content("{\"baseVersionNo\":1,\"contentMd\":\""+oversized+"\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/wiki/pages/{id}/draft",page).with(alice())).andExpect(status().isNoContent());
        assertThat(pageRepository.findById(page).orElseThrow().getCurrentVersion().getVersionNo()).isEqualTo(1);
    }

    @Test void nativeCreationAndTitleMutationRejectOversizeBeforeDatabaseWrites() throws Exception {
        long parent=pageRepository.findById(page).orElseThrow().getParent().getId();
        long before=pageRepository.count();
        String title="a".repeat(201);
        mvc.perform(post("/api/wiki/spaces/edit-team/pages").with(alice()).with(csrf()).contentType("application/json")
            .content("{\"parentId\":"+parent+",\"title\":\"Too large\",\"contentMd\":\""+"界".repeat(666667)+"\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/wiki/spaces/edit-team/pages").with(alice()).with(csrf()).contentType("application/json")
            .content("{\"parentId\":"+parent+",\"title\":\""+title+"\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/wiki/pages/{id}",page).with(alice()).with(csrf()).contentType("application/json").content("{\"title\":\""+title+"\"}"))
            .andExpect(status().isBadRequest());
        assertThat(pageRepository.count()).isEqualTo(before);
        long longTitle=pages.createPage("edit-team",new PageDtos.CreatePageRequest(parent,"a".repeat(200),"NATIVE",null)).id();
        assertThat(pages.copyPage(longTitle,new PageDtos.CopyPageRequest(parent,null)).title()).hasSize(200).endsWith(" (copy)");
    }
}
