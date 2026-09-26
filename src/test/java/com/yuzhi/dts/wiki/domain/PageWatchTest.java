package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageWatchTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PageWatchTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PageWatch.class);
        PageWatch pageWatch1 = getPageWatchSample1();
        PageWatch pageWatch2 = new PageWatch();
        assertThat(pageWatch1).isNotEqualTo(pageWatch2);

        pageWatch2.setId(pageWatch1.getId());
        assertThat(pageWatch1).isEqualTo(pageWatch2);

        pageWatch2 = getPageWatchSample2();
        assertThat(pageWatch1).isNotEqualTo(pageWatch2);
    }

    @Test
    void pageTest() {
        PageWatch pageWatch = getPageWatchRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        pageWatch.setPage(pageBack);
        assertThat(pageWatch.getPage()).isEqualTo(pageBack);

        pageWatch.page(null);
        assertThat(pageWatch.getPage()).isNull();
    }
}
