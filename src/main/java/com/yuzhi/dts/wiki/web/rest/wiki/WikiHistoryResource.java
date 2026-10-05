package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.service.wiki.PageService;
import com.yuzhi.dts.wiki.service.wiki.WikiActivityService;
import com.yuzhi.dts.wiki.service.wiki.WikiHistoryService;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wiki")
public class WikiHistoryResource {
    private final WikiHistoryService history;
    private final PageService pages;
    private final WikiActivityService activity;
    public WikiHistoryResource(WikiHistoryService history, PageService pages, WikiActivityService activity) {
        this.history = history; this.pages = pages; this.activity = activity;
    }
    @GetMapping("/pages/{id}/versions")
    public ResponseEntity<WikiHistoryService.History> versions(@PathVariable long id, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(history.list(id, page, size));
    }
    @GetMapping("/pages/{id}/versions/{no}")
    public ResponseEntity<WikiHistoryService.Version> version(@PathVariable long id, @PathVariable int no) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(history.get(id, no));
    }
    public record RestoreRequest(Integer baseVersionNo) {}
    @PostMapping("/pages/{id}/versions/{no}/restore")
    public PageDtos.SaveContentResult restore(@PathVariable long id, @PathVariable int no, @RequestBody RestoreRequest request) {
        if (request.baseVersionNo() == null) throw new IllegalArgumentException("baseVersionNo is required");
        return pages.restoreVersion(id, no, request.baseVersionNo());
    }
    @GetMapping("/activity")
    public ResponseEntity<List<WikiActivityService.Item>> activity(@RequestParam(required = false) String space,
        @RequestParam(required = false) String author, @RequestParam(required = false) Instant since,
        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(activity.list(space, author, since, page, size));
    }
}
