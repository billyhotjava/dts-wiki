package com.yuzhi.dts.wiki.service.mapper;

import static com.yuzhi.dts.wiki.domain.PageVersionAsserts.*;
import static com.yuzhi.dts.wiki.domain.PageVersionTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PageVersionMapperTest {

    private PageVersionMapper pageVersionMapper;

    @BeforeEach
    void setUp() {
        pageVersionMapper = new PageVersionMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPageVersionSample1();
        var actual = pageVersionMapper.toEntity(pageVersionMapper.toDto(expected));
        assertPageVersionAllPropertiesEquals(expected, actual);
    }
}
