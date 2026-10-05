package com.yuzhi.dts.wiki.service.wiki.dto;

import java.util.List;

/** Space list/detail payloads (design 03 S4). */
public final class SpaceDtos {

    private SpaceDtos() {}

    public record SpaceSummary(String slug, String name, String description, long pageCount, String syncStatus) {}

    public record SyncRootInfo(String repoPath, Long mountPageId, boolean enabled) {}

    public record SpaceDetail(
        String slug,
        String name,
        String description,
        Long rootPageId,
        List<SyncRootInfo> syncRoots
    ) {}

    public record TreeNode(Long id, String title, String kind, boolean hasChildren, String syncStatus, boolean readOnly, List<TreeNode> children) {}

    public record Breadcrumb(Long id, String title) {}
}
