package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.PageDraftTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PageDraftTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PageDraft.class);
        PageDraft pageDraft1 = getPageDraftSample1();
        PageDraft pageDraft2 = new PageDraft();
        assertThat(pageDraft1).isNotEqualTo(pageDraft2);

        pageDraft2.setId(pageDraft1.getId());
        assertThat(pageDraft1).isEqualTo(pageDraft2);

        pageDraft2 = getPageDraftSample2();
        assertThat(pageDraft1).isNotEqualTo(pageDraft2);
    }

    @Test
    void pageTest() {
        PageDraft pageDraft = getPageDraftRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        pageDraft.setPage(pageBack);
        assertThat(pageDraft.getPage()).isEqualTo(pageBack);

        pageDraft.page(null);
        assertThat(pageDraft.getPage()).isNull();
    }
}
