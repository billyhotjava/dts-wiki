package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.repository.UserRepository;
import com.yuzhi.dts.wiki.security.SecurityUtils;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Writes idempotent intent only. Recipient authorization and SMTP run after commit. */
@Service
public class WikiNotificationIntents {
    private static final Pattern MENTION = Pattern.compile("(?<![\\p{L}\\p{N}._%+@-])@([A-Za-z0-9][A-Za-z0-9._-]{0,49})(?![A-Za-z0-9._-])");
    private final NamedParameterJdbcTemplate sql;
    private final UserRepository users;
    public WikiNotificationIntents(NamedParameterJdbcTemplate sql, UserRepository users) { this.sql = sql; this.users = users; }

    @Transactional
    public void watch(Page page, User user, boolean enabled) {
        sql.update("INSERT INTO page_watch(id,page_id,user_id,created_at,muted) VALUES(nextval('sequence_generator'),:page,:user,CURRENT_TIMESTAMP,:muted) ON CONFLICT(page_id,user_id) DO UPDATE SET muted=EXCLUDED.muted",
            Map.of("page", page.getId(), "user", user.getId(), "muted", !enabled));
    }
    private void autoWatch(Page page, User user) {
        sql.update("INSERT INTO page_watch(id,page_id,user_id,created_at,muted) VALUES(nextval('sequence_generator'),:page,:user,CURRENT_TIMESTAMP,false) ON CONFLICT(page_id,user_id) DO NOTHING", Map.of("page", page.getId(), "user", user.getId()));
    }
    @Transactional
    public void version(Page page, PageVersion version, String actorLogin) {
        User actor = actorLogin == null ? null : users.findOneByLogin(actorLogin).orElse(null);
        if (actor != null) autoWatch(page, actor);
        String event = "version:" + version.getId();
        watchers(page, event, actor == null ? null : actor.getId());
        mentions(page, event, version.getContentMd(), actor == null ? null : actor.getId());
    }
    @Transactional
    public void comment(Page page, long commentId, String body, User actor, User replyTo) {
        autoWatch(page, actor); String event = "comment:" + commentId;
        watchers(page, event, actor.getId()); mentions(page, event, body, actor.getId());
        if (replyTo != null && !replyTo.getId().equals(actor.getId())) record(page, event, replyTo.getId(), "COMMENT_REPLY");
    }
    private void watchers(Page page, String event, String actorId) {
        var recipients = sql.queryForList("SELECT user_id FROM page_watch WHERE page_id=:page AND NOT muted", Map.of("page", page.getId()), String.class);
        for (String recipient : recipients) if (!recipient.equals(actorId)) record(page, event, recipient, "PAGE_UPDATED");
    }
    private void mentions(Page page, String event, String body, String actorId) {
        for (String login : mentionLogins(body)) users.findOneByLogin(login).filter(user -> !user.getId().equals(actorId))
            .ifPresent(user -> record(page, event, user.getId(), "MENTION"));
    }
    private void record(Page page, String event, String recipient, String type) {
        String title = page.getTitle().length() > 200 ? page.getTitle().substring(0, 200) : page.getTitle();
        sql.update("INSERT INTO notification(id,type,title,link,created_at,recipient_id,page_id,event_key) VALUES(nextval('sequence_generator'),:type,:title,:link,CURRENT_TIMESTAMP,:recipient,:page,:event) ON CONFLICT(event_key,recipient_id,type) DO NOTHING",
            Map.of("type", type, "title", title, "link", "/s/" + page.getSpace().getSlug() + "/p/" + page.getId(), "recipient", recipient, "page", page.getId(), "event", event));
    }
    public static Set<String> mentionLogins(String markdown) {
        if (markdown == null) return Set.of();
        String text = markdown.replaceAll("(?ms)^ {0,3}(`{3,}|~{3,})[^\\n]*\\n.*?^ {0,3}\\1[^\\n]*(?:\\n|$)", " ")
            .replaceAll("`+[^`\\n]*`+", " ");
        var matcher = MENTION.matcher(text); Set<String> result = new LinkedHashSet<>();
        while (matcher.find()) { if (result.size() >= 100) throw new IllegalArgumentException("Too many mentions"); result.add(matcher.group(1).replaceAll("[.]+$", "").toLowerCase(java.util.Locale.ROOT)); }
        return result;
    }
}
