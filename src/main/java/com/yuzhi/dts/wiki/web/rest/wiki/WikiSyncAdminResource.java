package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.service.wiki.sync.ConflictService;
import com.yuzhi.dts.wiki.service.wiki.sync.ImportService;
import com.yuzhi.dts.wiki.service.wiki.sync.SyncAdminService;
import com.yuzhi.dts.wiki.domain.enumeration.ConflictResolution;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Sync administration + conflict resolution (design 03 S4, F4/T02 + F5/T06 admin part).
 * NOTE: paths live under {@code /api/wiki/admin} (not the bare {@code /admin} in 03 §4),
 * because the bare frontend routes are public shells — management APIs stay authenticated
 * and ADMIN-gated here.
 */
@RestController
@RequestMapping("/api/wiki/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class WikiSyncAdminResource {

    private final SyncAdminService adminService;
    private final ConflictService conflictService;
    private final ImportService importService;

    public WikiSyncAdminResource(SyncAdminService adminService, ConflictService conflictService, ImportService importService) {
        this.adminService = adminService;
        this.conflictService = conflictService;
        this.importService = importService;
    }

    @GetMapping("/sync")
    public List<SyncAdminService.SpaceSyncStatus> sync() {
        return adminService.statuses();
    }

    @PostMapping("/sync/{slug}/run")
    public ResponseEntity<Void> run(@PathVariable String slug) {
        adminService.runNow(slug);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/sync/{slug}/public-key")
    public Map<String, String> publicKey(@PathVariable String slug) {
        return Map.of("publicKey", adminService.publicKey(slug));
    }

    @PostMapping("/sync/{slug}/test")
    public Map<String, String> test(@PathVariable String slug) {
        return Map.of("result", adminService.testConnection(slug));
    }

    @PostMapping("/spaces")
    public Map<String, String> createSpace(@RequestBody CreateSpaceRequest request) {
        var space = adminService.createSpace(request.slug(), request.name(), request.description(), request.gitRepoUrl(), request.gitBranch(), request.syncRoots());
        return Map.of("slug", space.getSlug());
    }

    public record CreateSpaceRequest(String slug, String name, String description, String gitRepoUrl, String gitBranch, List<SyncAdminService.RootSpec> syncRoots) {}

    @PostMapping("/import/{slug}")
    public ImportService.ImportReport importSpace(@PathVariable String slug, @RequestParam(value = "importHistory", defaultValue = "true") boolean importHistory) {
        return importService.importSpace(slug, importHistory, 20);
    }

    @GetMapping("/conflicts")
    public List<Map<String, Object>> conflicts(@RequestParam(value = "space", required = false) String space) {
        return conflictService
            .openConflicts(space)
            .stream()
            .map(c -> Map.<String, Object>of("id", c.getId(), "pageId", c.getPage() == null ? null : c.getPage().getId(), "detectedAt", String.valueOf(c.getDetectedAt())))
            .toList();
    }

    @GetMapping("/conflicts/{id}")
    public ConflictService.ConflictView conflict(@PathVariable Long id) {
        return conflictService.view(id);
    }

    @PostMapping("/conflicts/{id}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable Long id, @RequestBody ResolveRequest request) {
        String login = SecurityContextHolder.getContext().getAuthentication() == null
            ? "unknown"
            : SecurityContextHolder.getContext().getAuthentication().getName();
        conflictService.resolve(id, request.contentMd(), request.resolution(), login);
        return ResponseEntity.ok().build();
    }

    public record ResolveRequest(String contentMd, ConflictResolution resolution) {}
}
