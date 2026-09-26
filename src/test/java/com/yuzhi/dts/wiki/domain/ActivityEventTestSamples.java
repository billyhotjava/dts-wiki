package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class ActivityEventTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static ActivityEvent getActivityEventSample1() {
        return new ActivityEvent().id(1L).actorName("actorName1").targetTitle("targetTitle1");
    }

    public static ActivityEvent getActivityEventSample2() {
        return new ActivityEvent().id(2L).actorName("actorName2").targetTitle("targetTitle2");
    }

    public static ActivityEvent getActivityEventRandomSampleGenerator() {
        return new ActivityEvent()
            .id(longCount.incrementAndGet())
            .actorName(UUID.randomUUID().toString())
            .targetTitle(UUID.randomUUID().toString());
    }
}
