package com.yuzhi.dts.wiki.service.mapper;

import static com.yuzhi.dts.wiki.domain.PageDraftAsserts.*;
import static com.yuzhi.dts.wiki.domain.PageDraftTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PageDraftMapperTest {

    private PageDraftMapper pageDraftMapper;

    @BeforeEach
    void setUp() {
        pageDraftMapper = new PageDraftMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getPageDraftSample1();
        var actual = pageDraftMapper.toEntity(pageDraftMapper.toDto(expected));
        assertPageDraftAllPropertiesEquals(expected, actual);
    }
}
