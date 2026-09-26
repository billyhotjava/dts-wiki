package com.yuzhi.dts.wiki.service.wiki;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retrieval document maintenance (design 10 S3.1/B6): one row per page holding the
 * LATEST version only. Title carries title + tags + doc_id; body is frontmatter-free
 * plain text (links keep text only).
 */
@Service
public class SearchIndexService {

    private final JdbcTemplate jdbc;

    public SearchIndexService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void upsert(long pageId, long spaceId, String title, String tagsAndDocId, String plainBody) {
        jdbc.update(
            """
            INSERT INTO page_search_doc (page_id, space_id, title, body, updated_at)
            VALUES (?, ?, ?, ?, now())
            ON CONFLICT (page_id) DO UPDATE SET space_id = EXCLUDED.space_id, title = EXCLUDED.title,
                body = EXCLUDED.body, updated_at = now()
            """,
            pageId,
            spaceId,
            title + " " + tagsAndDocId,
            plainBody
        );
    }

    @Transactional
    public void delete(long pageId) {
        jdbc.update("DELETE FROM page_search_doc WHERE page_id = ?", pageId);
    }
}
