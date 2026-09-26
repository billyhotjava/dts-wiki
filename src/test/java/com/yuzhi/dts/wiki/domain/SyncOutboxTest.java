package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SpaceTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SyncOutboxTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SyncOutboxTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SyncOutbox.class);
        SyncOutbox syncOutbox1 = getSyncOutboxSample1();
        SyncOutbox syncOutbox2 = new SyncOutbox();
        assertThat(syncOutbox1).isNotEqualTo(syncOutbox2);

        syncOutbox2.setId(syncOutbox1.getId());
        assertThat(syncOutbox1).isEqualTo(syncOutbox2);

        syncOutbox2 = getSyncOutboxSample2();
        assertThat(syncOutbox1).isNotEqualTo(syncOutbox2);
    }

    @Test
    void spaceTest() {
        SyncOutbox syncOutbox = getSyncOutboxRandomSampleGenerator();
        Space spaceBack = getSpaceRandomSampleGenerator();

        syncOutbox.setSpace(spaceBack);
        assertThat(syncOutbox.getSpace()).isEqualTo(spaceBack);

        syncOutbox.space(null);
        assertThat(syncOutbox.getSpace()).isNull();
    }

    @Test
    void pageTest() {
        SyncOutbox syncOutbox = getSyncOutboxRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        syncOutbox.setPage(pageBack);
        assertThat(syncOutbox.getPage()).isEqualTo(pageBack);

        syncOutbox.page(null);
        assertThat(syncOutbox.getPage()).isNull();
    }
}
