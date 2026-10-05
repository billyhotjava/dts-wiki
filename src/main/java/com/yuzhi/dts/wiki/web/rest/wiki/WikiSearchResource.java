package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.service.wiki.WikiSearchService;
import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wiki")
public class WikiSearchResource {
    private final WikiSearchService search;
    public WikiSearchResource(WikiSearchService search) { this.search = search; }

    @GetMapping("/search")
    public ResponseEntity<WikiSearchService.Result> search(@RequestParam String q,
        @RequestParam(required = false) String space, @RequestParam(required = false) String type,
        @RequestParam(required = false) String status, @RequestParam(required = false) String owner,
        @RequestParam(required = false) String tag, @RequestParam(required = false) Instant since,
        @RequestParam(required = false) Instant until, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
            search.search(new WikiSearchService.Filters(q, space, type, status, owner, tag, since, until, page, size)));
    }
}
