package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.User;
import com.yuzhi.dts.wiki.repository.PageRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Private durable drafts and ephemeral soft presence. Neither publishes a page version. */
@Service
public class WikiEditingService {

    public record Draft(String contentMd, int baseVersionNo, Instant updatedAt) {}
    public record Presence(String login, String displayName, Instant expiresAt) {}
    private record EditorKey(long pageId, String userId) {}

    private static final int MAX_DRAFT_BYTES = 2_000_000;
    private static final int MAX_PRESENCE_ENTRIES = 10_000;
    private final ConcurrentHashMap<EditorKey, Presence> editors = new ConcurrentHashMap<>();
    private final WikiPersonalService personal;
    private final PageRepository pages;
    private final SpaceAccessService access;
    private final PageWritePolicy policy;
    private final NamedParameterJdbcTemplate sql;
    private final Clock clock;

    @Autowired
    public WikiEditingService(WikiPersonalService personal, PageRepository pages, SpaceAccessService access,
        PageWritePolicy policy, NamedParameterJdbcTemplate sql) {
        this(personal, pages, access, policy, sql, Clock.systemUTC());
    }

    WikiEditingService(WikiPersonalService personal, PageRepository pages, SpaceAccessService access,
        PageWritePolicy policy, NamedParameterJdbcTemplate sql, Clock clock) {
        this.personal = personal;
        this.pages = pages;
        this.access = access;
        this.policy = policy;
        this.sql = sql;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Draft draft(long id) {
        writable(id);
        var rows = sql.query("SELECT content_md,base_version_no,updated_at FROM page_draft WHERE page_id=:page AND user_id=:user",
            Map.of("page", id, "user", personal.current().getId()),
            (rs, row) -> new Draft(rs.getString(1), rs.getInt(2), rs.getTimestamp(3).toInstant()));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    @Transactional
    public void saveDraft(long id, int base, String markdown) {
        Page page = pages.findForUpdate(id).filter(value -> value.getDeletedAt() == null)
            .orElseThrow(() -> new SpaceNotVisibleException("page:" + id));
        access.requireWrite(page);
        policy.requireWritable(page);
        int current = page.getCurrentVersion() == null ? 0 : page.getCurrentVersion().getVersionNo();
        if (base < 0 || base > current || markdown == null || markdown.getBytes(StandardCharsets.UTF_8).length > MAX_DRAFT_BYTES) {
            throw new IllegalArgumentException("Invalid draft base or content size");
        }
        String user = personal.current().getId();
        // A delayed autosave after publishing identical text must not resurrect its draft.
        if (page.getCurrentVersion() != null && markdown.equals(page.getCurrentVersion().getContentMd())) {
            clearPublished(id, user, base, markdown);
            return;
        }
        sql.update("INSERT INTO page_draft(id,page_id,user_id,content_md,base_version_no,updated_at) "
            + "VALUES(nextval('sequence_generator'),:page,:user,:content,:base,CURRENT_TIMESTAMP) "
            + "ON CONFLICT(page_id,user_id) DO UPDATE SET content_md=EXCLUDED.content_md,base_version_no=EXCLUDED.base_version_no,updated_at=EXCLUDED.updated_at",
            Map.of("page", id, "user", user, "content", markdown, "base", base));
    }

    @Transactional
    public void discard(long id) {
        writable(id);
        sql.update("DELETE FROM page_draft WHERE page_id=:page AND user_id=:user", Map.of("page", id, "user", personal.current().getId()));
    }

    @Transactional
    public void clearPublished(long id, String user, int base, String markdown) {
        sql.update("DELETE FROM page_draft WHERE page_id=:page AND user_id=:user AND base_version_no<=:base AND content_md=:content",
            Map.of("page", id, "user", user, "base", base, "content", markdown));
    }

    @Transactional(readOnly = true)
    public synchronized List<Presence> heartbeat(long id) {
        writable(id);
        User user = personal.current();
        prune();
        EditorKey key = new EditorKey(id, user.getId());
        if (editors.size() >= MAX_PRESENCE_ENTRIES && !editors.containsKey(key)) {
            throw new IllegalArgumentException("Editing presence capacity reached");
        }
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
            + (user.getLastName() == null ? "" : user.getLastName())).strip();
        editors.put(key, new Presence(user.getLogin(), name.isBlank() ? user.getLogin() : name, clock.instant().plusSeconds(90)));
        return others(id, user.getId());
    }

    @Transactional(readOnly = true)
    public List<Presence> presence(long id) {
        writable(id);
        prune();
        return others(id, personal.current().getId());
    }

    @Transactional(readOnly = true)
    public void leave(long id) {
        writable(id);
        editors.remove(new EditorKey(id, personal.current().getId()));
    }

    private List<Presence> others(long id, String user) {
        return editors.entrySet().stream().filter(entry -> entry.getKey().pageId() == id && !entry.getKey().userId().equals(user))
            .map(Map.Entry::getValue).sorted(Comparator.comparing(Presence::login)).limit(50).toList();
    }

    private void prune() {
        Instant now = clock.instant();
        editors.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private Page writable(long id) {
        Page page = personal.visible(id);
        access.requireWrite(page);
        policy.requireWritable(page);
        return page;
    }
}
