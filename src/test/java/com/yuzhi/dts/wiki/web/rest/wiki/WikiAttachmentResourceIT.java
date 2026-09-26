package com.yuzhi.dts.wiki.web.rest.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import java.nio.file.Path;
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

/**
 * Attachment flows (Sprint-6 W5): upload → list → download → raw preview → delete.
 * Blob root is an isolated temp dir per run.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = { "ROLE_ADMIN" })
class WikiAttachmentResourceIT {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void blobProperties(DynamicPropertyRegistry registry) {
        // resolved lazily: @TempDir static fields are injected after this method runs
        registry.add("application.wiki.attachments-dir", () -> tempDir.resolve("attachments").toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpaceRepository spaceRepository;

    private Long pageId;

    @BeforeEach
    void initPage() throws Exception {
        Space space = spaceRepository.findOneBySlug("w5att").orElseGet(() -> spaceRepository.saveAndFlush(new Space().slug("w5att").name("W5 ATT").archived(false)));
        assertThat(space.getId()).isNotNull();
        String body = mockMvc
            .perform(post("/api/wiki/spaces/w5att/pages").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Root\",\"kind\":\"FOLDER\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        pageId = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void imageUploadListDownloadRawDelete() throws Exception {
        byte[] png = new byte[] { (byte) 0x89, 'P', 'N', 'G', 1, 2, 3 };
        String uploaded = mockMvc
            .perform(multipart("/api/wiki/pages/{id}/attachments", pageId).file(new MockMultipartFile("file", "shot.png", "image/png", png)).with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fileName").value(org.hamcrest.Matchers.endsWith(".png")))
            .andExpect(jsonPath("$.markdown").value(org.hamcrest.Matchers.startsWith("![")))
            .andReturn()
            .getResponse()
            .getContentAsString();
        long attachmentId = ((Number) com.jayway.jsonpath.JsonPath.read(uploaded, "$.id")).longValue();
        String storedName = com.jayway.jsonpath.JsonPath.read(uploaded, "$.fileName");

        // list + download bytes back
        mockMvc.perform(get("/api/wiki/pages/{id}/attachments", pageId)).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value((int) attachmentId));
        byte[] downloaded = mockMvc.perform(get("/api/wiki/attachments/{id}", attachmentId)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        org.assertj.core.api.Assertions.assertThat(downloaded).isEqualTo(png);

        // editor raw preview by relative path
        byte[] raw = mockMvc.perform(get("/api/wiki/pages/{id}/raw/assets/{file}", pageId, storedName)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        org.assertj.core.api.Assertions.assertThat(raw).isEqualTo(png);

        // delete -> gone from list and download
        mockMvc.perform(delete("/api/wiki/attachments/{id}", attachmentId).with(csrf())).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/wiki/attachments/{id}", attachmentId)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsExecutable() throws Exception {
        mockMvc
            .perform(multipart("/api/wiki/pages/{id}/attachments", pageId).file(new MockMultipartFile("file", "evil.exe", "application/x-msdownload", new byte[] { 1 })).with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void templatesEndpoint() throws Exception {
        mockMvc.perform(get("/api/wiki/templates").param("space", "w5att")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("builtin:blank"));
    }
}
