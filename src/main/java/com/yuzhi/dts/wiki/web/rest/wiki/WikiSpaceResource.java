package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.security.SecurityUtils;
import com.yuzhi.dts.wiki.service.UserService;
import com.yuzhi.dts.wiki.service.wiki.PageService;
import com.yuzhi.dts.wiki.service.wiki.TemplateService;
import com.yuzhi.dts.wiki.service.wiki.dto.PageDtos;
import com.yuzhi.dts.wiki.service.wiki.dto.SpaceDtos;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Business REST for spaces and the page tree (Sprint-6 design 03 S4).
 * All endpoints go through {@code SpaceAccessService}: invisible spaces read as 404,
 * read-only access writes as 403. Generated entity endpoints stay ROLE_ADMIN only.
 */
@RestController
@RequestMapping("/api/wiki")
public class WikiSpaceResource {

    private final PageService pageService;
    private final TemplateService templateService;
    private final UserService userService;

    public WikiSpaceResource(PageService pageService, TemplateService templateService, UserService userService) {
        this.pageService = pageService;
        this.templateService = templateService;
        this.userService = userService;
    }

    /** Single startup call for the frontend (design 10 S3.3): account + spaces + admin flag. */
    @GetMapping("/bootstrap")
    public BootstrapPayload bootstrap() {
        String login = SecurityUtils.getCurrentUserLogin().orElse("");
        var account = userService
            .getUserWithAuthoritiesByLogin(login)
            .map(u -> new AccountPayload(u.getLogin(), u.getFirstName(), u.getLastName(), u.getEmail()))
            .orElse(new AccountPayload(login, null, null, null));
        List<SpaceDtos.SpaceSummary> spaces = pageService.listSpaces();
        boolean canCreateSpace = SecurityUtils.hasCurrentUserThisAuthority(com.yuzhi.dts.wiki.security.AuthoritiesConstants.ADMIN);
        return new BootstrapPayload(account, spaces, canCreateSpace);
    }

    public record AccountPayload(String login, String firstName, String lastName, String email) {}

    public record BootstrapPayload(AccountPayload account, List<SpaceDtos.SpaceSummary> spaces, boolean canCreateSpace) {}

    @GetMapping("/templates")
    public List<TemplateService.TemplateItem> templates(@RequestParam(value = "space", required = false) String space) {
        return templateService.templates(space);
    }

    @GetMapping("/spaces")
    public List<SpaceDtos.SpaceSummary> spaces() {
        return pageService.listSpaces();
    }

    @GetMapping("/spaces/{slug}")
    public SpaceDtos.SpaceDetail space(@PathVariable String slug) {
        return pageService.getSpace(slug);
    }

    @GetMapping("/spaces/{slug}/tree")
    public List<SpaceDtos.TreeNode> tree(@PathVariable String slug) {
        return pageService.tree(slug);
    }

    @GetMapping("/spaces/{slug}/resolve")
    public PageDtos.PageIdResult resolve(@PathVariable String slug, @RequestParam("path") String path) {
        return pageService.resolve(slug, path);
    }

    @GetMapping("/spaces/{slug}/trash")
    public List<Long> trash(@PathVariable String slug) {
        return pageService.trash(slug).stream().map(p -> p.getId()).toList();
    }

    @GetMapping("/pages/{id}")
    public PageDtos.PageView page(@PathVariable Long id) {
        return pageService.getPage(id);
    }

    @PostMapping("/spaces/{slug}/pages")
    public ResponseEntity<PageDtos.PageView> create(@PathVariable String slug, @RequestBody PageDtos.CreatePageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pageService.createPage(slug, request));
    }

    @PutMapping("/pages/{id}/content")
    public PageDtos.SaveContentResult saveContent(@PathVariable Long id, @RequestBody PageDtos.SaveContentRequest request) {
        return pageService.saveContent(id, request);
    }

    @PatchMapping("/pages/{id}")
    public PageDtos.PageView update(@PathVariable Long id, @RequestBody PageDtos.UpdatePageRequest request) {
        return pageService.renameOrMove(id, request);
    }

    @PostMapping("/pages/{id}/copy")
    public ResponseEntity<PageDtos.PageView> copy(@PathVariable Long id, @RequestBody PageDtos.CopyPageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pageService.copyPage(id, request));
    }

    @DeleteMapping("/pages/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pageService.deletePage(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pages/{id}/restore")
    public PageDtos.PageView restore(@PathVariable Long id) {
        return pageService.restorePage(id);
    }
}
