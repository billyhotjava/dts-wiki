package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.service.wiki.PageService;
import com.yuzhi.dts.wiki.service.wiki.WikiQueryService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

/** Personal-identity Markdown and metadata access, using the same page authorization. */
@RestController
@RequestMapping("/api/wiki")
public class WikiContentResource {
    private final PageService pages;
    private final WikiQueryService queries;

    public WikiContentResource(PageService pages, WikiQueryService queries) {
        this.pages = pages;
        this.queries = queries;
    }

    @GetMapping("/query")
    public ResponseEntity<List<WikiQueryService.Item>> query(
        @RequestParam String space, @RequestParam(required = false) String type,
        @RequestParam(required = false) String status, @RequestParam(required = false) String owner,
        @RequestParam(required = false) String sprint, @RequestParam(required = false) String feature,
        @RequestParam(required = false) String priority, @RequestParam(required = false) String tag,
        @RequestParam(required = false) String q, @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        var result = queries.query(new WikiQueryService.Filters(space, type, status, owner, sprint, feature, priority, tag, q, page, size));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Total-Count", Long.toString(result.total()))
            .header("X-Page", Integer.toString(result.page())).header("X-Page-Size", Integer.toString(result.size())).body(result.items());
    }

    @GetMapping("/pages/{id}/markdown")
    public ResponseEntity<String> markdown(@PathVariable Long id, WebRequest request) {
        var page = pages.getPage(id);
        String tag = "\"v" + (page.versionNo() == null ? 0 : page.versionNo()) + "\"";
        if (request.checkNotModified(tag)) return ResponseEntity.status(304).eTag(tag).cacheControl(CacheControl.noCache().cachePrivate()).build();
        var response = ResponseEntity.ok().contentType(MediaType.parseMediaType("text/markdown;charset=UTF-8"))
            .cacheControl(CacheControl.noCache().cachePrivate()).eTag(tag).header("X-Wiki-Version", String.valueOf(page.versionNo() == null ? 0 : page.versionNo()));
        if (page.gitPath() != null) response.header("X-Wiki-Git-Path", page.gitPath());
        if (page.meta() != null && page.meta().docType() != null) response.header("X-Wiki-Doc-Type", page.meta().docType());
        return response.body(page.contentMd() == null ? "" : page.contentMd());
    }

    @GetMapping("/spaces/{slug}/markdown")
    public ResponseEntity<String> markdownAtPath(@PathVariable String slug, @RequestParam String path, WebRequest request) {
        return markdown(pages.resolve(slug, path).pageId(), request);
    }

    @GetMapping("/spaces/{slug}/llms.txt")
    public ResponseEntity<String> llms(@PathVariable String slug, WebRequest request) throws java.security.NoSuchAlgorithmException {
        String text = queries.llms(slug);
        String tag = "\"" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))) + "\"";
        if (request.checkNotModified(tag)) return ResponseEntity.status(304).eTag(tag).header("Cache-Control", "private, max-age=60, no-cache").build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")).eTag(tag)
            .header("Cache-Control", "private, max-age=60, no-cache").body(text);
    }
}
