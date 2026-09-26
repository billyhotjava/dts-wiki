package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class LabelTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Label getLabelSample1() {
        return new Label().id(1L).name("name1");
    }

    public static Label getLabelSample2() {
        return new Label().id(2L).name("name2");
    }

    public static Label getLabelRandomSampleGenerator() {
        return new Label().id(longCount.incrementAndGet()).name(UUID.randomUUID().toString());
    }
}
