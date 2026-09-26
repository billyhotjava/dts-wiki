package com.yuzhi.dts.wiki.service.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pure unit tests for content-addressed storage (no Spring). */
class FileBlobStoreTest {

    @TempDir
    Path dir;

    private static final String SHA = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"; // sha256("test")

    @Test
    void storeLoadExistsDelete() throws Exception {
        FileBlobStore store = new FileBlobStore(dir);
        byte[] bytes = "test".getBytes(StandardCharsets.UTF_8);
        assertThat(store.exists(SHA)).isFalse();
        store.store(SHA, new ByteArrayInputStream(bytes));
        assertThat(store.exists(SHA)).isTrue();
        // idempotent re-store
        store.store(SHA, new ByteArrayInputStream(bytes));
        try (var in = store.load(SHA)) {
            assertThat(in.readAllBytes()).isEqualTo(bytes);
        }
        store.delete(SHA);
        assertThat(store.exists(SHA)).isFalse();
    }

    @Test
    void rejectsBadSha() {
        FileBlobStore store = new FileBlobStore(dir);
        assertThatThrownBy(() -> store.store("../evil", new ByteArrayInputStream(new byte[0]))).isInstanceOf(IllegalArgumentException.class);
    }
}
