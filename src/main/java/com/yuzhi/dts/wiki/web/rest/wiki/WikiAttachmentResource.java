package com.yuzhi.dts.wiki.web.rest.wiki;

import com.yuzhi.dts.wiki.domain.Attachment;
import com.yuzhi.dts.wiki.service.wiki.AttachmentService;
import com.yuzhi.dts.wiki.service.wiki.BlobStore;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Attachments and images (Sprint-6 design 03 S4; feature F4).
 */
@RestController
@RequestMapping("/api/wiki")
public class WikiAttachmentResource {

    private final AttachmentService attachmentService;
    private final BlobStore blobStore;

    public WikiAttachmentResource(AttachmentService attachmentService, BlobStore blobStore) {
        this.attachmentService = attachmentService;
        this.blobStore = blobStore;
    }

    @PostMapping("/pages/{id}/attachments")
    public ResponseEntity<AttachmentService.AttachmentInfo> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        AttachmentService.AttachmentInfo info = attachmentService.upload(id, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(info);
    }

    @GetMapping("/pages/{id}/attachments")
    public List<AttachmentService.AttachmentInfo> list(@PathVariable Long id) {
        return attachmentService.list(id);
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long attachmentId) throws IOException {
        Attachment attachment = attachmentService.requireVisible(attachmentId);
        InputStream content = blobStore.load(attachment.getSha256());
        MediaType type = MediaTypeFactory.getMediaType(attachment.getFileName()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        boolean inline = attachment.getMimeType() != null && (attachment.getMimeType().startsWith("image/") || attachment.getMimeType().equals("application/pdf"));
        return ResponseEntity.ok()
            .contentType(type)
            .contentLength(attachment.getSize())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder(inline ? "inline" : "attachment").filename(attachment.getFileName()).build().toString())
            .body(new InputStreamResource(content));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long attachmentId) {
        attachmentService.delete(attachmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pages/{id}/raw/**")
    public ResponseEntity<InputStreamResource> raw(@PathVariable Long id, HttpServletRequest request) throws IOException {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String prefix = "/api/wiki/pages/" + id + "/raw/";
        String relative = path != null && path.startsWith(prefix) ? path.substring(prefix.length()) : "";
        Attachment attachment = attachmentService.resolveRaw(id, relative);
        InputStream content = blobStore.load(attachment.getSha256());
        MediaType type = MediaTypeFactory.getMediaType(attachment.getFileName()).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).contentLength(attachment.getSize()).body(new InputStreamResource(content));
    }
}
