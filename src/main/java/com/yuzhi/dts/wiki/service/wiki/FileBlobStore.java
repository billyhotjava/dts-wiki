package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.config.WikiProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Content-addressed blob storage (Sprint-6 design 02 D12).
 * Files live at {@code <root>/<sha256>}; rows in {@code attachment} reference them.
 * A later change of {@link com.yuzhi.dts.wiki.config.WikiProperties} to S3 only
 * replaces this implementation (callers are unaffected).
 */
public class FileBlobStore implements BlobStore {

    private final Path root;

    public FileBlobStore(Path root) {
        this.root = root;
    }

    @Override
    public void store(String sha256, InputStream content) throws IOException {
        Files.createDirectories(root);
        Path target = path(sha256);
        if (Files.exists(target)) {
            return; // dedupe: identical bytes already stored
        }
        Path tmp = Files.createTempFile(root, "upload-", ".tmp");
        try (InputStream in = content) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(tmp);
            throw e;
        }
        try {
            Files.move(tmp, target);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            if (!Files.exists(target)) {
                throw e;
            }
        }
    }

    @Override
    public InputStream load(String sha256) throws IOException {
        return Files.newInputStream(path(sha256));
    }

    @Override
    public boolean exists(String sha256) {
        return Files.exists(path(sha256));
    }

    @Override
    public void delete(String sha256) throws IOException {
        Files.deleteIfExists(path(sha256));
    }

    private Path path(String sha256) {
        if (sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invalid sha256");
        }
        return root.resolve(sha256);
    }
}
