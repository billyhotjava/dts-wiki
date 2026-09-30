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

    /** Local git working copies root, one dir per space slug (design 04 S2). */
    private String reposDir = "/data/repos";

    /** Per-repo SSH deploy keys: {@code <space-slug>.key} (design 04 S2). */
    private String sshKeysDir = "/data/secrets";

    /** Outbound/inbound poll interval seconds (design 03 S6, default 30 s). */
    private long syncIntervalSeconds = 30;

    /** git committer identity for wiki-originated commits (design 04 S2). */
    private String committerName = "DTS Wiki";

    /** git committer identity for wiki-originated commits (design 04 S2). */
    private String committerEmail = "wiki@yuzhicloud.com";

    /** Files larger than this are never synced into git (design 04 S5, default 20 MB). */
    private long maxSyncFileSize = 20L * 1024 * 1024;

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

    public java.nio.file.Path reposPath() {
        return java.nio.file.Path.of(reposDir);
    }

    public String getReposDir() {
        return reposDir;
    }

    public void setReposDir(String reposDir) {
        this.reposDir = reposDir;
    }

    public java.nio.file.Path sshKeysPath() {
        return java.nio.file.Path.of(sshKeysDir);
    }

    public String getSshKeysDir() {
        return sshKeysDir;
    }

    public void setSshKeysDir(String sshKeysDir) {
        this.sshKeysDir = sshKeysDir;
    }

    public long getSyncIntervalSeconds() {
        return syncIntervalSeconds;
    }

    public void setSyncIntervalSeconds(long syncIntervalSeconds) {
        this.syncIntervalSeconds = syncIntervalSeconds;
    }

    public String getCommitterName() {
        return committerName;
    }

    public void setCommitterName(String committerName) {
        this.committerName = committerName;
    }

    public String getCommitterEmail() {
        return committerEmail;
    }

    public void setCommitterEmail(String committerEmail) {
        this.committerEmail = committerEmail;
    }

    public long getMaxSyncFileSize() {
        return maxSyncFileSize;
    }

    public void setMaxSyncFileSize(long maxSyncFileSize) {
        this.maxSyncFileSize = maxSyncFileSize;
    }
}
