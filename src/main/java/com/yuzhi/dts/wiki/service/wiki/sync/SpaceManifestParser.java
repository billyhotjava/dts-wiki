package com.yuzhi.dts.wiki.service.wiki.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Validates the complete inventory against the pinned wiki-content v1 contract. */
@Component
public class SpaceManifestParser {

    private final Schema schema;
    private final ObjectMapper mapper = new ObjectMapper();

    public SpaceManifestParser() {
        try (var input = new ClassPathResource("protocol/wiki-content/space-manifest.v1.schema.json").getInputStream()) {
            schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                .getSchema(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load the pinned space-manifest v1 schema", e);
        }
    }

    public record Manifest(int version, List<Entry> spaces) {
        public Manifest { spaces = List.copyOf(spaces); }
    }

    public record Entry(String slug, String name, String description, List<String> roots, String role) {
        public Entry { roots = List.copyOf(roots); }
    }

    public Manifest parse(String source) {
        try {
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setMaxAliasesForCollections(0);
            options.setCodePointLimit(1_000_000);
            options.setNestingDepthLimit(30);
            Object data = new Yaml(new SafeConstructor(options)).load(source);
            String json = mapper.writeValueAsString(data);
            var errors = schema.validate(tools.jackson.databind.json.JsonMapper.builder().build().readTree(json));
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("Space manifest violates wiki-content v1: " + errors);
            }
            Manifest manifest = mapper.readValue(json, Manifest.class);
            var slugs = new HashSet<String>();
            var roots = new java.util.ArrayList<String>();
            for (Entry entry : manifest.spaces()) {
                if (!slugs.add(entry.slug())) {
                    throw new IllegalArgumentException("Duplicate space slug: " + entry.slug());
                }
                for (String root : entry.roots()) {
                    if (java.util.Arrays.stream(root.split("/")).anyMatch(p -> p.equals(".") || p.equals(".."))) {
                        throw new IllegalArgumentException("Unsafe content root: " + root);
                    }
                    if (roots.stream().anyMatch(p -> p.equals(root) || p.startsWith(root + "/") || root.startsWith(p + "/"))) {
                        throw new IllegalArgumentException("Overlapping content root: " + root);
                    }
                    roots.add(root);
                }
            }
            return manifest;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid space manifest", e);
        }
    }
}
