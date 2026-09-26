package com.yuzhi.dts.wiki.service.wiki.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * DTS-MD v1 content analysis (design 10 S3.1): frontmatter split, type detection,
 * JSON Schema validation, duplicate doc_id check, title/plain-text/headings extraction.
 *
 * <p>Modes: {@code STRICT} (web/agent saves: invalid throws {@link FrontmatterInvalidException});
 * {@code LENIENT} (git inbound, import: always stores, records valid=false + errors).
 */
@Service
public class ContentService {

    public enum Mode {
        STRICT,
        LENIENT
    }

    private static final Set<String> KNOWN_TYPES = Set.of("sprint", "feature", "task", "adr", "evidence", "page");

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    private final ContentSchemaRegistry schemas;
    private final ObjectMapper objectMapper;

    public ContentService(ContentSchemaRegistry schemas, ObjectMapper objectMapper) {
        this.schemas = schemas;
        this.objectMapper = objectMapper;
    }

    public ContentAnalysis analyze(String markdown, Mode mode) {
        return analyze(markdown, mode, null);
    }

    /**
     * @param duplicateChecker given a docId, returns the gitPath of another page in the same
     * space already using it, or empty.
     */
    public ContentAnalysis analyze(String markdown, Mode mode, java.util.function.Function<String, java.util.Optional<String>> duplicateChecker) {
        FrontmatterParser.Parsed parsed = FrontmatterParser.parse(markdown);
        Map<String, Object> frontmatter = parsed.data();
        String body = parsed.split().body();
        List<ContentAnalysis.FieldError> errors = new ArrayList<>();
        if (parsed.yamlError() != null) {
            errors.add(new ContentAnalysis.FieldError("$", parsed.yamlError()));
        }

        String rawType = string(frontmatter.get("type"));
        String docType = rawType == null || !KNOWN_TYPES.contains(rawType) ? "page" : rawType;
        // 09 S3.3: unknown types are treated as page WITHOUT validation.
        if (parsed.split().present() && !frontmatter.isEmpty() && (rawType == null || KNOWN_TYPES.contains(rawType)) && schemas.hasType(docType)) {
            Schema schema = schemas.schema(docType);
            JsonNode input;
            try {
                input = JSON_MAPPER.readTree(objectMapper.writeValueAsString(frontmatter));
            } catch (Exception e) {
                throw new IllegalStateException("Frontmatter JSON conversion failed", e);
            }
            for (Error error : schema.validate(input)) {
                errors.add(new ContentAnalysis.FieldError(error.getInstanceLocation().toString(), translate(error)));
            }
        }

        String docId = string(frontmatter.get("id"));
        if (docId != null && duplicateChecker != null) {
            duplicateChecker.apply(docId).ifPresent(other -> errors.add(new ContentAnalysis.FieldError("id", "与 " + other + " 重复")));
        }

        MarkdownText.DocText text = MarkdownText.analyze(body);
        String title = firstNonBlank(string(frontmatter.get("title")), text.headings().isEmpty() ? null : text.headings().get(0), null);

        boolean valid = errors.isEmpty();
        ContentAnalysis analysis = new ContentAnalysis(
            frontmatter,
            body,
            docType,
            docId,
            string(frontmatter.get("status")),
            title,
            string(frontmatter.get("owner")),
            strings(frontmatter.get("tags")),
            strings(frontmatter.get("depends")),
            strings(frontmatter.get("related")),
            string(frontmatter.get("sprint")),
            string(frontmatter.get("feature")),
            string(frontmatter.get("priority")),
            valid,
            List.copyOf(errors),
            text.plainText(),
            text.headings()
        );
        if (!valid && mode == Mode.STRICT) {
            throw new FrontmatterInvalidException(analysis.errors());
        }
        return analysis;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private static List<String> strings(Object value) {
        if (value instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }
            }
            return out;
        }
        if (value instanceof String single && !single.isBlank()) {
            return List.of(single);
        }
        return List.of();
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    /** Translate json-schema-validator keywords to Chinese field messages. */
    static String translate(Error error) {
        String keyword = error.getKeyword() == null ? "" : error.getKeyword();
        String message = error.getMessage() == null ? "" : error.getMessage();
        return switch (keyword) {
            case "required" -> "缺少必填字段" + message;
            case "enum", "const" -> "取值不在允许范围" + message;
            case "pattern" -> "格式不符合要求" + message;
            case "type" -> "类型错误" + message;
            case "minLength" -> "太短" + message;
            case "minItems" -> "至少需要一项";
            case "format" -> "日期格式应为 YYYY-MM-DD";
            default -> message;
        };
    }
}
