package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Identity and email IO are deliberately outside the content transaction. */
@Service
public class WikiNotificationService {
    public record Notice(long id, String type, String title, String url, Instant createdAt, boolean read) {}
    public record Notices(List<Notice> items, long total, long unread) {}
    private record Pending(long id, String recipient, long pageId, String type, String title, String slug, String role, boolean live, Instant createdAt) {}
    private final NamedParameterJdbcTemplate sql;
    private final WikiPersonalService personal;
    private final SpaceAccessService access;
    private final SpaceRepository spaces;
    private final CurrentIdentityService identity;
    private final ObjectProvider<JavaMailSender> mail;
    private final WikiProperties.Notifications settings;
    private final TransactionTemplate tx;
    public WikiNotificationService(NamedParameterJdbcTemplate sql, WikiPersonalService personal, SpaceAccessService access, SpaceRepository spaces,
        CurrentIdentityService identity, ObjectProvider<JavaMailSender> mail, WikiProperties properties, PlatformTransactionManager manager) {
        this.sql = sql; this.personal = personal; this.access = access; this.spaces = spaces; this.identity = identity; this.mail = mail;
        settings = properties.getNotifications(); tx = new TransactionTemplate(manager);
    }
    private Set<Long> currentSpaces() {
        var target = identity.lookup(personal.current().getId()); var claimed = access.readableSpaceIds();
        return spaces.findAll().stream().filter(space -> claimed.contains(space.getId()) && target.canRead(space)).map(com.yuzhi.dts.wiki.domain.Space::getId).collect(Collectors.toSet());
    }
    public Notices list(int page, int size) {
        if (page < 0 || page > 10000 || size < 1 || size > 50) throw new IllegalArgumentException("Invalid notification pagination");
        Set<Long> allowed = currentSpaces(); if (allowed.isEmpty()) return new Notices(List.of(), 0, 0);
        var params = Map.of("user", personal.current().getId(), "spaces", allowed, "offset", page * size, "size", size);
        String from = " FROM notification n JOIN page p ON p.id=n.page_id JOIN space s ON s.id=p.space_id WHERE n.recipient_id=:user AND n.auth_state<>'SUPPRESSED' AND p.deleted_at IS NULL AND s.id IN (:spaces)";
        long total = sql.queryForObject("SELECT count(*)" + from, params, Long.class), unread = sql.queryForObject("SELECT count(*)" + from + " AND n.read_at IS NULL", params, Long.class);
        var items = sql.query("SELECT n.id,n.type,n.title,n.created_at,n.read_at,s.slug,p.id AS page_id" + from + " ORDER BY n.created_at DESC,n.id DESC LIMIT :size OFFSET :offset", params,
            (rs, row) -> new Notice(rs.getLong("id"), rs.getString("type"), rs.getString("title"), "/s/" + rs.getString("slug") + "/p/" + rs.getLong("page_id"), rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("read_at") != null));
        return new Notices(items, total, unread);
    }
    public void read(Long id) {
        Set<Long> allowed = currentSpaces(); if (allowed.isEmpty()) { if (id != null) throw new SpaceNotVisibleException("notification"); return; }
        var params = new java.util.HashMap<String, Object>(Map.of("user", personal.current().getId(), "spaces", allowed)); if (id != null) params.put("id", id);
        int count = update("UPDATE notification n SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) FROM page p WHERE n.page_id=p.id AND n.recipient_id=:user AND n.auth_state<>'SUPPRESSED' AND p.deleted_at IS NULL AND p.space_id IN (:spaces)" + (id == null ? "" : " AND n.id=:id"), params);
        if (id != null && count == 0) throw new SpaceNotVisibleException("notification");
    }
    public String open(long id) {
        Set<Long> allowed = currentSpaces(); if (allowed.isEmpty()) throw new SpaceNotVisibleException("notification");
        var urls = sql.queryForList("SELECT '/s/' || s.slug || '/p/' || p.id FROM notification n JOIN page p ON p.id=n.page_id JOIN space s ON s.id=p.space_id WHERE n.id=:id AND n.recipient_id=:user AND n.auth_state<>'SUPPRESSED' AND p.deleted_at IS NULL AND p.space_id IN (:spaces)", Map.of("id", id, "user", personal.current().getId(), "spaces", allowed), String.class);
        if (urls.isEmpty()) throw new SpaceNotVisibleException("notification"); read(id); return urls.getFirst();
    }

