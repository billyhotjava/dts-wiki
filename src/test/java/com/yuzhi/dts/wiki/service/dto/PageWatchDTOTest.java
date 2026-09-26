package com.yuzhi.dts.wiki.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PageWatchDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PageWatchDTO.class);
        PageWatchDTO pageWatchDTO1 = new PageWatchDTO();
        pageWatchDTO1.setId(1L);
        PageWatchDTO pageWatchDTO2 = new PageWatchDTO();
        assertThat(pageWatchDTO1).isNotEqualTo(pageWatchDTO2);
        pageWatchDTO2.setId(pageWatchDTO1.getId());
        assertThat(pageWatchDTO1).isEqualTo(pageWatchDTO2);
        pageWatchDTO2.setId(2L);
        assertThat(pageWatchDTO1).isNotEqualTo(pageWatchDTO2);
        pageWatchDTO1.setId(null);
        assertThat(pageWatchDTO1).isNotEqualTo(pageWatchDTO2);
    }
}
