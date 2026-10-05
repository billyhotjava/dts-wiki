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

    private final Content content = new Content();
    private final Directory directory = new Directory();
    private final Notifications notifications = new Notifications();

    public Directory getDirectory() { return directory; }
    public Notifications getNotifications() { return notifications; }

    /** External, read-only identity API settings; recipient permissions are never cached. */
    public static class Directory {
        private String baseUrl = "";
        private String realm = "";
        private String wikiClientUuid = "";
        private String clientId = "";
        private String clientSecret = "";
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String value) { baseUrl = value; }
        public String getRealm() { return realm; }
        public void setRealm(String value) { realm = value; }
        public String getWikiClientUuid() { return wikiClientUuid; }
        public void setWikiClientUuid(String value) { wikiClientUuid = value; }
        public String getClientId() { return clientId; }
        public void setClientId(String value) { clientId = value; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String value) { clientSecret = value; }
    }

    public static class Notifications {
        private boolean mailEnabled;
        private String from = "";
        private String publicUrl = "";
        public boolean isMailEnabled() { return mailEnabled; }
        public void setMailEnabled(boolean value) { mailEnabled = value; }
        public String getFrom() { return from; }
        public void setFrom(String value) { from = value; }
        public String getPublicUrl() { return publicUrl; }
        public void setPublicUrl(String value) { publicUrl = value; }
    }

    /** Outbound writes require an explicit opt-in; content-managed spaces stay read-only. */
    private boolean outboundEnabled;

    public Content getContent() { return content; }
    public boolean isOutboundEnabled() { return outboundEnabled; }
    public void setOutboundEnabled(boolean value) { outboundEnabled = value; }

    public static class Content {
        private String repoUrl = "";
        private String branch = "main";
        private String manifestPath = "dts-worklog/spaces.yml";
        private String deployKeyPath = "/data/secrets/content.key";

        public String getRepoUrl() { return repoUrl; }
        public void setRepoUrl(String value) { repoUrl = value; }
        public String getBranch() { return branch; }
        public void setBranch(String value) { branch = value; }
        public String getManifestPath() { return manifestPath; }
        public void setManifestPath(String value) { manifestPath = value; }
        public String getDeployKeyPath() { return deployKeyPath; }
        public void setDeployKeyPath(String value) { deployKeyPath = value; }
    }

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
