package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.config.WikiProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * git CLI wrapper (design 04 S2, D8): every repository has a local clone that only
 * the sync scheduler touches. All commands run with a timeout and captured stderr;
 * SSH uses the space's deploy key, never the process default agent.
 */
@Service
public class GitRepoManager {

    private static final Logger LOG = LoggerFactory.getLogger(GitRepoManager.class);

    private final WikiProperties properties;

    public GitRepoManager(WikiProperties properties) {
        this.properties = properties;
    }

    public Path repoDir(String spaceSlug) {
        requireSlug(spaceSlug);
        return properties.reposPath().toAbsolutePath().normalize().resolve(spaceSlug);
    }

    public Path keyPath(String spaceSlug) {
        requireSlug(spaceSlug);
        if (properties.getContent().getRepoUrl() != null && !properties.getContent().getRepoUrl().isBlank()) {
            return Path.of(properties.getContent().getDeployKeyPath());
        }
        return properties.sshKeysPath().resolve(spaceSlug + ".key");
    }

    public boolean hasClone(String spaceSlug) {
        return Files.isDirectory(repoDir(spaceSlug).resolve(".git"));
    }

    public boolean hasKey(String spaceSlug) {
        return Files.isRegularFile(keyPath(spaceSlug));
    }

    /** Environment for SSH remotes: pinned deploy key, no agent forwarding surprises. */
    public Map<String, String> sshEnv(String spaceSlug) {
        Path key = keyPath(spaceSlug);
        Path knownHosts = properties.sshKeysPath().resolve("known_hosts");
        return Map.of(
            "GIT_SSH_COMMAND",
            "ssh -i " + shellQuote(key.toAbsolutePath().toString()) + " -o BatchMode=yes -o IdentitiesOnly=yes -o IdentityAgent=none -o StrictHostKeyChecking=yes -o UserKnownHostsFile=" + shellQuote(knownHosts.toAbsolutePath().toString())
        );
    }

    public String run(String spaceSlug, List<String> args, long timeoutSeconds) {
        return run(spaceSlug, args, Map.of(), timeoutSeconds);
    }


    public String run(String spaceSlug, List<String> args, Map<String, String> extraEnv, long timeoutSeconds) {
        Map<String, String> environment = new java.util.HashMap<>(sshEnv(spaceSlug));
        environment.putAll(extraEnv);
        return new String(execute(repoDir(spaceSlug), args, environment, timeoutSeconds), StandardCharsets.UTF_8).strip();
    }

    public String runIn(Path directory, List<String> args, Map<String, String> extraEnv, long timeoutSeconds) {
        return new String(execute(directory, args, extraEnv, timeoutSeconds), StandardCharsets.UTF_8).strip();
    }

    private byte[] execute(Path directory, List<String> args, Map<String, String> environment, long timeoutSeconds) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-c");
        command.add("core.quotePath=false");
        command.addAll(args);
        Process process = null;
        var readers = Executors.newVirtualThreadPerTaskExecutor();
        try {
            ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile());
            builder.environment().putAll(environment);
            builder.environment().put("GIT_TERMINAL_PROMPT", "0");
            process = builder.start();
            Process running = process;
            var stdout = readers.submit(() -> running.getInputStream().readAllBytes());
            var stderr = readers.submit(() -> running.getErrorStream().readAllBytes());
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new GitCommandException(String.join(" ", args), 124, "timeout after " + timeoutSeconds + "s");
            }
            byte[] output = stdout.get();
            String error = new String(stderr.get(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new GitCommandException(String.join(" ", args), process.exitValue(), tail(error));
            }
            return output;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitCommandException(String.join(" ", args), -1, "interrupted");
        } catch (IOException | ExecutionException e) {
            throw new GitCommandException(String.join(" ", args), -1, e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
            readers.shutdownNow();
        }
    }

    public byte[] fileBytesAt(String spaceSlug, String revision, String path) {
        return execute(repoDir(spaceSlug), List.of("show", revision + ":" + path), sshEnv(spaceSlug), 30);
    }

    public String fileAt(String spaceSlug, String revision, String path) {
        return new String(fileBytesAt(spaceSlug, revision, path), StandardCharsets.UTF_8);
    }

    public String head(String spaceSlug) {
        return run(spaceSlug, List.of("rev-parse", "HEAD"), 30);
    }

    public String commitAuthor(String spaceSlug, String revision, String path) {
        // login%00email of the most recent commit touching path
        return run(spaceSlug, List.of("log", "-1", "--format=%an%x00%ae", revision, "--", path), 30);
    }

    public record TrackedFile(String mode, String path) {}

    public record Change(String status, String fromPath, String path) {}

    public List<Change> changes(String slug, String from, String to, List<String> roots) {
        List<String> arguments = new ArrayList<>(List.of("diff", "--name-status", "-z", "-M50%", from + ".." + to, "--"));
        arguments.addAll(roots);
        String[] items = new String(execute(repoDir(slug), arguments, sshEnv(slug), 60), StandardCharsets.UTF_8).split("\u0000");
        List<Change> changes = new ArrayList<>();
        for (int i = 0; i < items.length && !items[i].isEmpty();) {
            String status = items[i++];
            String first = items[i++];
            if (status.startsWith("R") || status.startsWith("C")) { changes.add(new Change(status, first, items[i++])); }
            else { changes.add(new Change(status, null, first)); }
        }
        return List.copyOf(changes);
    }

    public List<TrackedFile> trackedFiles(String slug, String revision, String root) {
        byte[] listing = execute(repoDir(slug), List.of("ls-tree", "-r", "-z", revision, "--", root), sshEnv(slug), 60);
        List<TrackedFile> files = new ArrayList<>();
        for (String item : new String(listing, StandardCharsets.UTF_8).split("\u0000")) {
            int tab = item.indexOf('\t');
            if (tab > 0) { files.add(new TrackedFile(item.substring(0, 6), item.substring(tab + 1))); }
        }
        return List.copyOf(files);
    }

    public boolean isRegularFile(String slug, String revision, String path) {
        String entry = run(slug, List.of("ls-tree", revision, "--", path), 30);
        return entry.startsWith("100644 ") || entry.startsWith("100755 ");
    }

    private static void requireSlug(String slug) {
        if (slug == null || !(slug.equals("_content") || slug.matches("[a-z][a-z0-9-]{1,31}"))) {
            throw new IllegalArgumentException("Invalid repository slug");
        }
    }

    private static String shellQuote(String value) { return "'" + value.replace("'", "'\"'\"'") + "'"; }

    private static String tail(String stderr) {
        if (stderr == null) {
            return "";
        }
        String trimmed = stderr.strip();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(trimmed.length() - 500);
    }
}
