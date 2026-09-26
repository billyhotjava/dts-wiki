package com.yuzhi.dts.wiki.service.mapper;

import static com.yuzhi.dts.wiki.domain.PageWatchAsserts.*;
import static com.yuzhi.dts.wiki.domain.PageWatchTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PageWatchMapperTest {

    private PageWatchMapper pageWatchMapper;

    @BeforeEach
    void setUp() {
        pageWatchMapper = new PageWatchMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPageWatchSample1();
        var actual = pageWatchMapper.toEntity(pageWatchMapper.toDto(expected));
        assertPageWatchAllPropertiesEquals(expected, actual);
    }
}
