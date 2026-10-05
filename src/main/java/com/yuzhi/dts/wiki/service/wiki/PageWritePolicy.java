package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.repository.PageRepository;
import org.springframework.stereotype.Component;

/** Protects Git-owned content even when a request targets an ancestor or a removed root. */
@Component
public class PageWritePolicy {
    private final WikiProperties properties;
    private final PageRepository pages;

    public PageWritePolicy(WikiProperties properties, PageRepository pages) {
        this.properties = properties;
        this.pages = pages;
    }

    public boolean gitReadOnly(Page page) {
        if (page == null) { return false; }
        if (properties.isOutboundEnabled() && !page.getSpace().isManifestManaged()) { return false; }
        for (Page cursor = page; cursor != null; cursor = cursor.getParent()) {
            if (cursor.getKind() == PageKind.GIT || cursor.getGitPath() != null) { return true; }
        }
        return false;
    }

    public void requireWritable(Page page) {
        if (gitReadOnly(page)) { throw new GitPageReadOnlyException(); }
    }

    public void requireWritableSubtree(Page page) {
        requireWritable(page);
        for (Page candidate : pages.findBySpaceId(page.getSpace().getId())) {
            for (Page cursor = candidate; cursor != null; cursor = cursor.getParent()) {
                if (page.getId().equals(cursor.getId())) { requireWritable(candidate); break; }
            }
        }
    }
}
