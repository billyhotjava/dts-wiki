package com.yuzhi.dts.wiki.service.wiki.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.yuzhi.dts.wiki.service.wiki.*;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import com.yuzhi.dts.wiki.service.wiki.dto.SpaceDtos;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Small MCP adapters into existing business services; no separate privilege or data path. */
@Service
public class WikiMcpTools {
    public record Definition(String name, String description, Map<String, Object> inputSchema, Map<String, Boolean> annotations) {}
    private final PageService pages;
    private final WikiQueryService query;
    private final WikiSearchService search;
    private final DiagramArtifactService diagrams;
    private final PageMetaDao metadata;
    private final ObjectMapper json;
    private final Map<String, Definition> definitions = new LinkedHashMap<>();
    private final Map<String, Schema> validators = new LinkedHashMap<>();

    public WikiMcpTools(PageService pages, WikiQueryService query, WikiSearchService search, DiagramArtifactService diagrams, PageMetaDao metadata, ObjectMapper json) {
        this.pages = pages; this.query = query; this.search = search; this.diagrams = diagrams; this.metadata = metadata; this.json = json;
        var text = Map.<String, Object>of("type", "string", "maxLength", 300);
        var positive = Map.<String, Object>of("type", "integer", "minimum", 1, "maximum", Long.MAX_VALUE);
        var markdown = Map.<String, Object>of("type", "string", "maxLength", 10_000_000);
        add("wiki_search", "Search current authorized knowledge and attachment names.", Map.of("query", text, "space", text, "type", text, "limit", Map.of("type", "integer", "minimum", 1, "maximum", 200)), List.of("query"), true);
        add("wiki_get_page", "Read Markdown, its current version and metadata by page ID or space/path.", Map.of("pageId", positive, "space", text, "path", text), List.of(), true);
        add("wiki_list_tree", "List an authorized subtree with metadata; bounded to 2000 nodes.", Map.of("space", text, "path", text, "depth", Map.of("type", "integer", "minimum", 1, "maximum", 20)), List.of("space"), true);
        var filters = new LinkedHashMap<String, Object>();
        for (String key : List.of("space", "type", "status", "owner", "sprint", "feature", "priority", "tag", "q")) filters.put(key, text);
        filters.put("page", Map.of("type", "integer", "minimum", 0, "maximum", 1_000_000));
        filters.put("size", Map.of("type", "integer", "minimum", 1, "maximum", 200));
        add("wiki_query", "Query authorized structured metadata without changing content contracts.", filters, List.of("space"), true);
        add("wiki_update_page", "Update a native page with an exact base version; Git pages remain read-only.", Map.of("pageId", positive, "baseVersionNo", Map.of("type", "integer", "minimum", 0, "maximum", Integer.MAX_VALUE), "markdown", markdown, "message", Map.of("type", "string", "maxLength", 500)), List.of("pageId", "baseVersionNo", "markdown"), false);
        add("wiki_create_page", "Create a native Markdown page under an authorized parent.", Map.of("space", text, "parentId", positive, "parentPath", text, "fileName", text, "markdown", markdown, "message", Map.of("type", "string", "maxLength", 500)), List.of("space", "fileName", "markdown"), false);
        add("wiki_get_diagram_spec", "Read an authorized JSON diagram source attachment.", Map.of("pageId", positive, "path", text), List.of("pageId", "path"), true);
        add("wiki_put_diagram", "Store a caller-rendered JSON, HTML and PNG diagram bundle on a native page.", Map.of("pageId", positive, "name", text, "spec", Map.of("type", "string", "maxLength", 1_000_000), "html", Map.of("type", "string", "maxLength", 2_000_000), "pngBase64", Map.of("type", "string", "maxLength", 14_000_000)), List.of("pageId", "name", "spec", "html", "pngBase64"), false);
    }

