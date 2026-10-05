package com.yuzhi.dts.wiki.service.wiki;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Resolve externally configured legacy aliases through the current space permissions. */
@Service
public class LegacyLinkService {
    private final PageService pages;
    private final Map<String, Alias> aliases;

    public LegacyLinkService(PageService pages, ObjectMapper json,
        @Value("${application.wiki.legacy.aliases-json:{}}") String configuration) throws java.io.IOException {
        this.pages = pages;
        Map<String, Alias> parsed = json.readValue(configuration.isBlank() ? "{}" : configuration, new TypeReference<>() {});
        parsed.forEach((name, alias) -> {
            if (!name.matches("[a-zA-Z0-9][a-zA-Z0-9_-]{0,63}") || alias == null || alias.spaceSlug() == null
                || !alias.spaceSlug().matches("[a-z0-9][a-z0-9-]{0,31}") || !safePath(alias.rootPath())) {
                throw new IllegalArgumentException("Invalid legacy link configuration");
            }
        });
        this.aliases = Map.copyOf(parsed);
    }

    public Destination resolve(String legacySpace, String path) {
        Alias alias = aliases.get(legacySpace);
        if (alias == null || !safePath(path)) throw new SpaceNotVisibleException("legacy link");
        String target = alias.rootPath().isEmpty() ? path : alias.rootPath() + (path.isEmpty() ? "" : "/" + path);
        return new Destination(alias.spaceSlug(), pages.resolve(alias.spaceSlug(), target).pageId());
    }

    private static boolean safePath(String path) {
        if (path == null || path.startsWith("/") || path.endsWith("/") || path.contains("\\") || path.contains(":")
            || path.chars().anyMatch(Character::isISOControl)) return false;
        return java.util.Arrays.stream(path.split("/", -1)).allMatch(part -> !part.equals(".") && !part.equals("..")
            && (!part.isEmpty() || path.isEmpty()));
    }

    public record Alias(String spaceSlug, String rootPath) {}
    public record Destination(String spaceSlug, long pageId) {}
}
