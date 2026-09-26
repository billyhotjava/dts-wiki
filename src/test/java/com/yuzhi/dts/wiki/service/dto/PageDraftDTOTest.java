package com.yuzhi.dts.wiki.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PageDraftDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PageDraftDTO.class);
        PageDraftDTO pageDraftDTO1 = new PageDraftDTO();
        pageDraftDTO1.setId(1L);
        PageDraftDTO pageDraftDTO2 = new PageDraftDTO();
        assertThat(pageDraftDTO1).isNotEqualTo(pageDraftDTO2);
        pageDraftDTO2.setId(pageDraftDTO1.getId());
        assertThat(pageDraftDTO1).isEqualTo(pageDraftDTO2);
        pageDraftDTO2.setId(2L);
        assertThat(pageDraftDTO1).isNotEqualTo(pageDraftDTO2);
        pageDraftDTO1.setId(null);
        assertThat(pageDraftDTO1).isNotEqualTo(pageDraftDTO2);
    }
}
