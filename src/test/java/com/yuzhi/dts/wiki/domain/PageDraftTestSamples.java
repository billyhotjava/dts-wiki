package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PageDraftTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static PageDraft getPageDraftSample1() {
        return new PageDraft().id(1L).baseVersionNo(1);
    }

    public static PageDraft getPageDraftSample2() {
        return new PageDraft().id(2L).baseVersionNo(2);
    }

    public static PageDraft getPageDraftRandomSampleGenerator() {
        return new PageDraft().id(longCount.incrementAndGet()).baseVersionNo(intCount.incrementAndGet());
    }
}
