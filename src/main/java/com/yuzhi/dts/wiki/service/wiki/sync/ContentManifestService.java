package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.config.WikiProperties;
import java.nio.file.Files;
import java.util.List;
import org.springframework.stereotype.Service;

/** Fetches one immutable repository snapshot before changing any database inventory. */
@Service
public class ContentManifestService {

    private static final String INVENTORY_CLONE = "_content";
    private final GitRepoManager git;
    private final WikiProperties properties;
    private final SpaceManifestParser parser;
    private final ManifestReconciler reconciler;
    private volatile Snapshot snapshot;

    public record Snapshot(String commit, List<String> slugs) {
        public Snapshot { slugs = List.copyOf(slugs); }
    }

    public ContentManifestService(GitRepoManager git, WikiProperties properties, SpaceManifestParser parser, ManifestReconciler reconciler) {
        this.git = git;
        this.properties = properties;
        this.parser = parser;
        this.reconciler = reconciler;
    }

    public synchronized Snapshot refresh() {
        var config = properties.getContent();
        if (config.getRepoUrl() == null || config.getRepoUrl().isBlank()) { return null; }
        String branch = config.getBranch();
        if (branch == null || branch.isBlank() || branch.startsWith("-")) { throw new IllegalArgumentException("Invalid content branch"); }
        validatePath(config.getManifestPath());
        try {
            Files.createDirectories(git.repoDir(INVENTORY_CLONE).getParent());
            git.runIn(git.repoDir(INVENTORY_CLONE).getParent(), List.of("check-ref-format", "refs/heads/" + branch), java.util.Map.of(), 10);
            if (!git.hasClone(INVENTORY_CLONE)) {
                git.runIn(git.repoDir(INVENTORY_CLONE).getParent(), List.of("clone", "--no-checkout", "--single-branch", "--branch", branch, "--", config.getRepoUrl(), git.repoDir(INVENTORY_CLONE).toString()), git.sshEnv(INVENTORY_CLONE), 300);
            } else {
                git.run(INVENTORY_CLONE, List.of("remote", "set-url", "origin", config.getRepoUrl()), 10);
            }
            git.run(INVENTORY_CLONE, List.of("fetch", "origin", "+refs/heads/" + branch + ":refs/remotes/origin/" + branch), 60);
            String commit = git.run(INVENTORY_CLONE, List.of("rev-parse", "origin/" + branch), 10);
            requireRegularPath(commit, config.getManifestPath(), false);
            var manifest = parser.parse(git.fileAt(INVENTORY_CLONE, commit, config.getManifestPath()));
            for (var entry : manifest.spaces()) {
                for (String root : entry.roots()) { requireRegularPath(commit, root, true); }
            }
            var current = new Snapshot(commit, reconciler.reconcile(manifest));
            snapshot = current;
            return current;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot prepare the content inventory clone", e);
        }
    }

    public Snapshot snapshot() { return snapshot; }

    private void requireRegularPath(String commit, String path, boolean directory) {
        String[] parts = path.split("/");
        String prefix = "";
        for (int i = 0; i < parts.length; i++) {
            prefix = prefix.isEmpty() ? parts[i] : prefix + "/" + parts[i];
            String item = git.run(INVENTORY_CLONE, List.of("ls-tree", commit, "--", prefix), 20);
            String mode = i < parts.length - 1 || directory ? "040000 " : "100644 ";
            if (!item.startsWith(mode) && !(i == parts.length - 1 && !directory && item.startsWith("100755 "))) {
                throw new IllegalArgumentException("Content path is missing or crosses a symlink/gitlink: " + prefix);
            }
        }
    }

    private static void validatePath(String path) {
        if (path == null || !path.matches("[A-Za-z0-9._-]+(?:/[A-Za-z0-9._-]+)*")
            || java.util.Arrays.stream(path.split("/")).anyMatch(part -> part.equals(".") || part.equals(".."))) {
            throw new IllegalArgumentException("Invalid content manifest path");
        }
    }
}
