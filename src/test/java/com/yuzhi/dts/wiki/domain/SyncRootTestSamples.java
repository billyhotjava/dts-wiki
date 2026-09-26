package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class SyncRootTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static SyncRoot getSyncRootSample1() {
        return new SyncRoot().id(1L).repoPath("repoPath1");
    }

    public static SyncRoot getSyncRootSample2() {
        return new SyncRoot().id(2L).repoPath("repoPath2");
    }

    public static SyncRoot getSyncRootRandomSampleGenerator() {
        return new SyncRoot().id(longCount.incrementAndGet()).repoPath(UUID.randomUUID().toString());
    }
}
