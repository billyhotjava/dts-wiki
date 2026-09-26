package com.yuzhi.dts.wiki.service.mapper;

import static com.yuzhi.dts.wiki.domain.SyncRootAsserts.*;
import static com.yuzhi.dts.wiki.domain.SyncRootTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncRootMapperTest {

    private SyncRootMapper syncRootMapper;

    @BeforeEach
    void setUp() {
        syncRootMapper = new SyncRootMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSyncRootSample1();
        var actual = syncRootMapper.toEntity(syncRootMapper.toDto(expected));
        assertSyncRootAllPropertiesEquals(expected, actual);
    }
}
