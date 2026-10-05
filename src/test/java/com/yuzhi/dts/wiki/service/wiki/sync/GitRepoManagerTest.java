package com.yuzhi.dts.wiki.service.wiki.sync;

import static org.assertj.core.api.Assertions.*;

import com.yuzhi.dts.wiki.config.WikiProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitRepoManagerTest {
    @TempDir Path directory;

    private GitRepoManager manager() {
        WikiProperties properties = new WikiProperties();
        properties.setReposDir(directory.toString());
        return new GitRepoManager(properties);
    }

    @Test
    void fileReadsPreserveAllSourceWhitespaceAndUnicode() throws Exception {
        Path repo = Files.createDirectory(directory.resolve("sample"));
        GitRepoManager git = manager();
        git.runIn(repo, List.of("init", "-b", "main"), Map.of(), 10);
        String markdown = "  # 公司文档\r\n\r\ntext  \r\n\r\n";
        Files.writeString(repo.resolve("page.md"), markdown);
        git.runIn(repo, List.of("add", "page.md"), Map.of(), 10);
        git.runIn(repo, List.of("-c", "user.name=Test", "-c", "user.email=test@example.test", "commit", "-m", "source"), Map.of(), 10);
        assertThat(git.fileAt("sample", "HEAD", "page.md")).isEqualTo(markdown);
    }

    @Test
    void timeoutStopsACommandBeforeItsOutputStreamsClose() throws Exception {
        GitRepoManager git = manager();
        long start = System.nanoTime();
        assertThatThrownBy(() -> git.runIn(directory, List.of("-c", "alias.pause=!sleep 30", "pause"), Map.of(), 1))
            .isInstanceOf(GitCommandException.class).hasMessageContaining("timeout");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(8));
    }

    @Test
    void largeStderrDoesNotBlockStdoutCapture() {
        String output = manager().runIn(directory, List.of("-c", "alias.noisy=!printf done; head -c 200000 /dev/zero >&2", "noisy"), Map.of(), 5);
        assertThat(output).isEqualTo("done");
    }
}
