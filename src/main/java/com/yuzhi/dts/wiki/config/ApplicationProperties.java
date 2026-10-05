package com.yuzhi.dts.wiki.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

/** Company-owned infrastructure settings; business settings live in WikiProperties. */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = true)
public class ApplicationProperties {
    private final CorsConfiguration cors = new CorsConfiguration();
    private final Cache cache = new Cache();
    private final Security security = new Security();
    public CorsConfiguration getCors() { return cors; }
    public Cache getCache() { return cache; }
    public Security getSecurity() { return security; }
    public static class Cache {
        private final Caffeine caffeine = new Caffeine();
        public Caffeine getCaffeine() { return caffeine; }
        public static class Caffeine {
            private long maxEntries = 1000;
            private long timeToLiveSeconds = 3600;
            public long getMaxEntries() { return maxEntries; }
            public void setMaxEntries(long value) { maxEntries = value; }
            public long getTimeToLiveSeconds() { return timeToLiveSeconds; }
            public void setTimeToLiveSeconds(long value) { timeToLiveSeconds = value; }
        }
    }
    public static class Security {
        private final Oauth2 oauth2 = new Oauth2();
        public Oauth2 getOauth2() { return oauth2; }
        public static class Oauth2 {
            private List<String> audience = new ArrayList<>();
            public List<String> getAudience() { return audience; }
            public void setAudience(List<String> value) { audience = value; }
        }
    }
}
