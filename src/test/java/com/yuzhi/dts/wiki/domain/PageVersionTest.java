package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageVersionTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PageVersionTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PageVersion.class);
        PageVersion pageVersion1 = getPageVersionSample1();
        PageVersion pageVersion2 = new PageVersion();
        assertThat(pageVersion1).isNotEqualTo(pageVersion2);

        pageVersion2.setId(pageVersion1.getId());
        assertThat(pageVersion1).isEqualTo(pageVersion2);

        pageVersion2 = getPageVersionSample2();
        assertThat(pageVersion1).isNotEqualTo(pageVersion2);
    }

    @Test
    void pageTest() {
        PageVersion pageVersion = getPageVersionRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        pageVersion.setPage(pageBack);
        assertThat(pageVersion.getPage()).isEqualTo(pageBack);

        pageVersion.page(null);
        assertThat(pageVersion.getPage()).isNull();
    }
}
