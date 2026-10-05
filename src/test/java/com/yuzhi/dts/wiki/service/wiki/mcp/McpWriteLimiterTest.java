package com.yuzhi.dts.wiki.service.wiki.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class McpWriteLimiterTest {
    @Test
    void enforcesSlidingPersonalBudgetAndExpiresOldWrites() {
        var limiter = new McpWriteLimiter();
        for (int i = 0; i < 60; i++) assertThat(limiter.allow("alice", 100_000 + i)).isTrue();
        assertThat(limiter.allow("alice", 100_060)).isFalse();
        assertThat(limiter.allow("bob", 100_060)).isTrue();
        assertThat(limiter.allow("alice", 160_000)).isTrue();
        assertThat(limiter.allow("alice", 160_000)).isFalse();
    }
}
