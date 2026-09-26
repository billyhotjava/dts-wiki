package com.yuzhi.dts.wiki.service.mapper;

import static com.yuzhi.dts.wiki.domain.PageAsserts.*;
import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PageMapperTest {

    private PageMapper pageMapper;

    @BeforeEach
    void setUp() {
        pageMapper = new PageMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPageSample1();
        var actual = pageMapper.toEntity(pageMapper.toDto(expected));
        assertPageAllPropertiesEquals(expected, actual);
    }
}
