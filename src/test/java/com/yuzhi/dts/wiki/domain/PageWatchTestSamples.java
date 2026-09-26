package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class PageWatchTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static PageWatch getPageWatchSample1() {
        return new PageWatch().id(1L);
    }

    public static PageWatch getPageWatchSample2() {
        return new PageWatch().id(2L);
    }

    public static PageWatch getPageWatchRandomSampleGenerator() {
        return new PageWatch().id(longCount.incrementAndGet());
    }
}
