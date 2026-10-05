package com.yuzhi.dts.wiki.config;

import com.github.benmanes.caffeine.jcache.configuration.CaffeineConfiguration;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.yuzhi.dts.wiki.config.ApplicationProperties;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    public CacheConfiguration(ApplicationProperties applicationProperties) {
        ApplicationProperties.Cache.Caffeine caffeine = applicationProperties.getCache().getCaffeine();

        CaffeineConfiguration<Object, Object> caffeineConfiguration = new CaffeineConfiguration<>();
        caffeineConfiguration.setMaximumSize(OptionalLong.of(caffeine.getMaxEntries()));
        caffeineConfiguration.setExpireAfterWrite(OptionalLong.of(TimeUnit.SECONDS.toNanos(caffeine.getTimeToLiveSeconds())));
        caffeineConfiguration.setStatisticsEnabled(true);
        jcacheConfiguration = caffeineConfiguration;
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cacheManager) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            createCache(cm, com.yuzhi.dts.wiki.repository.UserRepository.USERS_BY_LOGIN_CACHE);
            createCache(cm, com.yuzhi.dts.wiki.repository.UserRepository.USERS_BY_EMAIL_CACHE);
            createCache(cm, com.yuzhi.dts.wiki.domain.User.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Authority.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.User.class.getName() + ".authorities");
            createCache(cm, com.yuzhi.dts.wiki.domain.Space.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Space.class.getName() + ".syncRootses");
            createCache(cm, com.yuzhi.dts.wiki.domain.Space.class.getName() + ".pageses");
            createCache(cm, com.yuzhi.dts.wiki.domain.SyncRoot.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.SyncState.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Page.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Page.class.getName() + ".childrens");
            createCache(cm, com.yuzhi.dts.wiki.domain.Page.class.getName() + ".versionses");
            createCache(cm, com.yuzhi.dts.wiki.domain.Page.class.getName() + ".attachmentses");
            createCache(cm, com.yuzhi.dts.wiki.domain.Page.class.getName() + ".commentses");
            createCache(cm, com.yuzhi.dts.wiki.domain.Page.class.getName() + ".labelses");
            createCache(cm, com.yuzhi.dts.wiki.domain.PageVersion.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Attachment.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Comment.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Comment.class.getName() + ".replieses");
            createCache(cm, com.yuzhi.dts.wiki.domain.SyncConflict.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.SyncOutbox.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.PageDraft.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.PageWatch.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Notification.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.ActivityEvent.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Label.class.getName());
            createCache(cm, com.yuzhi.dts.wiki.domain.Label.class.getName() + ".pageses");
        };
    }

    private void createCache(javax.cache.CacheManager cm, String cacheName) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, jcacheConfiguration);
        }
    }
}
