package com.yuzhi.dts.wiki;

import com.yuzhi.dts.wiki.config.AsyncSyncConfiguration;
import com.yuzhi.dts.wiki.config.DatabaseTestcontainer;
import com.yuzhi.dts.wiki.config.JacksonConfiguration;
import com.yuzhi.dts.wiki.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        DtsWikiApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        TestSecurityConfiguration.class,
        com.yuzhi.dts.wiki.config.JacksonHibernateConfiguration.class,
    }
)
@ImportTestcontainers(DatabaseTestcontainer.class)
public @interface IntegrationTest {}
