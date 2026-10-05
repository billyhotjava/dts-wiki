package com.yuzhi.dts.wiki.service.wiki.sync;

import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Attachment;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.repository.AttachmentRepository;
import com.yuzhi.dts.wiki.service.wiki.BlobStore;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

/** Imports real Git blobs into the same storage used by native attachments. */
@Service
public class GitAttachmentImporter {
    private final GitRepoManager git;
    private final AttachmentRepository attachments;
    private final BlobStore blobs;
    private final WikiProperties properties;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    public GitAttachmentImporter(GitRepoManager git, AttachmentRepository attachments, BlobStore blobs, WikiProperties properties, com.fasterxml.jackson.databind.ObjectMapper json) {
        this.git = git;
        this.attachments = attachments;
        this.blobs = blobs;
        this.properties = properties;
        this.json = json;
    }

    public boolean ingest(Page owner, String revision, String path) {
        if (!isSupportedPath(path)) return false;
        String slug = owner.getSpace().getSlug();
        if (!git.isRegularFile(slug, revision, path)) { return false; }
        long size = Long.parseLong(git.run(slug, java.util.List.of("cat-file", "-s", revision + ":" + path), 30));
        if (size > properties.getMaxSyncFileSize()) { return false; }
        if (path.endsWith(".archify.json") && size > 2_000_000 || path.endsWith(".html") && size > 5_000_000) return false;
        byte[] content = git.fileBytesAt(slug, revision, path);
        try {
            if (path.endsWith(".archify.json")) {
                var source = json.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                    .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(content);
                if (source == null || !source.isObject()) return false;
            }
            String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            Attachment attachment = attachments.findLiveBySpaceAndGitPath(owner.getSpace().getId(), path).orElseGet(Attachment::new);
            if (sha.equals(attachment.getSha256())) { return true; }
            blobs.store(sha, new ByteArrayInputStream(content));
            attachment.setPage(owner);
            attachment.setGitPath(path);
            attachment.setFileName(path.substring(path.lastIndexOf('/') + 1));
            attachment.setMimeType(MediaTypeFactory.getMediaType(path).map(Object::toString).orElse("application/octet-stream"));
            attachment.setSize((long) content.length);
            attachment.setSha256(sha);
            if (attachment.getCreatedAt() == null) { attachment.setCreatedAt(Instant.now()); }
            attachments.save(attachment);
            return true;
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("wiki_diagram_source_invalid space={} path={}", slug, path);
            return false;
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot store a Git attachment", e);
        }
    }

    public static boolean isSupportedPath(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        return lower.matches(".*\\.(png|jpg|jpeg|gif|webp|pdf|pptx|docx|xlsx)$")
            || path.matches("(?:.*/)?diagrams/[A-Za-z0-9][A-Za-z0-9._-]*\\.(archify\\.json|html)");
    }

    public void delete(Long spaceId, String path) {
        attachments.findLiveBySpaceAndGitPath(spaceId, path).ifPresent(attachment -> attachment.setDeletedAt(Instant.now()));
    }

    public void reconcile(Page owner, java.util.List<String> currentPaths) {
        for (Attachment attachment : attachments.findByPageIdAndDeletedAtIsNull(owner.getId())) {
            if (attachment.getGitPath() != null && !currentPaths.contains(attachment.getGitPath())) { attachment.setDeletedAt(Instant.now()); }
        }
    }
}
