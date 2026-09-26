package com.yuzhi.dts.wiki.service.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yuzhi.dts.wiki.IntegrationTest;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import com.yuzhi.dts.wiki.service.wiki.dto.SpaceDtos;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * Page lifecycle against the real schema incl. custom constraints (Sprint-6 W4).
 * Runs as ADMIN so space checks pass; user-level checks live in SpaceAccessServiceTest.
 */
@IntegrationTest
@Transactional
@WithMockUser(authorities = { "ROLE_ADMIN" })
class PageServiceIT {

    @Autowired
    private PageService pageService;

    @Autowired
    private SpaceRepository spaceRepository;

    private Space space;

    @BeforeEach
    void initSpace() {
        space = new Space().slug("w4t").name("W4 Test").archived(false);
        space = spaceRepository.saveAndFlush(space);
    }

    private PageDtos.PageView root() {
        return pageService.createPage("w4t", new PageDtos.CreatePageRequest(null, "Root", "FOLDER", "# root"));
    }

    @Test
    void createReadAndTree() {
        PageDtos.PageView root = root();
        assertThat(root.versionNo()).isEqualTo(1);
        assertThat(root.editable()).isTrue();
        PageDtos.PageView child = pageService.createPage("w4t", new PageDtos.CreatePageRequest(root.id(), "Child", null, "hello"));
        assertThat(child.kind()).isEqualTo(PageKind.NATIVE.name());
        List<SpaceDtos.TreeNode> tree = pageService.tree("w4t");
        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).children()).extracting(SpaceDtos.TreeNode::title).containsExactly("Child");
        PageDtos.PageView read = pageService.getPage(child.id());
        assertThat(read.contentMd()).isEqualTo("hello");
        assertThat(read.breadcrumbs()).extracting(SpaceDtos.Breadcrumb::title).containsExactly("Root");
    }

    @Test
    void saveContentVersionsAndConflicts() {
        PageDtos.PageView root = root();
        // same content -> no new version (I9)
        assertThat(pageService.saveContent(root.id(), new PageDtos.SaveContentRequest(1, "# root", null)).versionNo()).isEqualTo(1);
        // changed content -> v2
        assertThat(pageService.saveContent(root.id(), new PageDtos.SaveContentRequest(1, "# root v2", "edit")).versionNo()).isEqualTo(2);
        // stale base -> 409
        assertThatThrownBy(() -> pageService.saveContent(root.id(), new PageDtos.SaveContentRequest(1, "other", null)))
            .isInstanceOf(PageVersionConflictException.class);
    }

    @Test
    void moveValidatesInvariants() {
        PageDtos.PageView root = root();
        PageDtos.PageView child = pageService.createPage("w4t", new PageDtos.CreatePageRequest(root.id(), "Child", null, null));
        // I6: cannot move under own descendant (root under child)
        assertThatThrownBy(() -> pageService.renameOrMove(root.id(), new PageDtos.UpdatePageRequest(null, child.id(), null)))
            .isInstanceOf(IllegalArgumentException.class);
        // rename works
        assertThat(pageService.renameOrMove(child.id(), new PageDtos.UpdatePageRequest("Renamed", null, null)).title()).isEqualTo("Renamed");
    }

    @Test
    void copyDeleteRestoreTrash() {
        PageDtos.PageView root = root();
        PageDtos.PageView child = pageService.createPage("w4t", new PageDtos.CreatePageRequest(root.id(), "Child", null, "body"));
        PageDtos.PageView copy = pageService.copyPage(child.id(), new PageDtos.CopyPageRequest(root.id(), null));
        assertThat(copy.title()).contains("copy");
        assertThat(copy.versionNo()).isEqualTo(1);
        pageService.deletePage(child.id());
        assertThatThrownBy(() -> pageService.getPage(child.id())).isInstanceOf(SpaceNotVisibleException.class); // I10 invisible
        assertThat(pageService.trash("w4t")).extracting(p -> p.getId()).contains(child.id());
        PageDtos.PageView restored = pageService.restorePage(child.id());
        assertThat(pageService.getPage(restored.id()).contentMd()).isEqualTo("body");
    }

    @Test
    void unknownSpaceReadsAs404() {
        assertThatThrownBy(() -> pageService.tree("nope")).isInstanceOf(SpaceNotVisibleException.class);
    }

    @Test
    @WithMockUser(authorities = { "ROLE_USER" })
    void nonMemberCannotWrite() {
        assertThatThrownBy(() -> pageService.createPage("w4t", new PageDtos.CreatePageRequest(null, "X", null, null)))
            .isInstanceOf(SpaceNotVisibleException.class);
    }

    @Test
    @WithMockUser(authorities = { "ROLE_USER", "ROLE_SPACE_W4T" })
    void readableButNotWritableGets403() {
        assertThatThrownBy(() -> pageService.createPage("w4t", new PageDtos.CreatePageRequest(null, "X", null, null)))
            .isInstanceOf(AccessDeniedException.class);
    }
}
