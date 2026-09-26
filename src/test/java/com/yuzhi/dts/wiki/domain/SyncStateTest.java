package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.SyncRootTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SyncStateTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SyncStateTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(SyncState.class);
        SyncState syncState1 = getSyncStateSample1();
        SyncState syncState2 = new SyncState();
        assertThat(syncState1).isNotEqualTo(syncState2);

        syncState2.setId(syncState1.getId());
        assertThat(syncState1).isEqualTo(syncState2);

        syncState2 = getSyncStateSample2();
        assertThat(syncState1).isNotEqualTo(syncState2);
    }

    @Test
    void syncRootTest() {
        SyncState syncState = getSyncStateRandomSampleGenerator();
        SyncRoot syncRootBack = getSyncRootRandomSampleGenerator();

        syncState.setSyncRoot(syncRootBack);
        assertThat(syncState.getSyncRoot()).isEqualTo(syncRootBack);

        syncState.syncRoot(null);
        assertThat(syncState.getSyncRoot()).isNull();
    }
}
