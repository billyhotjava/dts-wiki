package com.yuzhi.dts.wiki.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PageDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PageDTO.class);
        PageDTO pageDTO1 = new PageDTO();
        pageDTO1.setId(1L);
        PageDTO pageDTO2 = new PageDTO();
        assertThat(pageDTO1).isNotEqualTo(pageDTO2);
        pageDTO2.setId(pageDTO1.getId());
        assertThat(pageDTO1).isEqualTo(pageDTO2);
        pageDTO2.setId(2L);
        assertThat(pageDTO1).isNotEqualTo(pageDTO2);
        pageDTO1.setId(null);
        assertThat(pageDTO1).isNotEqualTo(pageDTO2);
    }
}
