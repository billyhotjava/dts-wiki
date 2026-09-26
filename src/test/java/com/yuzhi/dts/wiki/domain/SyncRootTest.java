package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SpaceTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SyncRootTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SyncStateTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SyncRootTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SyncRoot.class);
        SyncRoot syncRoot1 = getSyncRootSample1();
        SyncRoot syncRoot2 = new SyncRoot();
        assertThat(syncRoot1).isNotEqualTo(syncRoot2);

        syncRoot2.setId(syncRoot1.getId());
        assertThat(syncRoot1).isEqualTo(syncRoot2);

        syncRoot2 = getSyncRootSample2();
        assertThat(syncRoot1).isNotEqualTo(syncRoot2);
    }

    @Test
    void mountPageTest() {
        SyncRoot syncRoot = getSyncRootRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        syncRoot.setMountPage(pageBack);
        assertThat(syncRoot.getMountPage()).isEqualTo(pageBack);

        syncRoot.mountPage(null);
        assertThat(syncRoot.getMountPage()).isNull();
    }

    @Test
    void spaceTest() {
        SyncRoot syncRoot = getSyncRootRandomSampleGenerator();
        Space spaceBack = getSpaceRandomSampleGenerator();

        syncRoot.setSpace(spaceBack);
        assertThat(syncRoot.getSpace()).isEqualTo(spaceBack);

        syncRoot.space(null);
        assertThat(syncRoot.getSpace()).isNull();
    }

    @Test
    void syncStateTest() {
        SyncRoot syncRoot = getSyncRootRandomSampleGenerator();
        SyncState syncStateBack = getSyncStateRandomSampleGenerator();

        syncRoot.setSyncState(syncStateBack);
        assertThat(syncRoot.getSyncState()).isEqualTo(syncStateBack);
        assertThat(syncStateBack.getSyncRoot()).isEqualTo(syncRoot);

        syncRoot.syncState(null);
        assertThat(syncRoot.getSyncState()).isNull();
        assertThat(syncStateBack.getSyncRoot()).isNull();
    }
}
