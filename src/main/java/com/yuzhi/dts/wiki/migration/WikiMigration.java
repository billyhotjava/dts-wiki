package com.yuzhi.dts.wiki.migration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Apply the existing changelog without starting HTTP, identity clients or content workers. */
public final class WikiMigration {
    private WikiMigration() {}

    public static void run(String[] args) {
        SpringApplication application = new SpringApplication(MigrationConfiguration.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setAdditionalProfiles("wiki-migration");
        try (var context = application.run(args)) {
            if (!context.getEnvironment().getProperty("spring.liquibase.enabled", Boolean.class, true)) {
                throw new IllegalStateException("Migration mode requires Liquibase to be enabled");
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Profile("wiki-migration")
    @ImportAutoConfiguration({ DataSourceAutoConfiguration.class, LiquibaseAutoConfiguration.class })
    static class MigrationConfiguration {}
}
