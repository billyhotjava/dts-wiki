package com.yuzhi.dts.wiki.service.wiki.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pure unit tests for git merge-file wrapping (no Spring, no git state). */
class ConflictServiceTest {

    @BeforeEach
    void requireGit() {
        try {
            Process process = new ProcessBuilder("git", "--version").start();
            assumeTrue(process.waitFor() == 0, "git CLI required");
        } catch (Exception e) {
            assumeTrue(false, "git CLI required");
        }
    }

    @Test
    void cleanMergeOfDisjointEdits() throws Exception {
        String base = "line1\nline2\nline3\n";
        String wiki = "line1\nline2 wiki\nline3\n";
        String git = "line1\nline2\nline3 git\n";
        ConflictService.MergeResult result = ConflictService.merge(base, wiki, git);
        // line2 vs line3 edits are on different lines: still one file, merge-file handles hunks
        assertThat(result.merged()).contains("line1");
    }

    @Test
    void conflictingEditsProduceMarkers() throws Exception {
        String base = "same\n";
        ConflictService.MergeResult result = ConflictService.merge(base, "wiki change\n", "git change\n");
        assertThat(result.clean()).isFalse();
        assertThat(result.merged()).contains("<<<<<<<");
    }

    @Test
    void identicalInputsAreClean() throws Exception {
        ConflictService.MergeResult result = ConflictService.merge("a\n", "a\n", "a\n");
        assertThat(result.clean()).isTrue();
        assertThat(result.merged()).isEqualTo("a\n");
    }

}
