package com.yuzhi.dts.wiki.service.wiki;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Store caller-rendered diagram bundles; the server never executes renderer input. */
@Service
public class DiagramArtifactService {
    private final AttachmentService attachments;
    private final BlobStore blobs;
    private final ObjectMapper json;
    public DiagramArtifactService(AttachmentService attachments, BlobStore blobs, ObjectMapper json) {
        this.attachments = attachments; this.blobs = blobs; this.json = json;
    }
    @Transactional(readOnly = true)
    public Map<String, Object> get(long pageId, String path) throws java.io.IOException {
        var attachment = attachments.resolveRaw(pageId, path);
        if (!attachment.getFileName().endsWith(".json") || attachment.getSize() > 2_000_000) throw new IllegalArgumentException("Expected a bounded JSON diagram source");
        try (var stream = blobs.load(attachment.getSha256())) {
            var source = json.readTree(stream.readNBytes(2_000_001));
            if (!source.isObject()) throw new IllegalArgumentException("Diagram source must be a JSON object");
            return Map.of("pageId", pageId, "path", path, "source", json.convertValue(source, Map.class));
        }
    }
    @Transactional
    public List<AttachmentService.AttachmentInfo> put(long pageId, String name, String spec, String html, String pngBase64) throws java.io.IOException {
        if (!name.matches("[a-zA-Z0-9][a-zA-Z0-9_-]{0,79}")) throw new IllegalArgumentException("Invalid diagram name");
        if (spec.length() > 2_000_000 || html.length() > 5_000_000 || pngBase64.length() > 14_000_000) throw new IllegalArgumentException("Diagram bundle is too large");
        if (!json.readTree(spec).isObject()) throw new IllegalArgumentException("Diagram source must be a JSON object");
        byte[] png = Base64.getDecoder().decode(pngBase64);
        byte[] signature = new byte[] { (byte) 137, 80, 78, 71, 13, 10, 26, 10 };
        if (png.length < signature.length || !java.util.Arrays.equals(signature, java.util.Arrays.copyOf(png, signature.length))) throw new IllegalArgumentException("Invalid PNG artifact");
        return List.of(attachments.uploadDiagram(pageId, name + ".archify.json", "application/json", spec.getBytes(StandardCharsets.UTF_8)),
            attachments.uploadDiagram(pageId, name + ".html", "text/html", html.getBytes(StandardCharsets.UTF_8)),
            attachments.uploadDiagram(pageId, name + ".png", "image/png", png));
    }
}
