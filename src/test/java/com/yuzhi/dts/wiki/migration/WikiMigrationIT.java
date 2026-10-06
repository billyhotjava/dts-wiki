package com.yuzhi.dts.wiki.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.DriverManager;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** A separate disposable database proves the non-web entry point without OIDC or application beans. */
@Testcontainers
class WikiMigrationIT {
    @Container
    static final PostgreSQLContainer DATABASE = new PostgreSQLContainer(
        DockerImageName.parse("dts-wiki-db:18-bigm").asCompatibleSubstituteFor("postgres"));

    private String[] arguments(boolean enabled) {
        return new String[] {
            "--spring.profiles.active=prod,kubernetes",
            "--spring.datasource.url=" + DATABASE.getJdbcUrl(),
            "--spring.datasource.username=" + DATABASE.getUsername(),
            "--spring.datasource.password=" + DATABASE.getPassword(),
            "--spring.liquibase.contexts=prod",
            "--spring.liquibase.enabled=" + enabled,
            "--spring.security.oauth2.client.registration.oidc.client-id=",
        };
    }

    @Test
    void freshAndRepeatedMigrationRequireOnlyPostgres() throws Exception {
        WikiMigration.run(arguments(true));
        try (var connection = DriverManager.getConnection(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
             var statement = connection.createStatement()) {
            var initial = statement.executeQuery("SELECT count(*) FROM databasechangelog");
            initial.next();
            int count = initial.getInt(1);
            assertThat(count).isPositive();
            WikiMigration.run(arguments(true));
            var repeated = statement.executeQuery("SELECT count(*) FROM databasechangelog");
            repeated.next();
            assertThat(repeated.getInt(1)).isEqualTo(count);
            var extension = statement.executeQuery("SELECT extname FROM pg_extension WHERE extname='pg_bigm'");
            assertThat(extension.next()).isTrue();
        }
    }

    @Test
    void disabledMigrationCannotReportSuccess() {
        assertThatThrownBy(() -> WikiMigration.run(arguments(false)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Migration mode requires Liquibase to be enabled");
    }
}
