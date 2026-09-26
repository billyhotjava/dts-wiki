package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SyncConflictTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static SyncConflict getSyncConflictSample1() {
        return new SyncConflict().id(1L).gitCommit("gitCommit1");
    }

    public static SyncConflict getSyncConflictSample2() {
        return new SyncConflict().id(2L).gitCommit("gitCommit2");
    }

    public static SyncConflict getSyncConflictRandomSampleGenerator() {
        return new SyncConflict().id(longCount.incrementAndGet()).gitCommit(UUID.randomUUID().toString());
    }
}
