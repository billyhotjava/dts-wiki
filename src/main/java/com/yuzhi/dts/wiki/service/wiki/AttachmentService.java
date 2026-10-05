package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Attachment;
import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxOp;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.repository.AttachmentRepository;
import com.yuzhi.dts.wiki.repository.PageRepository;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Attachments and images (Sprint-6 design 02 D12, 03 S4; feature F4).
 * Blobs are content-addressed by sha256 ({@link BlobStore}); rows carry the display
 * name and, for GIT pages, the repository path. Markdown reference form:
 * {@code ![name](./assets/<file>)} for images, {@code [name](./assets/<file>)} otherwise.
 */
@Service
public class AttachmentService {

    private static final Set<String> IMAGE_MIMES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp");
    private static final Set<String> ALLOWED_MIMES = Set.of(
        "image/png",
        "image/jpeg",
        "image/gif",
        "image/webp",
        "image/bmp",
        "application/pdf",
        "text/plain",
        "text/markdown",
        "application/json",
        "text/html",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Shanghai"));

    private final AttachmentRepository attachmentRepository;
    private final PageRepository pageRepository;
    private final PageService pageService;
    private final SpaceAccessService spaceAccessService;
    private final BlobStore blobStore;
    private final WikiProperties properties;
    private final PageWritePolicy writePolicy;

    public AttachmentService(
        AttachmentRepository attachmentRepository,
        PageRepository pageRepository,
        PageService pageService,
        SpaceAccessService spaceAccessService,
        BlobStore blobStore,
        WikiProperties properties,
        PageWritePolicy writePolicy
    ) {
        this.attachmentRepository = attachmentRepository;
        this.pageRepository = pageRepository;
        this.pageService = pageService;
        this.spaceAccessService = spaceAccessService;
        this.blobStore = blobStore;
        this.properties = properties;
        this.writePolicy = writePolicy;
    }

    public record AttachmentInfo(Long id, String fileName, String mimeType, long size, String url, String markdown) {}

    @Transactional
    public AttachmentInfo upload(Long pageId, String originalFilename, String contentType, byte[] bytes) {
        return uploadInternal(pageId, originalFilename, contentType, bytes, false);
    }

    /** A diagram bundle needs stable, matching names for its source and derived files. */
    @Transactional
    public AttachmentInfo uploadDiagram(Long pageId, String fileName, String contentType, byte[] bytes) {
        if (!fileName.matches("[a-zA-Z0-9][a-zA-Z0-9_-]{0,79}\\.(archify\\.json|html|png)")) {
            throw new IllegalArgumentException("Invalid diagram artifact filename");
        }
        return uploadInternal(pageId, fileName, contentType, bytes, true);
    }

    private AttachmentInfo uploadInternal(Long pageId, String originalFilename, String contentType, byte[] bytes, boolean diagram) {
        Page page = (diagram ? pageRepository.findForUpdate(pageId) : pageRepository.findLive(pageId))
            .filter(p -> p.getDeletedAt() == null).orElseThrow(() -> new SpaceNotVisibleException("page:" + pageId));
        spaceAccessService.requireWrite(page);
        writePolicy.requireWritable(page);
        String mime = contentType == null ? "application/octet-stream" : contentType.toLowerCase(Locale.ROOT).split(";")[0].trim();
        if (!ALLOWED_MIMES.contains(mime) || extensionOf(originalFilename).equals("svg")) {
            throw new IllegalArgumentException("Unsupported file type: " + mime);
        }
        boolean image = IMAGE_MIMES.contains(mime);
        long limit = image ? properties.getMaxImageSize() : properties.getMaxAttachmentSize();
        if (mime.equals("application/json")) limit = Math.min(limit, 2_000_000);
        if (mime.equals("text/html")) limit = Math.min(limit, 5_000_000);
        if (bytes.length > limit) {
            throw new IllegalArgumentException("File too large: " + bytes.length + " > " + limit);
        }
        String sha = sha256(bytes);
        try {
            blobStore.store(sha, new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new IllegalStateException("Blob store failed", e);
        }
        String storedName = image && !diagram ? stampedName(originalFilename) : sanitizeName(originalFilename);
        Attachment attachment = diagram ? attachmentRepository.findTopByPageIdAndFileNameAndDeletedAtIsNullOrderByIdDesc(pageId, storedName)
            .orElseGet(Attachment::new) : new Attachment();
        attachment.setPage(page);
        attachment.setFileName(storedName);
        attachment.setMimeType(mime);
        attachment.setSize((long) bytes.length);
        attachment.setSha256(sha);
        attachment.setCreatedAt(Instant.now());
        if (page.getKind() == PageKind.GIT) {
            attachment.setGitPath(pageService.pageDir(page) + "/assets/" + storedName);
        }
        attachment = attachmentRepository.save(attachment);
        // GIT sync (outbox consumed by F5); NATIVE stays LOCAL_ONLY.
        if (page.getKind() == PageKind.GIT) {
            pageService.markGitOutbox(page, OutboxOp.ATTACH, Map.of("gitPath", attachment.getGitPath(), "attachmentId", String.valueOf(attachment.getId())));
        }
        String markdown = image
            ? "![" + safeAlt(originalFilename) + "](./assets/" + storedName + ")"
            : "[" + safeAlt(originalFilename) + "](./assets/" + storedName + ")";
        return new AttachmentInfo(attachment.getId(), storedName, mime, bytes.length, "/api/wiki/attachments/" + attachment.getId(), markdown);
    }

    @Transactional(readOnly = true)
    public List<AttachmentInfo> list(Long pageId) {
        Page page = pageRepository.findLive(pageId).orElseThrow(() -> new SpaceNotVisibleException("page:" + pageId));
        spaceAccessService.requireRead(page);
        if (page.getGitPath() != null) {
            java.nio.file.Path base = java.nio.file.Path.of(page.getGitPath());
            if (page.getKind() != PageKind.FOLDER) { base = base.getParent(); }
            final java.nio.file.Path directory = base == null ? java.nio.file.Path.of("") : base;
            String prefix = directory.toString().isEmpty() ? "" : directory + "/";
            return attachmentRepository.findLiveGitBySpace(page.getSpace().getId()).stream()
                .filter(a -> a.getGitPath().startsWith(prefix))
                .map(a -> {
                    String relative = "./" + directory.relativize(java.nio.file.Path.of(a.getGitPath())).toString();
                    boolean image = IMAGE_MIMES.contains(a.getMimeType());
                    String reference = (image ? "!" : "") + "[" + safeAlt(a.getFileName()) + "](<" + relative + ">)";
                    return new AttachmentInfo(a.getId(), a.getFileName(), a.getMimeType(), a.getSize(), "/api/wiki/attachments/" + a.getId(), reference);
                }).toList();
        }
        return attachmentRepository
            .findByPageIdAndDeletedAtIsNull(pageId)
            .stream()
            .map(a -> toInfo(a, page))
            .toList();
    }

    @Transactional(readOnly = true)
    public Attachment requireVisible(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId).orElseThrow(() -> new SpaceNotVisibleException("attachment:" + attachmentId));
        if (attachment.getDeletedAt() != null || attachment.getPage() == null || attachment.getPage().getDeletedAt() != null) {
            throw new SpaceNotVisibleException("attachment:" + attachmentId);
        }
        spaceAccessService.requireRead(attachment.getPage());
        return attachment;
    }

    @Transactional
    public void delete(Long attachmentId) {
        Attachment attachment = requireVisible(attachmentId);
        spaceAccessService.requireWrite(attachment.getPage());
        writePolicy.requireWritable(attachment.getPage());
        attachment.setDeletedAt(Instant.now());
        attachmentRepository.save(attachment);
        // Retain blobs for backup/history; soft deletion must not destroy bytes before commit.
    }

    /** Editor preview: {@code ./assets/<file>} relative to the page (design 03 S4). */
    @Transactional(readOnly = true)
    public Attachment resolveRaw(Long pageId, String relativePath) {
        Page page = pageRepository.findLive(pageId).orElseThrow(() -> new SpaceNotVisibleException("page:" + pageId));
        spaceAccessService.requireRead(page);
        String name = relativePath == null ? "" : relativePath.replace("\\", "/");
        if (page.getGitPath() != null) {
            if (name.startsWith("/") || name.chars().anyMatch(Character::isISOControl)) {
                throw new SpaceNotVisibleException("asset");
            }
            java.nio.file.Path base = java.nio.file.Path.of(page.getGitPath());
            if (page.getKind() != PageKind.FOLDER) { base = base.getParent(); }
            String gitPath = (base == null ? java.nio.file.Path.of(name) : base.resolve(name)).normalize().toString().replace("\\", "/");
            if (gitPath.startsWith("../")) { throw new SpaceNotVisibleException("asset"); }
            return attachmentRepository.findLiveBySpaceAndGitPath(page.getSpace().getId(), gitPath)
                .orElseThrow(() -> new SpaceNotVisibleException("asset"));
        }
        if (name.startsWith("./")) {
            name = name.substring(2);
        }
        if (name.startsWith("assets/")) {
            name = name.substring("assets/".length());
        } else if (name.startsWith("diagrams/")) {
            name = name.substring("diagrams/".length());
        }
        final String file = name;
        return attachmentRepository.findTopByPageIdAndFileNameAndDeletedAtIsNullOrderByIdDesc(pageId, file)
            .orElseThrow(() -> new SpaceNotVisibleException("asset:" + relativePath));
    }

    private AttachmentInfo toInfo(Attachment attachment, Page page) {
        boolean image = IMAGE_MIMES.contains(attachment.getMimeType());
        String markdown = image
            ? "![" + attachment.getFileName() + "](./assets/" + attachment.getFileName() + ")"
            : "[" + attachment.getFileName() + "](./assets/" + attachment.getFileName() + ")";
        return new AttachmentInfo(
            attachment.getId(),
            attachment.getFileName(),
            attachment.getMimeType(),
            attachment.getSize(),
            "/api/wiki/attachments/" + attachment.getId(),
            markdown
        );
    }

    static String stampedName(String originalFilename) {
        String ext = extensionOf(originalFilename);
        String hex = HexFormat.of().formatHex(new byte[] { (byte) (Math.random() * 256), (byte) (Math.random() * 256), (byte) (Math.random() * 256) });
        return STAMP.format(Instant.now()) + "-" + hex + (ext.isEmpty() ? ".bin" : "." + ext);
    }

    static String sanitizeName(String originalFilename) {
        String base = originalFilename == null ? "file" : originalFilename.replace("\\", "/");
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "-").strip();
        if (base.isEmpty()) {
            base = "file";
        }
        return base.length() > 120 ? base.substring(0, 120) : base;
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        String base = filename.replace("\\", "/");
        base = base.substring(base.lastIndexOf('/') + 1);
        int dot = base.lastIndexOf('.');
        if (dot < 0 || dot == base.length() - 1) {
            return "";
        }
        String ext = base.substring(dot + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        return ext.length() > 10 ? "" : ext;
    }

    private static String safeAlt(String filename) {
        String base = sanitizeName(filename);
        int dot = base.lastIndexOf('.');
        return dot < 0 ? base : base.substring(0, dot);
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
