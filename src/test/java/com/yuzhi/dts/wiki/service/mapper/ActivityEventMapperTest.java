package com.yuzhi.dts.wiki.service.mapper;

import static com.yuzhi.dts.wiki.domain.ActivityEventAsserts.*;
import static com.yuzhi.dts.wiki.domain.ActivityEventTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActivityEventMapperTest {

    private ActivityEventMapper activityEventMapper;

    @BeforeEach
    void setUp() {
        activityEventMapper = new ActivityEventMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getActivityEventSample1();
        var actual = activityEventMapper.toEntity(activityEventMapper.toDto(expected));
        assertActivityEventAllPropertiesEquals(expected, actual);
    }
}
