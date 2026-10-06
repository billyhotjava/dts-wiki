package com.yuzhi.dts.wiki;
import com.yuzhi.dts.wiki.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class DtsWikiApp {
    public static void main(String[] args) {
        if (args.length > 0 && "--migrate".equals(args[0])) {
            com.yuzhi.dts.wiki.migration.WikiMigration.run(java.util.Arrays.copyOfRange(args, 1, args.length));
        } else {
            SpringApplication.run(DtsWikiApp.class, args);
        }
    }
}
