package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.service.wiki.WikiEditingService;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wiki/pages/{id}")
public class WikiEditingResource {

    public record DraftRequest(Integer baseVersionNo, String contentMd) {}

    private final WikiEditingService editing;

    public WikiEditingResource(WikiEditingService editing) {
        this.editing = editing;
    }

    @GetMapping("/draft")
    public ResponseEntity<WikiEditingService.Draft> draft(@PathVariable long id) {
        var draft = editing.draft(id);
        return draft == null
            ? ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build()
            : ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(draft);
    }

    @PutMapping("/draft")
    public ResponseEntity<Void> save(@PathVariable long id, @RequestBody DraftRequest request) {
        if (request.baseVersionNo() == null) throw new IllegalArgumentException("Draft baseVersionNo is required");
        editing.saveDraft(id, request.baseVersionNo(), request.contentMd());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/draft")
    public ResponseEntity<Void> discard(@PathVariable long id) {
        editing.discard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/editing")
    public List<WikiEditingService.Presence> heartbeat(@PathVariable long id) {
        return editing.heartbeat(id);
    }

    @GetMapping("/editing")
    public List<WikiEditingService.Presence> presence(@PathVariable long id) {
        return editing.presence(id);
    }

    @DeleteMapping("/editing")
    public ResponseEntity<Void> leave(@PathVariable long id) {
        editing.leave(id);
        return ResponseEntity.noContent().build();
    }
}
