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
        return properties.reposPath().resolve(spaceSlug);
    }

    public Path keyPath(String spaceSlug) {
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
            "ssh -i " + key.toAbsolutePath() + " -o IdentitiesOnly=yes -o StrictHostKeyChecking=accept-new -o UserKnownHostsFile=" + knownHosts.toAbsolutePath()
        );
    }

    public String run(String spaceSlug, List<String> args, long timeoutSeconds) {
        return run(spaceSlug, args, Map.of(), timeoutSeconds);
    }

    public String run(String spaceSlug, List<String> args, Map<String, String> extraEnv, long timeoutSeconds) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(args);
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(repoDir(spaceSlug).toFile());
            builder.environment().putAll(sshEnv(spaceSlug));
            builder.environment().putAll(extraEnv);
            builder.redirectErrorStream(false);
            Process process = builder.start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new GitCommandException(String.join(" ", args), 124, "timeout after " + timeoutSeconds + "s");
            }
            if (process.exitValue() != 0) {
                throw new GitCommandException(String.join(" ", args), process.exitValue(), tail(stderr));
            }
            return stdout.strip();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitCommandException(String.join(" ", args), -1, e.getMessage());
        }
    }

    public String runIn(Path directory, List<String> args, Map<String, String> extraEnv, long timeoutSeconds) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(args);
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(directory.toFile());
            builder.environment().putAll(extraEnv);
            Process process = builder.start();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new GitCommandException(String.join(" ", args), 124, "timeout after " + timeoutSeconds + "s");
            }
            if (process.exitValue() != 0) {
                throw new GitCommandException(String.join(" ", args), process.exitValue(), tail(stderr));
            }
            return stdout.strip();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GitCommandException(String.join(" ", args), -1, e.getMessage());
        }
    }

    public String fileAt(String spaceSlug, String revision, String path) {
        return run(spaceSlug, List.of("show", revision + ":" + path), 30);
    }

    public String head(String spaceSlug) {
        return run(spaceSlug, List.of("rev-parse", "HEAD"), 30);
    }

    public String commitAuthor(String spaceSlug, String revision, String path) {
        // login%00email of the most recent commit touching path
        return run(spaceSlug, List.of("log", "-1", "--format=%an%x00%ae", revision, "--", path), 30);
    }

    private static String tail(String stderr) {
        if (stderr == null) {
            return "";
        }
        String trimmed = stderr.strip();
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(trimmed.length() - 500);
    }
}
