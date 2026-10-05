package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.repository.SpaceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** pg_bigm retrieval over current content and attachment names, with SQL-level space filtering. */
@Service
@Transactional(readOnly = true)
public class WikiSearchService {
    public record Filters(String q, String space, String type, String status, String owner, String tag,
                          Instant since, Instant until, int page, int size) {}
    public record Hit(long pageId, String spaceSlug, String spaceName, String title, String snippet, String type,
                      String status, String gitPath, String url, Instant updatedAt, double score) {}
    public record Result(List<Hit> items, long total, int page, int size) {}
    private final NamedParameterJdbcTemplate jdbc;
    private final SpaceAccessService access;
    private final SpaceRepository spaces;

    public WikiSearchService(NamedParameterJdbcTemplate jdbc, SpaceAccessService access, SpaceRepository spaces) {
        this.jdbc = jdbc;
        this.access = access;
        this.spaces = spaces;
    }

    public Result search(Filters filter) {
        if (filter.q() == null || filter.q().isBlank() || filter.q().length() > 200) throw new IllegalArgumentException("q must contain between 1 and 200 characters");
        if (filter.page() < 0 || filter.page() > 1_000_000 || filter.size() < 1 || filter.size() > 200) throw new IllegalArgumentException("Invalid pagination");
        if (filter.since() != null && filter.until() != null && filter.since().isAfter(filter.until())) throw new IllegalArgumentException("Invalid date range");
        Set<Long> visible;
        if (filter.space() != null && !filter.space().isBlank()) {
            access.requireRead(filter.space());
            visible = Set.of(spaces.findOneBySlug(filter.space()).orElseThrow(() -> new SpaceNotVisibleException(filter.space())).getId());
        } else visible = access.readableSpaceIds();
        if (visible.isEmpty()) return new Result(List.of(), 0, filter.page(), filter.size());
        var params = new MapSqlParameterSource("spaces", visible).addValue("term", filter.q().strip())
            .addValue("pattern", "%" + WikiQueryService.escapeLike(filter.q().strip()) + "%");
        String candidates = """
            WITH hits AS (
                SELECT page_id FROM page_search_doc
                WHERE space_id IN (:spaces) AND (title ILIKE :pattern ESCAPE '!' OR body ILIKE :pattern ESCAPE '!')
                UNION
                SELECT a.page_id FROM attachment a JOIN page ap ON ap.id = a.page_id
                WHERE ap.space_id IN (:spaces) AND ap.deleted_at IS NULL AND a.deleted_at IS NULL AND a.file_name ILIKE :pattern ESCAPE '!'
            )
            """;
        var from = new StringBuilder("""
            FROM hits h JOIN page p ON p.id = h.page_id JOIN space sp ON sp.id = p.space_id
            LEFT JOIN page_search_doc d ON d.page_id = p.id LEFT JOIN page_meta m ON m.page_id = p.id
            WHERE p.space_id IN (:spaces) AND p.deleted_at IS NULL
            """);
        equality(from, params, "doc_type", filter.type());
        equality(from, params, "owner", filter.owner());
        if (filter.status() != null && !filter.status().isBlank()) {
            var values = java.util.Arrays.stream(filter.status().split(",")).map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
            if (values.isEmpty() || values.size() > 20 || filter.status().length() > 300) throw new IllegalArgumentException("Invalid statuses");
            from.append(" AND m.status IN (:statuses)"); params.addValue("statuses", values);
        }
        if (filter.tag() != null && !filter.tag().isBlank()) {
            if (filter.tag().length() > 300) throw new IllegalArgumentException("Tag is too long");
            from.append(" AND m.tags @> ARRAY[CAST(:tag AS text)]"); params.addValue("tag", filter.tag());
        }
        if (filter.since() != null) { from.append(" AND p.updated_at >= :since"); params.addValue("since", java.sql.Timestamp.from(filter.since())); }
        if (filter.until() != null) { from.append(" AND p.updated_at <= :until"); params.addValue("until", java.sql.Timestamp.from(filter.until())); }
        long total = jdbc.queryForObject(candidates + "SELECT count(*) " + from, params, Long.class);
        params.addValue("limit", filter.size()).addValue("offset", (long) filter.page() * filter.size());
        String select = """
            SELECT p.id, p.title, p.git_path, p.updated_at, sp.slug, sp.name,
                   m.doc_type, m.status, coalesce(d.body, '') AS body,
                   3 * bigm_similarity(coalesce(d.title, p.title), :term) +
                   bigm_similarity(coalesce(d.body, ''), :term) AS score
            """;
        List<Hit> items = jdbc.query(candidates + select + from + " ORDER BY score DESC, p.updated_at DESC, p.id DESC LIMIT :limit OFFSET :offset",
            params, (rs, n) -> new Hit(rs.getLong("id"), rs.getString("slug"), rs.getString("name"), rs.getString("title"),
                snippet(rs.getString("body"), filter.q()), rs.getString("doc_type"), rs.getString("status"), rs.getString("git_path"),
                "/s/" + rs.getString("slug") + "/p/" + rs.getLong("id"), rs.getTimestamp("updated_at").toInstant(), rs.getDouble("score")));
        return new Result(items, total, filter.page(), filter.size());
    }

    private static void equality(StringBuilder sql, MapSqlParameterSource params, String column, String value) {
        if (value == null || value.isBlank()) return;
        if (value.length() > 300) throw new IllegalArgumentException("Filter is too long");
        sql.append(" AND m.").append(column).append(" = :").append(column); params.addValue(column, value.strip());
    }

    private static String snippet(String body, String query) {
        String text = body.replaceAll("\\s+", " ").strip();
        int hit = text.toLowerCase(java.util.Locale.ROOT).indexOf(query.strip().toLowerCase(java.util.Locale.ROOT));
        int start = Math.max(0, hit - 50);
        int end = Math.min(text.length(), start + 180);
        return (start == 0 ? "" : "…") + text.substring(start, end) + (end == text.length() ? "" : "…");
    }
}
