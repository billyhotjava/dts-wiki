package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PageTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static Page getPageSample1() {
        return new Page().id(1L).title("title1").gitPath("gitPath1").position(1);
    }

    public static Page getPageSample2() {
        return new Page().id(2L).title("title2").gitPath("gitPath2").position(2);
    }

    public static Page getPageRandomSampleGenerator() {
        return new Page()
            .id(longCount.incrementAndGet())
            .title(UUID.randomUUID().toString())
            .gitPath(UUID.randomUUID().toString())
            .position(intCount.incrementAndGet());
    }
}
