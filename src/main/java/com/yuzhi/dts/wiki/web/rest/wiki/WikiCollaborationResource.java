package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.service.wiki.WikiCommentService;
import com.yuzhi.dts.wiki.service.wiki.WikiNotificationIntents;
import com.yuzhi.dts.wiki.service.wiki.WikiPersonalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wiki")
public class WikiCollaborationResource {
    public record NewComment(String bodyMd, Long parentId) {}
    public record EditComment(String bodyMd, Boolean resolved) {}
    private final WikiPersonalService personal;
    private final WikiCommentService comments;
    private final WikiNotificationIntents intents;
    public WikiCollaborationResource(WikiPersonalService personal, WikiCommentService comments, WikiNotificationIntents intents) {
        this.personal = personal; this.comments = comments; this.intents = intents;
    }
    @GetMapping("/pages/{id}/personal") public WikiPersonalService.State state(@PathVariable long id) { return personal.state(id); }
    @GetMapping("/me/favorites") public WikiPersonalService.SavedPages favorites(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return personal.list(true, page, size); }
    @GetMapping("/me/recent-pages") public WikiPersonalService.SavedPages recent(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) { return personal.list(false, page, size); }
    @PutMapping("/me/favorites/{id}") public ResponseEntity<Void> favorite(@PathVariable long id) { personal.favorite(id, true); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/me/favorites/{id}") public ResponseEntity<Void> unfavorite(@PathVariable long id) { personal.favorite(id, false); return ResponseEntity.noContent().build(); }
    @PostMapping("/pages/{id}/view") public ResponseEntity<Void> view(@PathVariable long id) { personal.view(id); return ResponseEntity.noContent().build(); }
    @PutMapping("/pages/{id}/watch") public ResponseEntity<Void> watch(@PathVariable long id) { intents.watch(personal.visible(id), personal.current(), true); return ResponseEntity.noContent().build(); }
    @DeleteMapping("/pages/{id}/watch") public ResponseEntity<Void> unwatch(@PathVariable long id) { intents.watch(personal.visible(id), personal.current(), false); return ResponseEntity.noContent().build(); }
    @GetMapping("/pages/{id}/comments") public WikiCommentService.Comments comments(@PathVariable long id, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) { return comments.list(id, page, size); }
    @PostMapping("/pages/{id}/comments") public ResponseEntity<WikiCommentService.CommentView> comment(@PathVariable long id, @RequestBody NewComment request) { return ResponseEntity.status(201).body(comments.create(id, request.bodyMd(), request.parentId())); }
    @PatchMapping("/comments/{id}") public WikiCommentService.CommentView edit(@PathVariable long id, @RequestBody EditComment request) { return comments.update(id, request.bodyMd(), request.resolved()); }
    @DeleteMapping("/comments/{id}") public ResponseEntity<Void> delete(@PathVariable long id) { comments.delete(id); return ResponseEntity.noContent().build(); }
}
