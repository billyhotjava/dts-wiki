package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.domain.SyncRoot;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import com.yuzhi.dts.wiki.repository.PageRepository;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.repository.SyncRootRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies a fully validated inventory atomically, retaining removed spaces and content. */
@Service
public class ManifestReconciler {

    private final SpaceRepository spaces;
    private final SyncRootRepository roots;
    private final PageRepository pages;
    private final WikiProperties properties;
    private final com.yuzhi.dts.wiki.repository.SyncStateRepository states;

    public ManifestReconciler(SpaceRepository spaces, SyncRootRepository roots, PageRepository pages, WikiProperties properties, com.yuzhi.dts.wiki.repository.SyncStateRepository states) {
        this.spaces = spaces;
        this.roots = roots;
        this.pages = pages;
        this.properties = properties;
        this.states = states;
    }

    @Transactional
    public List<String> reconcile(SpaceManifestParser.Manifest manifest) {
        var config = properties.getContent();
        var inventory = new HashSet<String>();
        for (var entry : manifest.spaces()) {
            inventory.add(entry.slug());
            var existing = spaces.findOneBySlug(entry.slug());
            if (existing.isPresent() && !existing.orElseThrow().isManifestManaged()
                && !config.getRepoUrl().equals(existing.orElseThrow().getGitRepoUrl())) {
                throw new IllegalArgumentException("Manifest slug is already owned by another space: " + entry.slug());
            }
        }
        for (var entry : manifest.spaces()) {
            Space space = spaces.findOneBySlug(entry.slug()).orElseGet(Space::new);
            boolean created = space.getId() == null;
            boolean sourceChanged = !java.util.Objects.equals(space.getGitRepoUrl(), config.getRepoUrl())
                || !java.util.Objects.equals(space.getGitBranch(), config.getBranch());
            space.setSlug(entry.slug());
            space.setName(entry.name());
            space.setDescription(entry.description());
            space.setAccessRole(entry.role());
            space.setManifestManaged(true);
            space.setGitRepoUrl(config.getRepoUrl());
            space.setGitBranch(config.getBranch());
            if (created) { space.setArchived(false); }
            space = spaces.save(space);
            if (created) {
                Page rootPage = new Page();
                rootPage.setSpace(space);
                rootPage.setTitle(entry.name());
                rootPage.setKind(PageKind.FOLDER);
                rootPage.setPosition(1000);
                rootPage.setSyncStatus(PageSyncStatus.LOCAL_ONLY);
                rootPage.setCreatedAt(Instant.now());
                rootPage.setUpdatedAt(Instant.now());
                pages.save(rootPage);
            }
            List<SyncRoot> configured = roots.findBySpaceWithMount(space.getId());
            for (SyncRoot root : configured) {
                boolean enabled = entry.roots().contains(root.getRepoPath());
                if (enabled && (sourceChanged || !Boolean.TRUE.equals(root.getEnabled()))) {
                    states.findOneBySyncRootId(root.getId()).ifPresent(state -> state.setLastSyncedCommit(null));
                }
                root.setEnabled(enabled);
            }
            for (String path : entry.roots()) {
                if (configured.stream().noneMatch(root -> path.equals(root.getRepoPath()))) {
                    SyncRoot root = new SyncRoot();
                    root.setSpace(space);
                    root.setRepoPath(path);
                    root.setEnabled(true);
                    roots.save(root);
                }
            }
        }
        for (Space removed : spaces.findAll()) {
            if (removed.isManifestManaged() && !inventory.contains(removed.getSlug())) {
                for (SyncRoot root : roots.findBySpaceWithMount(removed.getId())) { root.setEnabled(false); }
            }
        }
        return manifest.spaces().stream().map(SpaceManifestParser.Entry::slug).toList();
    }
}
