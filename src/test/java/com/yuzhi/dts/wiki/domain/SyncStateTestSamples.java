package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SyncStateTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static SyncState getSyncStateSample1() {
        return new SyncState().id(1L).lastSyncedCommit("lastSyncedCommit1");
    }

    public static SyncState getSyncStateSample2() {
        return new SyncState().id(2L).lastSyncedCommit("lastSyncedCommit2");
    }

    public static SyncState getSyncStateRandomSampleGenerator() {
        return new SyncState().id(longCount.incrementAndGet()).lastSyncedCommit(UUID.randomUUID().toString());
    }
}
