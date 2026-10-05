package com.yuzhi.dts.wiki.web.rest.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {"application.wiki.mcp.enabled=true", "application.wiki.mcp.resource-uri=https://wiki.example/mcp",
    "spring.security.oauth2.client.provider.oidc.issuer-uri=https://sso.example/realms/company"})
@WithMockUser(authorities = "ROLE_ADMIN")
class WikiMcpIT {
    private static final java.nio.file.Path BLOBS = temporaryBlobs();
    private static java.nio.file.Path temporaryBlobs() {
        try { return java.nio.file.Files.createTempDirectory("wiki-mcp-blobs-"); }
        catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
    @DynamicPropertySource static void configureBlobs(DynamicPropertyRegistry registry) {
        registry.add("application.wiki.attachments-dir", () -> BLOBS.toString());
    }
    @org.junit.jupiter.api.AfterAll static void cleanupBlobs() throws java.io.IOException {
        org.springframework.util.FileSystemUtils.deleteRecursively(BLOBS);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PageService pages;
    @Autowired PageRepository pageRepository;
    @Autowired SpaceRepository spaces;
    @Autowired JwtDecoder decoder;
    long root;
    long page;
    String accept = "application/json, text/event-stream";

    @BeforeEach void seed() {
        spaces.saveAndFlush(new Space().slug("mcp-team").name("MCP team").archived(false));
        root = pages.createPage("mcp-team", new PageDtos.CreatePageRequest(null, "Root", "FOLDER", null)).id();
        page = pages.createPage("mcp-team", new PageDtos.CreatePageRequest(root, "Knowledge", "NATIVE", "# Knowledge\n")).id();
        when(decoder.decode("personal")).thenReturn(token("https://wiki.example/mcp", "dts-wiki-agent", "alice", "wiki.read wiki.write", List.of("editor", "space-mcp-team")));
        when(decoder.decode("reader")).thenReturn(token("https://wiki.example/mcp", "dts-wiki-agent", "bob", "wiki.read", List.of("space-mcp-team")));
        when(decoder.decode("outsider")).thenReturn(token("https://wiki.example/mcp", "dts-wiki-agent", "outsider", "wiki.read", List.of("reader")));
    }
    private Jwt token(String audience, String client, String username, String scope, List<String> roles) {
        return Jwt.withTokenValue("fixture").header("alg", "RS256").issuer("https://sso.example/realms/company").subject(username)
            .audience(List.of(audience)).claim("azp", client).claim("preferred_username", username).claim("scope", scope).claim("roles", roles)
            .issuedAt(Instant.now().minusSeconds(10)).expiresAt(Instant.now().plusSeconds(300)).build();
    }
    private ResultActions rpc(String token, String method, Map<String, Object> params) throws Exception {
        return mvc.perform(post("/mcp").header("Authorization", "Bearer " + token).header("Accept", accept)
            .header("MCP-Protocol-Version", "2025-11-25").contentType("application/json")
            .content(json.writeValueAsString(Map.of("jsonrpc", "2.0", "id", 1, "method", method, "params", params))));
    }
    private ResultActions tool(String token, String name, Map<String, Object> args) throws Exception {
        return rpc(token, "tools/call", Map.of("name", name, "arguments", args));
    }

    @Test void transportNegotiationAndDiscovery() throws Exception {
        mvc.perform(get("/.well-known/oauth-protected-resource/mcp")).andExpect(status().isOk())
            .andExpect(jsonPath("$.resource").value("https://wiki.example/mcp"));
        mvc.perform(post("/mcp").with(anonymous()).contentType("application/json").header("Accept", accept).content("{}"))
            .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("resource_metadata=")));
        rpc("personal", "initialize", Map.of("protocolVersion", "2025-11-25", "capabilities", Map.of(), "clientInfo", Map.of("name", "fixture", "version", "1")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result.protocolVersion").value("2025-11-25"));
        mvc.perform(post("/mcp").header("Authorization", "Bearer personal").header("Accept", accept).contentType("application/json")
            .content("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"))
            .andExpect(status().isAccepted()).andExpect(content().string(""));
        mvc.perform(get("/mcp").header("Authorization", "Bearer personal")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/mcp").header("Authorization", "Bearer personal").header("Origin", "https://evil.example").header("Accept", accept).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/mcp").header("Authorization", "Bearer personal").header("Accept", accept).header("MCP-Protocol-Version", "bad").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        rpc("personal", "unsupported", Map.of()).andExpect(jsonPath("$.error.code").value(-32601));
    }

    @Test void personalTokenAudienceClientAndScopeAreRequired() throws Exception {
        for (String kind : List.of("wrong-audience", "wrong-client", "machine")) {
            when(decoder.decode(kind)).thenReturn(token(kind.equals("wrong-audience") ? "other" : "https://wiki.example/mcp",
                kind.equals("wrong-client") ? "other" : "dts-wiki-agent", kind.equals("machine") ? "service-account-agent" : "alice", "wiki.read", List.of("space-mcp-team")));
            rpc(kind, "ping", Map.of()).andExpect(status().isUnauthorized());
        }
        when(decoder.decode("no-scope")).thenReturn(token("https://wiki.example/mcp", "dts-wiki-agent", "alice", "openid", List.of("space-mcp-team")));
        rpc("no-scope", "ping", Map.of()).andExpect(status().isForbidden());
        mvc.perform(post("/mcp").with(user("session-user").roles("ADMIN")).header("Accept", accept).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test void toolsUseCallerSpacePermissionsAndPersonalVersionAuthor() throws Exception {
        rpc("reader", "tools/list", Map.of()).andExpect(jsonPath("$.result.tools.length()").value(5));
        tool("reader", "wiki_get_page", Map.of("pageId", page)).andExpect(jsonPath("$.result.structuredContent.contentMd").value("# Knowledge\n"));
        tool("outsider", "wiki_get_page", Map.of("pageId", page)).andExpect(jsonPath("$.result.isError").value(true))
            .andExpect(jsonPath("$.result.content[0].text").value(org.hamcrest.Matchers.containsString("SPACE_NOT_VISIBLE")));
        tool("reader", "wiki_update_page", Map.of("pageId", page, "baseVersionNo", 1, "markdown", "# Changed\n"))
            .andExpect(jsonPath("$.result.isError").value(true));
        tool("personal", "wiki_update_page", Map.of("pageId", page, "baseVersionNo", 1, "markdown", "# Changed\n"))
            .andExpect(jsonPath("$.result.structuredContent.versionNo").value(2));
        var current = pageRepository.findById(page).orElseThrow().getCurrentVersion();
        assertThat(current.getAuthorName()).isEqualTo("alice"); assertThat(current.getViaAgent()).isEqualTo("dts-wiki-agent");
        tool("personal", "wiki_update_page", Map.of("pageId", page, "baseVersionNo", 1, "markdown", "# Stale\n"))
            .andExpect(jsonPath("$.result.isError").value(true)).andExpect(jsonPath("$.result.content[0].text").value(org.hamcrest.Matchers.containsString("currentVersionNo")));
        var gitPage = pageRepository.findById(page).orElseThrow(); gitPage.setKind(PageKind.GIT); pageRepository.saveAndFlush(gitPage);
        tool("personal", "wiki_update_page", Map.of("pageId", page, "baseVersionNo", 2, "markdown", "# Git write\n"))
            .andExpect(jsonPath("$.result.content[0].text").value(org.hamcrest.Matchers.containsString("GIT_PAGE_READ_ONLY")));
    }

    @Test void createTreeQuerySearchAndMalformedInputs() throws Exception {
        tool("personal", "wiki_create_page", Map.of("space", "mcp-team", "parentId", root, "fileName", "review.md", "markdown", "# Agent review\n"))
            .andExpect(jsonPath("$.result.structuredContent.title").value("review"));
        tool("personal", "wiki_list_tree", Map.of("space", "mcp-team", "depth", 2)).andExpect(jsonPath("$.result.structuredContent.items[0].children.length()").value(2));
        tool("personal", "wiki_query", Map.of("space", "mcp-team")).andExpect(jsonPath("$.result.structuredContent.total").value(2));
        tool("personal", "wiki_search", Map.of("query", "Agent review", "space", "mcp-team")).andExpect(jsonPath("$.result.structuredContent.total").value(1));
        tool("personal", "wiki_get_page", Map.of("pageId", page, "unexpected", true)).andExpect(jsonPath("$.result.isError").value(true));
        tool("personal", "wiki_create_page", Map.of("space", "mcp-team", "fileName", "../bad.md", "markdown", "bad"))
            .andExpect(jsonPath("$.result.isError").value(true));
        tool("personal", "unknown", Map.of()).andExpect(jsonPath("$.error.code").value(-32602));
    }

    @Test void diagramArtifactsAreScopedAndServedWithSandboxPolicy() throws Exception {
        String png = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jXnQAAAAASUVORK5CYII=";
        tool("personal", "wiki_put_diagram", Map.of("pageId", page, "name", "flow", "spec", "{\"title\":\"Flow\"}",
            "html", "<html><script>window.fixture=true</script></html>", "pngBase64", png))
            .andExpect(jsonPath("$.result.structuredContent.attachments.length()").value(3));
        mvc.perform(get("/api/wiki/pages/{id}/attachments", page).with(user("alice").roles("ADMIN")))
            .andExpect(jsonPath("$[*].fileName", org.hamcrest.Matchers.containsInAnyOrder("flow.archify.json", "flow.html", "flow.png")));
        tool("personal", "wiki_put_diagram", Map.of("pageId", page, "name", "flow", "spec", "{\"title\":\"Revised\"}",
            "html", "<html><script>window.fixture=true</script></html>", "pngBase64", png))
            .andExpect(jsonPath("$.result.structuredContent.attachments.length()").value(3));
        mvc.perform(get("/api/wiki/pages/{id}/attachments", page).with(user("alice").roles("ADMIN")))
            .andExpect(jsonPath("$.length()").value(3));
        tool("personal", "wiki_get_diagram_spec", Map.of("pageId", page, "path", "diagrams/flow.archify.json"))
            .andExpect(jsonPath("$.result.structuredContent.source.title").value("Revised"));
        tool("outsider", "wiki_get_diagram_spec", Map.of("pageId", page, "path", "diagrams/flow.archify.json"))
            .andExpect(jsonPath("$.result.isError").value(true));
        mvc.perform(get("/api/wiki/pages/{id}/raw/diagrams/flow.html", page).with(user("alice").authorities(() -> "ROLE_SPACE_MCP_TEAM")))
            .andExpect(status().isOk()).andExpect(header().string("Content-Security-Policy", org.hamcrest.Matchers.containsString("sandbox allow-scripts")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        mvc.perform(get("/api/wiki/pages/{id}/raw/diagrams/flow.png", page).with(user("alice").authorities(() -> "ROLE_SPACE_MCP_TEAM")))
            .andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        tool("personal", "wiki_put_diagram", Map.of("pageId", page, "name", "bad", "spec", "{}", "html", "<html/>", "pngBase64", "YQ=="))
            .andExpect(jsonPath("$.result.isError").value(true));
        mvc.perform(multipart("/api/wiki/pages/{id}/attachments", page).file(new org.springframework.mock.web.MockMultipartFile("file", "unsafe.svg", "image/png", new byte[] {1})).with(user("alice").roles("ADMIN")).with(csrf()))
            .andExpect(status().isBadRequest());
        long attachmentId = json.readTree(mvc.perform(get("/api/wiki/pages/{id}/attachments", page).with(user("alice").roles("ADMIN")))
            .andReturn().getResponse().getContentAsString()).get(0).get("id").asLong();
        var deleted = pageRepository.findById(page).orElseThrow(); deleted.setDeletedAt(Instant.now()); pageRepository.saveAndFlush(deleted);
        mvc.perform(get("/api/wiki/attachments/{id}", attachmentId).with(user("alice").authorities(() -> "ROLE_SPACE_MCP_TEAM")))
            .andExpect(status().isNotFound());
    }
}