    private void add(String name, String description, Map<String, ?> properties, List<String> required, boolean readOnly) {
        var schema = Map.<String, Object>of("type", "object", "properties", properties, "required", required, "additionalProperties", false);
        definitions.put(name, new Definition(name, description, schema, Map.of("readOnlyHint", readOnly, "destructiveHint", !readOnly, "openWorldHint", false)));
        try { validators.put(name, SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12).getSchema(json.writeValueAsString(schema))); }
        catch (Exception e) { throw new IllegalStateException("Invalid MCP tool schema", e); }
    }
    public List<Definition> definitions(boolean writable) { return definitions.values().stream().filter(d -> writable || d.annotations().get("readOnlyHint")).toList(); }
    public boolean exists(String name) { return definitions.containsKey(name); }
    public boolean writeTool(String name) { return exists(name) && !definitions.get(name).annotations().get("readOnlyHint"); }

    public Object call(String name, JsonNode args, String viaAgent) throws java.io.IOException {
        if (!exists(name)) throw new IllegalArgumentException("Unknown tool");
        if (!validators.get(name).validate(new tools.jackson.databind.json.JsonMapper().readTree(json.writeValueAsString(args))).isEmpty()) {
            throw new IllegalArgumentException("Arguments do not match the tool input schema");
        }
        return switch (name) {
            case "wiki_search" -> search.search(new WikiSearchService.Filters(text(args, "query"), text(args, "space"), text(args, "type"), null, null, null, null, null, 0, number(args, "limit", 20)));
            case "wiki_get_page" -> pages.getPage(pageId(args));
            case "wiki_query" -> query.query(new WikiQueryService.Filters(text(args, "space"), text(args, "type"), text(args, "status"), text(args, "owner"), text(args, "sprint"), text(args, "feature"), text(args, "priority"), text(args, "tag"), text(args, "q"), number(args, "page", 0), number(args, "size", 50)));
            case "wiki_list_tree" -> listTree(args);
            case "wiki_update_page" -> pages.saveContent(args.get("pageId").longValue(), new PageDtos.SaveContentRequest(args.get("baseVersionNo").intValue(), text(args, "markdown"), text(args, "message")), viaAgent);
            case "wiki_create_page" -> create(args, viaAgent);
            case "wiki_get_diagram_spec" -> diagrams.get(args.get("pageId").longValue(), text(args, "path"));
            case "wiki_put_diagram" -> Map.of("attachments", diagrams.put(args.get("pageId").longValue(), text(args, "name"), text(args, "spec"), text(args, "html"), text(args, "pngBase64")));
            default -> throw new IllegalArgumentException("Unknown tool");
        };
    }

    private long pageId(JsonNode args) {
        if (args.has("pageId")) {
            if (args.has("space") || args.has("path")) throw new IllegalArgumentException("Use pageId or space/path");
            return args.get("pageId").longValue();
        }
        if (text(args, "space") == null || text(args, "path") == null) throw new IllegalArgumentException("pageId or space/path is required");
        return pages.resolve(text(args, "space"), text(args, "path")).pageId();
    }
    private PageDtos.PageView create(JsonNode args, String viaAgent) {
        String space = text(args, "space"), fileName = text(args, "fileName");
        if (!fileName.matches("[^/\\\\\\p{Cntrl}]{1,190}\\.md")) throw new IllegalArgumentException("fileName must be a Markdown filename without directories");
        var detail = pages.getSpace(space);
        Long parent = args.has("parentId") ? args.get("parentId").longValue() : detail.rootPageId();
        if (args.has("parentPath")) {
            if (args.has("parentId")) throw new IllegalArgumentException("Use parentId or parentPath");
            parent = pages.resolve(space, text(args, "parentPath")).pageId();
        }
        return pages.createPage(space, new PageDtos.CreatePageRequest(parent, fileName.substring(0, fileName.length() - 3), "NATIVE", text(args, "markdown")), viaAgent, text(args, "message"));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listTree(JsonNode args) {
        String space = text(args, "space");
        List<SpaceDtos.TreeNode> tree = pages.tree(space);
        if (args.has("path")) {
            long id = pages.resolve(space, text(args, "path")).pageId();
            tree = List.of(find(tree, id));
        }
        int[] remaining = { 2000 };
        List<SpaceDtos.TreeNode> bounded = trim(tree, number(args, "depth", 3), remaining);
        List<Long> ids = new ArrayList<>(); collect(bounded, ids);
        var meta = metadata.findByPages(ids);
        return Map.of("items", decorate(bounded, meta), "truncated", remaining[0] == 0);
    }
    private static SpaceDtos.TreeNode find(List<SpaceDtos.TreeNode> tree, long id) {
        for (var node : tree) { if (node.id() == id) return node; var found = find(node.children(), id); if (found != null) return found; }
        return null;
    }
    private static List<SpaceDtos.TreeNode> trim(List<SpaceDtos.TreeNode> tree, int depth, int[] budget) {
        List<SpaceDtos.TreeNode> result = new ArrayList<>();
        for (var n : tree) { if (budget[0]-- <= 0) { budget[0] = 0; break; }
            result.add(new SpaceDtos.TreeNode(n.id(), n.title(), n.kind(), n.hasChildren(), n.syncStatus(), n.readOnly(), depth <= 1 ? List.of() : trim(n.children(), depth - 1, budget))); }
        return result;
    }
    private static void collect(List<SpaceDtos.TreeNode> tree, List<Long> ids) { for (var node : tree) { ids.add(node.id()); collect(node.children(), ids); } }
    private static List<Map<String, Object>> decorate(List<SpaceDtos.TreeNode> tree, Map<Long, PageMetaDao.MetaRow> meta) {
        return tree.stream().map(node -> {
            Map<String, Object> item = new LinkedHashMap<>(); item.put("pageId", node.id()); item.put("title", node.title()); item.put("kind", node.kind()); item.put("readOnly", node.readOnly());
            var row = meta.get(node.id()); if (row != null) { item.put("docId", row.docId()); item.put("type", row.docType()); item.put("status", row.status()); }
            item.put("children", decorate(node.children(), meta)); return item;
        }).toList();
    }
    private static String text(JsonNode args, String name) { return args.hasNonNull(name) ? args.get(name).asText() : null; }
    private static int number(JsonNode args, String name, int fallback) { return args.has(name) ? args.get(name).intValue() : fallback; }
}
