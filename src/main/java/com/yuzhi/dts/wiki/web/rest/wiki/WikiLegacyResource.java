package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.service.wiki.LegacyLinkService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wiki/legacy")
public class WikiLegacyResource {
    private final LegacyLinkService links;
    public WikiLegacyResource(LegacyLinkService links) { this.links = links; }

    @GetMapping("/resolve")
    public ResponseEntity<LegacyLinkService.Destination> resolve(@RequestParam String space,
        @RequestParam(defaultValue = "") String path) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(links.resolve(space, path));
    }
}
