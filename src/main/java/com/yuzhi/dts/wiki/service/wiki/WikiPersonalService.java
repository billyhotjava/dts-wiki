package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.UserRepository;
import com.yuzhi.dts.wiki.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Personal navigation data belongs to Wiki and never modifies Git content. */
@Service
public class WikiPersonalService {
    public record SavedPage(long pageId, String title, String spaceSlug, String url, Instant savedAt) {}
    public record SavedPages(List<SavedPage> items, long total) {}
    public record State(boolean favorite, boolean watching) {}
    private final NamedParameterJdbcTemplate sql;
    private final PageRepository pages;
    private final UserRepository users;
    private final SpaceAccessService access;
    public WikiPersonalService(NamedParameterJdbcTemplate sql, PageRepository pages, UserRepository users, SpaceAccessService access) {
        this.sql = sql; this.pages = pages; this.users = users; this.access = access;
    }
    @Transactional(readOnly = true)
    public State state(long pageId) {
        visible(pageId); var params = Map.of("user", current().getId(), "page", pageId);
        boolean favorite = sql.queryForObject("SELECT count(*) FROM page_favorite WHERE user_id=:user AND page_id=:page", params, Long.class) > 0;
        boolean watching = sql.queryForObject("SELECT count(*) FROM page_watch WHERE user_id=:user AND page_id=:page AND NOT muted", params, Long.class) > 0;
        return new State(favorite, watching);
    }
    @Transactional
    public void favorite(long pageId, boolean enabled) {
        visible(pageId); var params = Map.of("user", current().getId(), "page", pageId);
        if (enabled) sql.update("INSERT INTO page_favorite(user_id,page_id,created_at) VALUES(:user,:page,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING", params);
        else sql.update("DELETE FROM page_favorite WHERE user_id=:user AND page_id=:page", params);
    }
    @Transactional
    public void view(long pageId) {
        visible(pageId); String user = current().getId(); var params = Map.of("user", user, "page", pageId);
        // Serialize upsert and pruning per user; concurrent visits cannot leave 51 rows.
        sql.queryForObject("SELECT id FROM jhi_user WHERE id=:user FOR UPDATE", params, String.class);
        sql.update("INSERT INTO page_view(user_id,page_id,viewed_at) VALUES(:user,:page,clock_timestamp()) ON CONFLICT(user_id,page_id) DO UPDATE SET viewed_at=EXCLUDED.viewed_at", params);
        sql.update("DELETE FROM page_view WHERE user_id=:user AND page_id IN (SELECT page_id FROM page_view WHERE user_id=:user ORDER BY viewed_at DESC,page_id DESC OFFSET 50)", params);
    }
    @Transactional(readOnly = true)
    public SavedPages list(boolean favorites, int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 50) throw new IllegalArgumentException("Invalid personal list pagination");
        var spaces = access.readableSpaceIds(); if (spaces.isEmpty()) return new SavedPages(List.of(), 0);
        String table = favorites ? "page_favorite" : "page_view", time = favorites ? "created_at" : "viewed_at";
        String from = " FROM " + table + " x JOIN page p ON p.id=x.page_id JOIN space s ON s.id=p.space_id WHERE x.user_id=:user AND p.deleted_at IS NULL AND s.id IN (:spaces)";
        var params = Map.of("user", current().getId(), "spaces", spaces, "offset", page * size, "size", size);
        long total = sql.queryForObject("SELECT count(*)" + from, params, Long.class);
        var rows = sql.query("SELECT p.id,p.title,s.slug,x." + time + " AS saved_at" + from + " ORDER BY x." + time + " DESC,p.id DESC LIMIT :size OFFSET :offset", params,
            (rs, row) -> new SavedPage(rs.getLong("id"), rs.getString("title"), rs.getString("slug"), "/s/" + rs.getString("slug") + "/p/" + rs.getLong("id"), rs.getTimestamp("saved_at").toInstant()));
        return new SavedPages(rows, total);
    }
    public User current() {
        return SecurityUtils.getCurrentUserLogin().flatMap(users::findOneByLogin).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Personal identity projection is required"));
    }
    public Page visible(long id) {
        Page page = pages.findLive(id).orElseThrow(() -> new SpaceNotVisibleException("page:" + id)); access.requireRead(page); return page;
    }
}
