package com.yuzhi.dts.wiki.service.wiki;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.service.wiki.content.ContentAnalysis;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * {@code page_meta} access via JdbcTemplate (a derived SQL projection, no entity CRUD API; design 10 S3.2).
 */
@Repository
public class PageMetaDao {

    public record MetaRow(
        long pageId,
        long spaceId,
        String docType,
        String docId,
        String status,
        String title,
        String owner,
        String sprint,
        String feature,
        String priority,
        List<String> tags,
        List<String> depends,
        List<String> related,
        Map<String, Object> meta,
        boolean valid,
        List<Map<String, String>> errors,
        Instant updatedAt
    ) {}

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public PageMetaDao(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public void upsert(long spaceId, long pageId, ContentAnalysis analysis) {
        jdbc.update(
            """
            INSERT INTO page_meta (page_id, space_id, doc_type, doc_id, status, title, owner, sprint, feature,
                                   priority, tags, depends, related, meta, valid, errors, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?::jsonb, ?)
            ON CONFLICT (page_id) DO UPDATE SET space_id = EXCLUDED.space_id, doc_type = EXCLUDED.doc_type,
                doc_id = EXCLUDED.doc_id, status = EXCLUDED.status, title = EXCLUDED.title, owner = EXCLUDED.owner,
                sprint = EXCLUDED.sprint, feature = EXCLUDED.feature, priority = EXCLUDED.priority,
                tags = EXCLUDED.tags, depends = EXCLUDED.depends, related = EXCLUDED.related,
                meta = EXCLUDED.meta, valid = EXCLUDED.valid, errors = EXCLUDED.errors, updated_at = EXCLUDED.updated_at
            """,
            pageId,
            spaceId,
            analysis.docType(),
            analysis.docId(),
            analysis.status(),
            analysis.title() == null ? "" : analysis.title(),
            analysis.owner(),
            analysis.sprint(),
            analysis.feature(),
            analysis.priority(),
            toArray(analysis.tags()),
            toArray(analysis.depends()),
            toArray(analysis.related()),
            toJson(analysis.frontmatter()),
            analysis.valid(),
            toJson(analysis.errors().stream().map(e -> Map.of("path", e.path(), "message", e.message())).toList()),
            java.sql.Timestamp.from(Instant.now())
        );
    }

    public void deleteByPage(long pageId) {
        jdbc.update("DELETE FROM page_meta WHERE page_id = ?", pageId);
    }

    public Optional<MetaRow> findByPage(long pageId) {
        List<MetaRow> rows = jdbc.query("SELECT * FROM page_meta WHERE page_id = ?", mapper(), pageId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<String> docIdInSpace(long spaceId, String docId, long excludePageId) {
        List<String> paths = jdbc.query(
            """
            SELECT p.git_path FROM page_meta m JOIN page p ON p.id = m.page_id
            WHERE m.space_id = ? AND m.doc_id = ? AND m.page_id <> ?
            """,
            (rs, n) -> rs.getString(1),
            spaceId,
            docId,
            excludePageId
        );
        return paths.isEmpty() ? Optional.empty() : Optional.of(paths.get(0) == null ? "(no git path)" : paths.get(0));
    }

    public List<Long> allMetaPageIds() {
        return jdbc.query("SELECT page_id FROM page_meta", (rs, n) -> rs.getLong(1));
    }

    private RowMapper<MetaRow> mapper() {
        return (ResultSet rs, int n) -> {
            try {
                return new MetaRow(
                    rs.getLong("page_id"),
                    rs.getLong("space_id"),
                    rs.getString("doc_type"),
                    rs.getString("doc_id"),
                    rs.getString("status"),
                    rs.getString("title"),
                    rs.getString("owner"),
                    rs.getString("sprint"),
                    rs.getString("feature"),
                    rs.getString("priority"),
                    toList(rs.getArray("tags")),
                    toList(rs.getArray("depends")),
                    toList(rs.getArray("related")),
                    toMap(rs.getString("meta")),
                    rs.getBoolean("valid"),
                    toListOfMaps(rs.getString("errors")),
                    rs.getTimestamp("updated_at").toInstant()
                );
            } catch (Exception e) {
                throw new SQLException(e);
            }
        };
    }

    private String[] toArray(List<String> values) {
        return values == null ? new String[0] : values.toArray(new String[0]);
    }

    private List<String> toList(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        return Arrays.asList((String[]) array.getArray());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private List<Map<String, String>> toListOfMaps(String json) {
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, String>>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
