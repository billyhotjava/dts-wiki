package com.yuzhi.dts.wiki.service.wiki.content;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Loads {@code classpath:content-schemas/*.schema.json} at startup (design 10 S3.1).
 * json-schema-validator 3.x works on Jackson 3 ({@code tools.jackson}).
 */
@Component
public class ContentSchemaRegistry {

    private final Map<String, Schema> schemas = new ConcurrentHashMap<>();

    public ContentSchemaRegistry() {
        SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            for (Resource resource : resolver.getResources("classpath:content-schemas/*.schema.json")) {
                String name = resource.getFilename();
                if (name == null || !name.endsWith(".schema.json")) {
                    continue;
                }
                String type = name.substring(0, name.length() - ".schema.json".length());
                try (InputStream in = resource.getInputStream()) {
                    String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    schemas.put(type, registry.getSchema(json));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load content schemas", e);
        }
    }

    public boolean hasType(String type) {
        return type != null && schemas.containsKey(type);
    }

    public Set<String> types() {
        return schemas.keySet();
    }

    public Schema schema(String type) {
        return schemas.get(type);
    }
}
