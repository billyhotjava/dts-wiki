package com.yuzhi.dts.wiki.service.mapper;

import static com.yuzhi.dts.wiki.domain.SyncConflictAsserts.*;
import static com.yuzhi.dts.wiki.domain.SyncConflictTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SyncConflictMapperTest {

    private SyncConflictMapper syncConflictMapper;

    @BeforeEach
    void setUp() {
        syncConflictMapper = new SyncConflictMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getSyncConflictSample1();
        var actual = syncConflictMapper.toEntity(syncConflictMapper.toDto(expected));
        assertSyncConflictAllPropertiesEquals(expected, actual);
    }
}
