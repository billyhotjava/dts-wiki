package com.yuzhi.dts.wiki;
import com.yuzhi.dts.wiki.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class DtsWikiApp {
    public static void main(String[] args) { SpringApplication.run(DtsWikiApp.class, args); }
}
