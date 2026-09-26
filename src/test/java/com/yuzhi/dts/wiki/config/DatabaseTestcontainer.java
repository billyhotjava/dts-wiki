package com.yuzhi.dts.wiki.config;

import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public interface DatabaseTestcontainer {
    // DTS-WIKI: customized (Sprint-6 design 07 S1): tests run against the real
    // dts-wiki-db image so custom changelogs (pg_bigm extension, gin_bigm_ops
    // indexes) are validated too. Build it with:
    //   docker build -f src/main/docker/postgres.Dockerfile -t dts-wiki-db:18-bigm .
    @Container
    PostgreSQLContainer databaseContainer = new PostgreSQLContainer(
        DockerImageName.parse("dts-wiki-db:18-bigm").asCompatibleSubstituteFor("postgres")
    )
        .withDatabaseName("dtsWiki")

        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(DatabaseTestcontainer.class)))
        .withReuse(true);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", databaseContainer::getJdbcUrl);
        registry.add("spring.datasource.username", databaseContainer::getUsername);
        registry.add("spring.datasource.password", databaseContainer::getPassword);
    }
}
