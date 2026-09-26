package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Page templates (Sprint-6 F4/T04): built-in skeletons plus per-space
 * {@code TEMPLATE} pages. Used by the new-page dialog and by
 * {@code POST /api/wiki/spaces/{slug}/pages} via {@code templateId}
 * (builtin key like {@code builtin:meeting}, or {@code page:<id>}).
 */
@Service
public class TemplateService {

    public record TemplateItem(String id, String title, String contentMd) {}

    private final PageRepository pageRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceAccessService spaceAccessService;

    public TemplateService(PageRepository pageRepository, SpaceRepository spaceRepository, SpaceAccessService spaceAccessService) {
        this.pageRepository = pageRepository;
        this.spaceRepository = spaceRepository;
        this.spaceAccessService = spaceAccessService;
    }

    @Transactional(readOnly = true)
    public List<TemplateItem> templates(String spaceSlug) {
        List<TemplateItem> items = new ArrayList<>(builtins());
        if (spaceSlug != null && !spaceSlug.isBlank()) {
            Space space = spaceRepository.findOneBySlug(spaceSlug).orElse(null);
            if (space != null && spaceAccessService.canRead(spaceSlug)) {
                for (Page page : pageRepository.findLiveBySpace(space.getId())) {
                    if (page.getKind() == PageKind.TEMPLATE && page.getCurrentVersion() != null) {
                        items.add(new TemplateItem("page:" + page.getId(), page.getTitle(), page.getCurrentVersion().getContentMd()));
                    }
                }
            }
        }
        return items;
    }

    /** Resolve template content for page creation; empty when unknown. */
    @Transactional(readOnly = true)
    public String resolve(String spaceSlug, String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return null;
        }
        for (TemplateItem item : templates(spaceSlug)) {
            if (item.id().equals(templateId)) {
                return item.contentMd();
            }
        }
        return null;
    }

    static List<TemplateItem> builtins() {
        return List.of(
            new TemplateItem(
                "builtin:blank",
                "空白页",
                ""
            ),
            new TemplateItem(
                "builtin:meeting",
                "会议纪要",
                "# 会议纪要\n\n- 时间：\n- 参会：\n- 记录：\n\n## 结论\n\n## 待办\n\n- [ ] \n"
            ),
            new TemplateItem(
                "builtin:adr",
                "架构决策（ADR）",
                "# ADR-NNN：标题\n\n## 背景\n\n## 决策\n\n## 备选\n\n## 后果\n\n"
            ),
            new TemplateItem(
                "builtin:sprint-task",
                "Sprint Task",
                "# TNN：标题\n\n**优先级**：P0 · **状态**：DRAFT\n\n## 目标\n\n## 技术设计\n\n## 验证\n\n- [ ] \n"
            )
        );
    }
}
