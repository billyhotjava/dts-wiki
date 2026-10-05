package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.repository.SpaceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Structured content queries scoped in SQL to a currently authorized space. */
@Service
@Transactional(readOnly = true)
public class WikiQueryService {
    public record Filters(String space, String type, String status, String owner, String sprint, String feature,
                          String priority, String tag, String q, int page, int size) {}
    public record Item(long pageId, String docId, String type, String status, String title, String owner,
                       String priority, String sprint, String feature, String url, String gitPath, boolean valid, Instant updatedAt) {}
    public record Result(List<Item> items, long total, int page, int size) {}

    private final NamedParameterJdbcTemplate jdbc;
    private final SpaceAccessService access;
    private final SpaceRepository spaces;

    public WikiQueryService(NamedParameterJdbcTemplate jdbc, SpaceAccessService access, SpaceRepository spaces) {
        this.jdbc = jdbc;
        this.access = access;
        this.spaces = spaces;
    }

    public Result query(Filters filter) {
        if (filter.space() == null || filter.space().isBlank()) throw new IllegalArgumentException("space is required");
        if (filter.page() < 0 || filter.page() > 1_000_000 || filter.size() < 1 || filter.size() > 200) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 200");
        }
        access.requireRead(filter.space());
        long spaceId = spaces.findOneBySlug(filter.space()).orElseThrow(() -> new SpaceNotVisibleException(filter.space())).getId();
        var params = new MapSqlParameterSource("space", spaceId);
        var where = new StringBuilder(" FROM page_meta m JOIN page p ON p.id = m.page_id WHERE m.space_id = :space AND p.deleted_at IS NULL");
        Map<String, String> values = new java.util.LinkedHashMap<>();
        values.put("doc_type", filter.type());
        values.put("owner", filter.owner());
        values.put("sprint", filter.sprint());
        values.put("feature", filter.feature());
        values.put("priority", filter.priority());
        values.forEach((column, value) -> {
            if (value != null && !value.isBlank()) {
                bounded(value);
                where.append(" AND m.").append(column).append(" = :").append(column);
                params.addValue(column, value.strip());
            }
        });
        if (filter.status() != null && !filter.status().isBlank()) {
            bounded(filter.status());
            List<String> statuses = java.util.Arrays.stream(filter.status().split(",")).map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
            if (statuses.isEmpty() || statuses.size() > 20) throw new IllegalArgumentException("Invalid status filter");
            where.append(" AND m.status IN (:statuses)");
            params.addValue("statuses", statuses);
        }
        if (filter.tag() != null && !filter.tag().isBlank()) {
            bounded(filter.tag());
            where.append(" AND m.tags @> ARRAY[CAST(:tag AS text)]");
            params.addValue("tag", filter.tag().strip());
        }
        if (filter.q() != null && !filter.q().isBlank()) {
            bounded(filter.q());
            where.append(" AND m.title ILIKE :q ESCAPE '!' ");
            params.addValue("q", "%" + escapeLike(filter.q().strip()) + "%");
        }
        long total = jdbc.queryForObject("SELECT count(*)" + where, params, Long.class);
        params.addValue("limit", filter.size()).addValue("offset", (long) filter.page() * filter.size());
        List<Item> rows = jdbc.query("SELECT m.*, p.git_path" + where + " ORDER BY m.updated_at DESC, m.page_id DESC LIMIT :limit OFFSET :offset",
            params, (rs, n) -> new Item(rs.getLong("page_id"), rs.getString("doc_id"), rs.getString("doc_type"), rs.getString("status"),
                rs.getString("title"), rs.getString("owner"), rs.getString("priority"), rs.getString("sprint"), rs.getString("feature"),
                "/s/" + filter.space() + "/p/" + rs.getLong("page_id"), rs.getString("git_path"), rs.getBoolean("valid"), rs.getTimestamp("updated_at").toInstant()));
        return new Result(rows, total, filter.page(), filter.size());
    }

    /** A bounded index in page-tree order. Clients revalidate before using cached data. */
    public String llms(String slug) {
        access.requireRead(slug);
        var space = spaces.findOneBySlug(slug).orElseThrow(() -> new SpaceNotVisibleException(slug));
        var lines = jdbc.query("""
            WITH RECURSIVE tree AS (
                SELECT p.id, ARRAY[p.position::bigint, p.id] AS ordering, 0 AS depth
                FROM page p WHERE p.space_id = :space AND p.parent_id IS NULL AND p.deleted_at IS NULL
                UNION ALL
                SELECT p.id, t.ordering || ARRAY[p.position::bigint, p.id], t.depth + 1
                FROM page p JOIN tree t ON p.parent_id = t.id
                WHERE p.space_id = :space AND p.deleted_at IS NULL AND t.depth < 256
            )
            SELECT p.id, p.title, m.doc_type, m.status,
                   coalesce(m.meta ->> 'goal', left(s.body, 512), '') AS summary
            FROM tree t JOIN page p ON p.id = t.id
            LEFT JOIN page_meta m ON m.page_id = p.id
            LEFT JOIN page_search_doc s ON s.page_id = p.id
            WHERE p.current_version_id IS NOT NULL ORDER BY t.ordering LIMIT 1996
            """, Map.of("space", space.getId()), (rs, n) -> {
                String summary = singleLine(rs.getString("summary"));
                if (summary.length() > 80) summary = summary.substring(0, 80);
                String type = rs.getString("doc_type");
                String status = rs.getString("status");
                return "- [" + markdownText(rs.getString("title")) + "](/api/wiki/pages/" + rs.getLong("id") + "/markdown): "
                    + summary + (type == null ? "" : " [" + singleLine(type) + (status == null ? "" : " · " + singleLine(status)) + "]");
            });
        var result = new ArrayList<String>();
        result.add("# " + singleLine(space.getName()));
        result.add("> " + singleLine(space.getDescription()));
        result.add("");
        result.add("## Pages");
        result.addAll(lines);
        return String.join("\n", result) + "\n";
    }

    private static String singleLine(String value) { return value == null ? "" : value.replaceAll("[\\p{Cntrl}\\s]+", " ").strip(); }
    private static String markdownText(String value) { return singleLine(value).replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]"); }
    private static void bounded(String value) { if (value.length() > 300) throw new IllegalArgumentException("Filter is too long"); }
    public static String escapeLike(String value) { return value.replace("!", "!!").replace("%", "!%").replace("_", "!_"); }
}
