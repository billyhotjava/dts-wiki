package com.yuzhi.dts.wiki.service.wiki.content;

import java.util.List;
import java.util.Map;

/** Result of analyzing one Markdown document (design 10 S3.1). */
public record ContentAnalysis(
    Map<String, Object> frontmatter,
    String body,
    String docType,
    String docId,
    String status,
    String title,
    String owner,
    List<String> tags,
    List<String> depends,
    List<String> related,
    String sprint,
    String feature,
    String priority,
    boolean valid,
    List<FieldError> errors,
    String plainText,
    List<String> headings
) {
    public record FieldError(String path, String message) {}
}
