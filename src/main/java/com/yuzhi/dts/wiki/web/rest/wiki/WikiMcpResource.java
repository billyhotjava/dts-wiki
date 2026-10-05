package com.yuzhi.dts.wiki.web.rest.wiki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.config.WikiMcpProperties;
import com.yuzhi.dts.wiki.security.WikiMcpTokenPolicy;
import com.yuzhi.dts.wiki.service.wiki.*;
import com.yuzhi.dts.wiki.service.wiki.mcp.McpWriteLimiter;
import com.yuzhi.dts.wiki.service.wiki.mcp.WikiMcpTools;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/** Stateless Streamable HTTP: JSON responses, notification acknowledgements, no SSE sessions. */
@RestController
@ConditionalOnProperty(name = "application.wiki.mcp.enabled", havingValue = "true")
public class WikiMcpResource {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WikiMcpResource.class);
    private static final List<String> VERSIONS = List.of("2025-11-25", "2025-06-18", "2025-03-26");
    private final WikiMcpTools tools;
    private final WikiMcpProperties settings;
    private final McpWriteLimiter limiter;
    private final ObjectMapper json;
    private final String issuer;
    public WikiMcpResource(WikiMcpTools tools, WikiMcpProperties settings, McpWriteLimiter limiter, ObjectMapper json,
        @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") String issuer) {
        this.tools = tools; this.settings = settings; this.limiter = limiter; this.json = json; this.issuer = issuer;
    }

    @RequestMapping(path = "/mcp", method = { RequestMethod.GET, RequestMethod.DELETE })
    public ResponseEntity<Void> noStream() { return ResponseEntity.status(405).header("Allow", "POST").build(); }

    @PostMapping(path = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> post(HttpServletRequest request, JwtAuthenticationToken authentication) throws java.io.IOException {
        String accept = request.getHeader("Accept");
        if (accept == null || !accept.contains("application/json") || !accept.contains("text/event-stream")) return ResponseEntity.status(406).build();
        String version = request.getHeader("MCP-Protocol-Version");
        if (version != null && !VERSIONS.contains(version)) return ResponseEntity.badRequest().body(error(null, -32600, "Unsupported MCP protocol version"));
        if (authentication == null) return ResponseEntity.status(401).build();
        try { WikiMcpTokenPolicy.validate(authentication.getToken(), settings, issuer); }
        catch (org.springframework.security.oauth2.jwt.JwtException e) {
            return ResponseEntity.status(401).header("WWW-Authenticate", "Bearer resource_metadata=\"" + settings.metadataUri() + "\"").build();
        }
        byte[] bytes = request.getInputStream().readNBytes(20_000_001);
        if (bytes.length > 20_000_000) return ResponseEntity.status(413).build();
        JsonNode message;
        try { message = json.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .with(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(bytes); }
        catch (Exception e) { return ResponseEntity.badRequest().body(error(null, -32700, "Invalid JSON")); }
        if (message == null || !message.isObject() || !"2.0".equals(message.path("jsonrpc").asText()) || !message.path("method").isTextual()
            || message.has("id") && !(message.get("id").isTextual() || message.get("id").isIntegralNumber())) {
            return ResponseEntity.badRequest().body(error(null, -32600, "Invalid JSON-RPC request"));
        }
        String method = message.get("method").asText();
        if (!message.has("id")) {
            return method.startsWith("notifications/") ? ResponseEntity.accepted().build()
                : ResponseEntity.badRequest().body(error(null, -32600, "Request id is required"));
        }
        Object id = json.convertValue(message.get("id"), Object.class);
        JsonNode params = message.has("params") ? message.get("params") : json.createObjectNode();
        if (!params.isObject()) return reply(error(id, -32602, "params must be an object"));
        Object result;
        switch (method) {
            case "initialize" -> {
                if (!params.path("protocolVersion").isTextual() || !params.path("capabilities").isObject()
                    || !params.path("clientInfo").isObject()) return reply(error(id, -32602, "Invalid initialize parameters"));
                String proposed = params.get("protocolVersion").asText();
                result = Map.of("protocolVersion", VERSIONS.contains(proposed) ? proposed : VERSIONS.get(0),
                    "capabilities", Map.of("tools", Map.of("listChanged", false)),
                    "serverInfo", Map.of("name", "dts-wiki", "version", "1.0.0"),
                    "instructions", "Use personal identity. Git content is read-only. Native writes require wiki.write and editor permission.");
            }
            case "ping" -> result = Map.of();
            case "tools/list" -> {
                if (params.has("cursor")) return reply(error(id, -32602, "Tool listing has no cursor"));
                result = Map.of("tools", tools.definitions(WikiMcpTokenPolicy.hasScope(authentication.getToken(), "wiki.write")));
            }
            case "tools/call" -> {
                if (!params.path("name").isTextual() || !tools.exists(params.get("name").asText())) return reply(error(id, -32602, "Unknown tool"));
                String name = params.get("name").asText();
                JsonNode args = params.has("arguments") ? params.get("arguments") : json.createObjectNode();
                if (!args.isObject()) return reply(error(id, -32602, "arguments must be an object"));
                if (tools.writeTool(name) && !WikiMcpTokenPolicy.hasScope(authentication.getToken(), "wiki.write")) {
                    return reply(success(id, toolError("INSUFFICIENT_SCOPE", 403, "wiki.write is required")));
                }
                if (tools.writeTool(name) && !limiter.allow(authentication.getToken().getSubject())) {
                    return ResponseEntity.status(429).header("Retry-After", "60").body(success(id, toolError("WRITE_RATE_LIMIT", 429, "Personal write budget exceeded")));
                }
                String agent = request.getHeader("X-Wiki-Agent");
                if (agent == null) agent = authentication.getToken().getClaimAsString("azp");
                if (agent.isBlank() || agent.length() > 100 || agent.chars().anyMatch(Character::isISOControl)) return reply(error(id, -32602, "Invalid agent label"));
                try {
                    Object output = tools.call(name, args, agent);
                    result = Map.of("content", List.of(Map.of("type", "text", "text", json.writeValueAsString(output))), "structuredContent", output, "isError", false);
                } catch (GitPageReadOnlyException e) { result = toolError("GIT_PAGE_READ_ONLY", 409, "Git page is read-only"); }
                catch (PageVersionConflictException e) {
                    result = Map.of("content", List.of(Map.of("type", "text", "text", json.writeValueAsString(Map.of("errorKey", "PAGE_VERSION_CONFLICT", "status", 409, "currentVersionNo", e.getCurrentVersionNo())))), "isError", true);
                } catch (PageSyncConflictException e) { result = toolError("PAGE_SYNC_CONFLICT", 409, "Page has an unresolved conflict"); }
                catch (SpaceNotVisibleException e) { result = toolError("SPACE_NOT_VISIBLE", 404, "Space or page not found"); }
                catch (AccessDeniedException e) { result = toolError("ACCESS_DENIED", 403, "No write permission"); }
                catch (com.yuzhi.dts.wiki.service.wiki.content.FrontmatterInvalidException e) { result = toolError("FRONTMATTER_INVALID", 422, "Frontmatter is invalid"); }
                catch (IllegalArgumentException e) { result = toolError("BAD_REQUEST", 400, e.getMessage()); }
                catch (Exception e) {
                    LOG.warn("MCP tool failed: tool={}, exception={}", name, e.getClass().getSimpleName());
                    return reply(error(id, -32603, "Internal tool error"));
                }
            }
            default -> { return reply(error(id, -32601, "Method not found")); }
        }
        return reply(success(id, result));
    }
    private Map<String, Object> toolError(String key, int status, String detail) throws java.io.IOException {
        return Map.of("content", List.of(Map.of("type", "text", "text", json.writeValueAsString(Map.of("errorKey", key, "status", status, "detail", detail)))), "isError", true);
    }
    private static Map<String, Object> success(Object id, Object result) { return Map.of("jsonrpc", "2.0", "id", id, "result", result); }
    private static Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>(); response.put("jsonrpc", "2.0"); response.put("id", id); response.put("error", Map.of("code", code, "message", message)); return response;
    }
    private static ResponseEntity<?> reply(Object body) { return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).header("Cache-Control", "no-store").body(body); }
}
