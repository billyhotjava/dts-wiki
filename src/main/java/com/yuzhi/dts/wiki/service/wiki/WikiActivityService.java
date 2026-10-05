package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.ActivityEvent;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.enumeration.ActivityType;
import com.yuzhi.dts.wiki.repository.ActivityEventRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WikiActivityService {
    public record Item(long id, String type, String actor, String title, Long pageId, String spaceSlug,
                       String url, String detail, Instant createdAt) {}
    private final ActivityEventRepository events;
    private final NamedParameterJdbcTemplate jdbc;
    private final SpaceAccessService access;
    private final SpaceRepository spaces;
    public WikiActivityService(ActivityEventRepository events, NamedParameterJdbcTemplate jdbc, SpaceAccessService access, SpaceRepository spaces) {
        this.events = events; this.jdbc = jdbc; this.access = access; this.spaces = spaces;
    }

    @Transactional
    public void record(Page page, ActivityType type, String actor, String detail, Instant timestamp) {
        events.save(new ActivityEvent().page(page).space(page.getSpace()).type(type).targetTitle(page.getTitle())
            .actorName(actor == null ? SecurityUtils.getCurrentUserLogin().orElse("unknown") : actor)
            .detail(detail).createdAt(timestamp == null ? Instant.now() : timestamp));
    }

    @Transactional(readOnly = true)
    public List<Item> list(String slug, String author, Instant since, int page, int size) {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 200) throw new IllegalArgumentException("Invalid pagination");
        Set<Long> ids;
        if (slug != null && !slug.isBlank()) {
            access.requireRead(slug);
            ids = Set.of(spaces.findOneBySlug(slug).orElseThrow(() -> new SpaceNotVisibleException(slug)).getId());
        } else ids = access.readableSpaceIds();
        if (ids.isEmpty()) return List.of();
        var params = new MapSqlParameterSource("spaces", ids).addValue("limit", size).addValue("offset", (long) page * size);
        var sql = new StringBuilder("""
            SELECT e.*, sp.slug FROM activity_event e JOIN space sp ON sp.id = e.space_id
            LEFT JOIN page p ON p.id = e.page_id
            WHERE e.space_id IN (:spaces) AND (e.page_id IS NULL OR p.deleted_at IS NULL)
            """);
        if (author != null && !author.isBlank()) {
            if (author.length() > 100) throw new IllegalArgumentException("Author is too long");
            sql.append(" AND e.actor_name = :author"); params.addValue("author", author);
        }
        if (since != null) { sql.append(" AND e.created_at >= :since"); params.addValue("since", java.sql.Timestamp.from(since)); }
        sql.append(" ORDER BY e.created_at DESC, e.id DESC LIMIT :limit OFFSET :offset");
        return jdbc.query(sql.toString(), params, (rs, n) -> {
            Long pageId = rs.getObject("page_id", Long.class);
            return new Item(rs.getLong("id"), rs.getString("type"), rs.getString("actor_name"), rs.getString("target_title"),
                pageId, rs.getString("slug"), pageId == null ? null : "/s/" + rs.getString("slug") + "/p/" + pageId,
                rs.getString("detail"), rs.getTimestamp("created_at").toInstant());
        });
    }
}
