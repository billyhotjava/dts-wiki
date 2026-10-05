package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.service.wiki.WikiNotificationService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wiki/me/notifications")
public class WikiNotificationResource {
    private final WikiNotificationService notifications;
    public WikiNotificationResource(WikiNotificationService notifications) { this.notifications = notifications; }
    @GetMapping public WikiNotificationService.Notices list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return notifications.list(page, size); }
    @GetMapping("/unread-count") public Map<String, Long> unread() { return Map.of("count", notifications.list(0, 1).unread()); }
    @PostMapping("/read-all") public ResponseEntity<Void> readAll() { notifications.read(null); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/read") public ResponseEntity<Void> read(@PathVariable long id) { notifications.read(id); return ResponseEntity.noContent().build(); }
    @PostMapping("/{id}/open") public Map<String, String> open(@PathVariable long id) { return Map.of("url", notifications.open(id)); }
}
