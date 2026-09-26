package com.yuzhi.dts.wiki.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Wiki business settings (Sprint-6 design 03 S2), prefix {@code application.wiki}.
 * Container paths are overridden by environment in deploy/compose.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "application.wiki")
public class WikiProperties {

    /** BlobStore root (design 02 D12). */
    private String attachmentsDir = "/data/attachments";

    /** Max image upload, bytes (design 03 S2). */
    private long maxImageSize = 10L * 1024 * 1024;

    /** Max generic attachment upload, bytes (design 03 S2). */
    private long maxAttachmentSize = 50L * 1024 * 1024;

    public Path attachmentsPath() {
        return Path.of(attachmentsDir);
    }

    public String getAttachmentsDir() {
        return attachmentsDir;
    }

    public void setAttachmentsDir(String attachmentsDir) {
        this.attachmentsDir = attachmentsDir;
    }

    public long getMaxImageSize() {
        return maxImageSize;
    }

    public void setMaxImageSize(long maxImageSize) {
        this.maxImageSize = maxImageSize;
    }

    public long getMaxAttachmentSize() {
        return maxAttachmentSize;
    }

    public void setMaxAttachmentSize(long maxAttachmentSize) {
        this.maxAttachmentSize = maxAttachmentSize;
    }
}
