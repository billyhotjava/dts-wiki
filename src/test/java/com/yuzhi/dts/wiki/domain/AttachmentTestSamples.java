package com.yuzhi.dts.wiki.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class AttachmentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Attachment getAttachmentSample1() {
        return new Attachment().id(1L).fileName("fileName1").mimeType("mimeType1").size(1L).sha256("sha2561").gitPath("gitPath1");
    }

    public static Attachment getAttachmentSample2() {
        return new Attachment().id(2L).fileName("fileName2").mimeType("mimeType2").size(2L).sha256("sha2562").gitPath("gitPath2");
    }

    public static Attachment getAttachmentRandomSampleGenerator() {
        return new Attachment()
            .id(longCount.incrementAndGet())
            .fileName(UUID.randomUUID().toString())
            .mimeType(UUID.randomUUID().toString())
            .size(longCount.incrementAndGet())
            .sha256(UUID.randomUUID().toString())
            .gitPath(UUID.randomUUID().toString());
    }
}
