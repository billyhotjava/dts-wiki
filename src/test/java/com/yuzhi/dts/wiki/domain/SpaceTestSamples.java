package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SpaceTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Space getSpaceSample1() {
        return new Space()
            .id(1L)
            .slug("slug1")
            .name("name1")
            .description("description1")
            .gitRepoUrl("gitRepoUrl1")
            .gitBranch("gitBranch1")
            .position(1);
    }

    public static Space getSpaceSample2() {
        return new Space()
            .id(2L)
            .slug("slug2")
            .name("name2")
            .description("description2")
            .gitRepoUrl("gitRepoUrl2")
            .gitBranch("gitBranch2")
            .position(2);
    }

    public static Space getSpaceRandomSampleGenerator() {
        return new Space()
            .id(longCount.incrementAndGet())
            .slug(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .description(UUID.randomUUID().toString())
            .gitRepoUrl(UUID.randomUUID().toString())
            .gitBranch(UUID.randomUUID().toString())
            .position(intCount.incrementAndGet());
    }
}