    @Scheduled(fixedDelayString="${application.wiki.notifications.worker-delay-ms:60000}", initialDelayString="${application.wiki.notifications.worker-delay-ms:60000}")
    public synchronized void process() {
        update("UPDATE notification SET delivery_state='PENDING' WHERE delivery_state='SENDING' AND retry_at<=CURRENT_TIMESTAMP", Map.of());
        var pending = sql.query("SELECT n.id,n.recipient_id,n.page_id,n.type,n.title,n.created_at,s.slug,s.access_role,p.deleted_at FROM notification n LEFT JOIN page p ON p.id=n.page_id LEFT JOIN space s ON s.id=p.space_id WHERE n.retry_at<=CURRENT_TIMESTAMP AND (n.auth_state='PENDING' OR n.auth_state='ALLOWED' AND n.delivery_state='PENDING') ORDER BY n.id LIMIT 100", Map.of(),
            (rs, row) -> new Pending(rs.getLong("id"), rs.getString("recipient_id"), rs.getLong("page_id"), rs.getString("type"), rs.getString("title"), rs.getString("slug"), rs.getString("access_role"), rs.getString("slug") != null && rs.getTimestamp("deleted_at") == null, rs.getTimestamp("created_at").toInstant()));
        for (Pending notice : pending) {
            try {
                if (!notice.live()) { suppress(notice.id()); continue; }
                var target = identity.lookup(notice.recipient());
                if (!target.canRead(notice.slug(), notice.role())) { suppress(notice.id()); continue; }
                update("UPDATE notification SET auth_state='ALLOWED' WHERE id=:id", Map.of("id", notice.id()));
                if (!settings.isMailEnabled() || target.verifiedEmail() == null) {
                    update("UPDATE notification SET delivery_state='SKIPPED' WHERE id=:id AND delivery_state='PENDING'", Map.of("id", notice.id())); continue;
                }
                if (notice.type().equals("PAGE_UPDATED") && notice.createdAt().plusSeconds(600).isAfter(Instant.now())) {
                    update("UPDATE notification SET retry_at=created_at+interval '10 minutes' WHERE id=:id", Map.of("id", notice.id())); continue;
                }
                deliver(notice, target.verifiedEmail());
            } catch (IdentityUnavailableException e) {
                update("UPDATE notification SET retry_at=CURRENT_TIMESTAMP+interval '1 minute' WHERE id=:id", Map.of("id", notice.id()));
                LoggerFactory.getLogger(getClass()).warn("wiki_notification_identity_unavailable notificationId={}", notice.id());
            }
        }
    }
    private int update(String statement, Map<String, ?> parameters) {
        return java.util.Objects.requireNonNull(tx.execute(status -> sql.update(statement, parameters)));
    }
    private void suppress(long id) { update("UPDATE notification SET auth_state='SUPPRESSED',delivery_state='SUPPRESSED' WHERE id=:id", Map.of("id", id)); }
    private void deliver(Pending notice, String email) {
        // Claim a bounded group in the database, then release the transaction before SMTP.
        List<Long> ids = tx.execute(status -> {
            String group = notice.type().equals("PAGE_UPDATED") ? " AND type='PAGE_UPDATED' AND created_at<=:until" : " AND id=:id";
            var params = Map.of("recipient", notice.recipient(), "page", notice.pageId(), "id", notice.id(), "until", java.sql.Timestamp.from(notice.createdAt().plusSeconds(600)));
            var selected = sql.queryForList("SELECT id FROM notification WHERE recipient_id=:recipient AND page_id=:page AND auth_state<>'SUPPRESSED' AND delivery_state='PENDING'" + group + " ORDER BY id LIMIT 100 FOR UPDATE SKIP LOCKED", params, Long.class);
            if (!selected.isEmpty()) update("UPDATE notification SET delivery_state='SENDING',retry_at=CURRENT_TIMESTAMP+interval '5 minutes' WHERE id IN (:ids)", Map.of("ids", selected));
            return selected;
        });
        if (ids == null || ids.isEmpty()) return;
        try {
            // Revalidate immediately before sending; revocation between intent and dispatch suppresses the entire group.
            var current = identity.lookup(notice.recipient());
            if (!current.canRead(notice.slug(), notice.role()) || sql.queryForObject("SELECT count(*) FROM page WHERE id=:page AND deleted_at IS NULL", Map.of("page", notice.pageId()), Long.class) == 0) { update("UPDATE notification SET auth_state='SUPPRESSED',delivery_state='SUPPRESSED' WHERE id IN (:ids)", Map.of("ids", ids)); return; }
            if (current.verifiedEmail() == null) { update("UPDATE notification SET delivery_state='SKIPPED' WHERE id IN (:ids)", Map.of("ids", ids)); return; }
            JavaMailSender sender = mail.getIfAvailable();
            URISettings.validate(settings);
            if (sender == null) throw new IllegalStateException("Mail transport unavailable");
            if (sender instanceof org.springframework.mail.javamail.JavaMailSenderImpl transport) {
                var properties = transport.getJavaMailProperties();
                properties.putIfAbsent("mail.smtp.connectiontimeout", "2000");
                properties.putIfAbsent("mail.smtp.timeout", "5000");
                properties.putIfAbsent("mail.smtp.writetimeout", "5000");
            }
            SimpleMailMessage message = new SimpleMailMessage(); message.setFrom(settings.getFrom()); message.setTo(current.verifiedEmail());
            message.setSubject("Wiki: " + notice.title().replaceAll("[\\r\\n]", " "));
            message.setText(notice.title() + "\n" + settings.getPublicUrl().replaceAll("/+$", "") + "/s/" + notice.slug() + "/p/" + notice.pageId() + "\nUpdates: " + ids.size());
            sender.send(message);
            update("UPDATE notification SET delivery_state='SENT',mail_at=CURRENT_TIMESTAMP WHERE id IN (:ids)", Map.of("ids", ids));
        } catch (IdentityUnavailableException e) {
            update("UPDATE notification SET delivery_state='PENDING',retry_at=CURRENT_TIMESTAMP+interval '1 minute' WHERE id IN (:ids)", Map.of("ids", ids));
        } catch (RuntimeException e) {
            update("UPDATE notification SET attempts=attempts+1,delivery_state=CASE WHEN attempts>=7 THEN 'FAILED' ELSE 'PENDING' END,retry_at=CURRENT_TIMESTAMP+interval '1 minute'*LEAST(60,power(2,attempts)::integer) WHERE id IN (:ids)", Map.of("ids", ids));
            LoggerFactory.getLogger(getClass()).warn("wiki_notification_mail_retry notificationId={}", notice.id());
        }
    }
    private static class URISettings {
        static void validate(WikiProperties.Notifications settings) {
            var uri = java.net.URI.create(settings.getPublicUrl());
            if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || settings.getFrom().isBlank() || settings.getFrom().contains("\n")) throw new IllegalStateException("Mail settings unavailable");
        }
    }
}
