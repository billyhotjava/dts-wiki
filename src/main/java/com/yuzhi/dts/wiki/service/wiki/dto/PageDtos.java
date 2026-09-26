package com.yuzhi.dts.wiki.service.wiki.dto;

import java.time.Instant;
import java.util.List;

/** Page payloads (design 03 S4). */
public final class PageDtos {

    private PageDtos() {}

    public record PageView(
        Long id,
        String spaceSlug,
        String title,
        String kind,
        String contentMd,
        Integer versionNo,
        Instant updatedAt,
        String updatedBy,
        String gitPath,
        String gitRepoUrl,
        String gitCommit,
        String syncStatus,
        List<SpaceDtos.Breadcrumb> breadcrumbs,
        List<String> labels,
        boolean watching,
        boolean editable
    ) {}

    public record CreatePageRequest(Long parentId, String title, String kind, String templateId, String contentMd) {
        public CreatePageRequest(Long parentId, String title, String kind, String contentMd) {
            this(parentId, title, kind, null, contentMd);
        }
    }

    public record SaveContentRequest(Integer baseVersionNo, String contentMd, String message) {}

    public record SaveContentResult(Integer versionNo) {}

    public record UpdatePageRequest(String title, Long parentId, Integer position) {}

    public record CopyPageRequest(Long targetParentId, String title) {}

    public record PageIdResult(Long pageId) {}
}
