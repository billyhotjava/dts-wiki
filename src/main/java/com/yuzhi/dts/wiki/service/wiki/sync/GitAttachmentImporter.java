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

    public GitAttachmentImporter(GitRepoManager git, AttachmentRepository attachments, BlobStore blobs, WikiProperties properties) {
        this.git = git;
        this.attachments = attachments;
        this.blobs = blobs;
        this.properties = properties;
    }

    public boolean ingest(Page owner, String revision, String path) {
        String slug = owner.getSpace().getSlug();
        if (!git.isRegularFile(slug, revision, path)) { return false; }
        long size = Long.parseLong(git.run(slug, java.util.List.of("cat-file", "-s", revision + ":" + path), 30));
        if (size > properties.getMaxSyncFileSize()) { return false; }
        byte[] content = git.fileBytesAt(slug, revision, path);
        try {
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
        } catch (IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot store a Git attachment", e);
        }
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
