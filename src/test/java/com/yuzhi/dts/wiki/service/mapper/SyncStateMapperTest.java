package com.yuzhi.dts.wiki.service.mapper;

import static com.yuzhi.dts.wiki.domain.SyncStateAsserts.*;
import static com.yuzhi.dts.wiki.domain.SyncStateTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncStateMapperTest {

    private SyncStateMapper syncStateMapper;

    @BeforeEach
    void setUp() {
        syncStateMapper = new SyncStateMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSyncStateSample1();
        var actual = syncStateMapper.toEntity(syncStateMapper.toDto(expected));
        assertSyncStateAllPropertiesEquals(expected, actual);
    }
}
