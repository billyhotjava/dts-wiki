package com.yuzhi.dts.wiki.service.wiki.content;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

/**
 * YAML frontmatter split + parse (rule shared by backend and frontend, design 10 S3.1):
 * a file starts with {@code ---\n} (or {@code ---\r\n}); frontmatter ends at the next
 * line that is exactly {@code ---}. No match means no frontmatter.
 */
public final class FrontmatterParser {

    private static final Pattern SPLIT = Pattern.compile("\\A---\\r?\\n(.*?)\\r?\\n---(\\r?\\n|[\\r\\n]?)(.*)\\z", Pattern.DOTALL);

    private FrontmatterParser() {}

    public record Split(String frontmatter, String body, boolean present) {}

    public record Parsed(Split split, Map<String, Object> data, String yamlError) {}

    public static Split split(String markdown) {
        if (markdown == null) {
            return new Split(null, "", false);
        }
        Matcher matcher = SPLIT.matcher(markdown);
        if (!matcher.matches()) {
            return new Split(null, markdown, false);
        }
        return new Split(matcher.group(1), matcher.group(3), true);
    }

    @SuppressWarnings("unchecked")
    public static Parsed parse(String markdown) {
        Split split = split(markdown);
        if (!split.present()) {
            return new Parsed(split, Map.of(), null);
        }
        try {
            Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
            Object loaded = yaml.load(split.frontmatter());
            Map<String, Object> data = loaded instanceof Map ? normalizeDates((Map<String, Object>) loaded) : Map.of();
            return new Parsed(split, data, null);
        } catch (Exception e) {
            return new Parsed(split, Map.of(), "YAML 语法错误: " + firstLine(e.getMessage()));
        }
    }

    /** SnakeYAML resolves unquoted dates to java.util.Date; schemas expect YYYY-MM-DD strings. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> normalizeDates(Map<String, Object> data) {
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof java.util.Date date) {
                out.put(entry.getKey(), new java.text.SimpleDateFormat("yyyy-MM-dd").format(date));
            } else if (value instanceof Map<?, ?> nested) {
                out.put(entry.getKey(), normalizeDates((Map<String, Object>) nested));
            } else if (value instanceof java.util.List<?> list) {
                out.put(entry.getKey(), list.stream().map(item -> item instanceof java.util.Date d
                    ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(d)
                    : item).toList());
            } else {
                out.put(entry.getKey(), value);
            }
        }
        return out;
    }

    private static String firstLine(String message) {
        if (message == null) {
            return "";
        }
        int newline = message.indexOf('\n');
        return newline < 0 ? message : message.substring(0, newline);
    }
}
