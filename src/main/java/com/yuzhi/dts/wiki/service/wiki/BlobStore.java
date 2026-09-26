package com.yuzhi.dts.wiki.service.wiki;

import java.io.IOException;
import java.io.InputStream;

/**
 * Content-addressed blob storage (Sprint-6 design 02 D12).
 */
public interface BlobStore {
    void store(String sha256, InputStream content) throws IOException;

    InputStream load(String sha256) throws IOException;

    boolean exists(String sha256);

    void delete(String sha256) throws IOException;
}
