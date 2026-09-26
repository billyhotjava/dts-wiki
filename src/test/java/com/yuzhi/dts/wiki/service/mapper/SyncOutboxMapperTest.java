package com.yuzhi.dts.wiki.service.mapper;

import static com.yuzhi.dts.wiki.domain.SyncOutboxAsserts.*;
import static com.yuzhi.dts.wiki.domain.SyncOutboxTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncOutboxMapperTest {

    private SyncOutboxMapper syncOutboxMapper;

    @BeforeEach
    void setUp() {
        syncOutboxMapper = new SyncOutboxMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSyncOutboxSample1();
        var actual = syncOutboxMapper.toEntity(syncOutboxMapper.toDto(expected));
        assertSyncOutboxAllPropertiesEquals(expected, actual);
    }
}
