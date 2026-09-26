package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageVersionTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SyncConflictTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SyncConflictTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SyncConflict.class);
        SyncConflict syncConflict1 = getSyncConflictSample1();
        SyncConflict syncConflict2 = new SyncConflict();
        assertThat(syncConflict1).isNotEqualTo(syncConflict2);

        syncConflict2.setId(syncConflict1.getId());
        assertThat(syncConflict1).isEqualTo(syncConflict2);

        syncConflict2 = getSyncConflictSample2();
        assertThat(syncConflict1).isNotEqualTo(syncConflict2);
    }

    @Test
    void pageTest() {
        SyncConflict syncConflict = getSyncConflictRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        syncConflict.setPage(pageBack);
        assertThat(syncConflict.getPage()).isEqualTo(pageBack);

        syncConflict.page(null);
        assertThat(syncConflict.getPage()).isNull();
    }

    @Test
    void baseVersionTest() {
        SyncConflict syncConflict = getSyncConflictRandomSampleGenerator();
        PageVersion pageVersionBack = getPageVersionRandomSampleGenerator();

        syncConflict.setBaseVersion(pageVersionBack);
        assertThat(syncConflict.getBaseVersion()).isEqualTo(pageVersionBack);

        syncConflict.baseVersion(null);
        assertThat(syncConflict.getBaseVersion()).isNull();
    }

    @Test
    void wikiVersionTest() {
        SyncConflict syncConflict = getSyncConflictRandomSampleGenerator();
        PageVersion pageVersionBack = getPageVersionRandomSampleGenerator();

        syncConflict.setWikiVersion(pageVersionBack);
        assertThat(syncConflict.getWikiVersion()).isEqualTo(pageVersionBack);

        syncConflict.wikiVersion(null);
        assertThat(syncConflict.getWikiVersion()).isNull();
    }
}
