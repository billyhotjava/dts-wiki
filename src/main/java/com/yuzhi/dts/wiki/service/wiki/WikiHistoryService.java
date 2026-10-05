package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.PageVersion;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.PageVersionRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WikiHistoryService {
    public record Version(int versionNo, String authorName, String viaAgent, String source, String message,
                          String gitCommit, Instant createdAt, String contentSha256, String contentMd) {}
    public record History(List<Version> items, long total) {}
    private final PageRepository pages;
    private final PageVersionRepository versions;
    private final SpaceAccessService access;
    public WikiHistoryService(PageRepository pages, PageVersionRepository versions, SpaceAccessService access) {
        this.pages = pages; this.versions = versions; this.access = access;
    }
    public History list(long id, int page, int size) {
        authorize(id);
        if (page < 0 || page > 1_000_000 || size < 1 || size > 200) throw new IllegalArgumentException("Invalid pagination");
        var history = versions.findByPageIdOrderByVersionNoDesc(id, PageRequest.of(page, size));
        return new History(history.getContent().stream().map(v -> view(v, false)).toList(), history.getTotalElements());
    }
    public Version get(long id, int no) {
        authorize(id);
        return view(versions.findByPageIdAndVersionNo(id, no).orElseThrow(() -> new SpaceNotVisibleException("version:" + no)), true);
    }
    private void authorize(long id) { access.requireRead(pages.findLive(id).orElseThrow(() -> new SpaceNotVisibleException("page:" + id))); }
    private static Version view(PageVersion v, boolean content) {
        return new Version(v.getVersionNo(), v.getAuthorName(), v.getViaAgent(), v.getSource().name(), v.getMessage(),
            v.getGitCommit(), v.getCreatedAt(), v.getContentSha256(), content ? v.getContentMd() : null);
    }
}
