package com.yuzhi.dts.wiki.config;

import com.yuzhi.dts.wiki.service.wiki.BlobStore;
import com.yuzhi.dts.wiki.service.wiki.FileBlobStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** BlobStore wiring (Sprint-6 design 02 D12). */
@Configuration
public class BlobStoreConfiguration {

    @Bean
    public BlobStore blobStore(WikiProperties properties) {
        return new FileBlobStore(properties.attachmentsPath());
    }
}
