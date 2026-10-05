package com.yuzhi.dts.wiki.service.wiki.mcp;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.ArrayDeque;
import org.springframework.stereotype.Component;

/** Sliding one-minute write budget per personal subject on this application instance. */
@Component
public class McpWriteLimiter {
    private final Cache<String, ArrayDeque<Long>> writes = Caffeine.newBuilder().maximumSize(10_000).expireAfterAccess(Duration.ofMinutes(2)).build();
    public boolean allow(String subject) { return allow(subject, System.currentTimeMillis()); }
    boolean allow(String subject, long now) {
        ArrayDeque<Long> window = writes.get(subject, ignored -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && window.peekFirst() <= now - 60_000) window.removeFirst();
            if (window.size() >= 60) return false;
            window.addLast(now);
            return true;
        }
    }
}
