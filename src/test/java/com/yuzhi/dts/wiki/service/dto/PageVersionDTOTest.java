package com.yuzhi.dts.wiki.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PageVersionDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PageVersionDTO.class);
        PageVersionDTO pageVersionDTO1 = new PageVersionDTO();
        pageVersionDTO1.setId(1L);
        PageVersionDTO pageVersionDTO2 = new PageVersionDTO();
        assertThat(pageVersionDTO1).isNotEqualTo(pageVersionDTO2);
        pageVersionDTO2.setId(pageVersionDTO1.getId());
        assertThat(pageVersionDTO1).isEqualTo(pageVersionDTO2);
        pageVersionDTO2.setId(2L);
        assertThat(pageVersionDTO1).isNotEqualTo(pageVersionDTO2);
        pageVersionDTO1.setId(null);
        assertThat(pageVersionDTO1).isNotEqualTo(pageVersionDTO2);
    }
}
