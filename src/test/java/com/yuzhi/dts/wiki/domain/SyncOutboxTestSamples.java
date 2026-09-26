package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SyncOutboxTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static SyncOutbox getSyncOutboxSample1() {
        return new SyncOutbox().id(1L).actorLogin("actorLogin1").actorName("actorName1").actorEmail("actorEmail1").attempts(1);
    }

    public static SyncOutbox getSyncOutboxSample2() {
        return new SyncOutbox().id(2L).actorLogin("actorLogin2").actorName("actorName2").actorEmail("actorEmail2").attempts(2);
    }

    public static SyncOutbox getSyncOutboxRandomSampleGenerator() {
        return new SyncOutbox()
            .id(longCount.incrementAndGet())
            .actorLogin(UUID.randomUUID().toString())
            .actorName(UUID.randomUUID().toString())
            .actorEmail(UUID.randomUUID().toString())
            .attempts(intCount.incrementAndGet());
    }
}
