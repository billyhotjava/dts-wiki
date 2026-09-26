package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PageVersionTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static PageVersion getPageVersionSample1() {
        return new PageVersion()
            .id(1L)
            .versionNo(1)
            .contentSha256("contentSha2561")
            .authorName("authorName1")
            .authorEmail("authorEmail1")
            .gitCommit("gitCommit1")
            .message("message1");
    }

    public static PageVersion getPageVersionSample2() {
        return new PageVersion()
            .id(2L)
            .versionNo(2)
            .contentSha256("contentSha2562")
            .authorName("authorName2")
            .authorEmail("authorEmail2")
            .gitCommit("gitCommit2")
            .message("message2");
    }

    public static PageVersion getPageVersionRandomSampleGenerator() {
        return new PageVersion()
            .id(longCount.incrementAndGet())
            .versionNo(intCount.incrementAndGet())
            .contentSha256(UUID.randomUUID().toString())
            .authorName(UUID.randomUUID().toString())
            .authorEmail(UUID.randomUUID().toString())
            .gitCommit(UUID.randomUUID().toString())
            .message(UUID.randomUUID().toString());
    }
}
