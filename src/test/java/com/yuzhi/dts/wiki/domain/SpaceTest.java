package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SpaceTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SyncRootTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpaceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Space.class);
        Space space1 = getSpaceSample1();
        Space space2 = new Space();
        assertThat(space1).isNotEqualTo(space2);

        space2.setId(space1.getId());
        assertThat(space1).isEqualTo(space2);

        space2 = getSpaceSample2();
        assertThat(space1).isNotEqualTo(space2);
    }

    @Test
    void syncRootsTest() {
        Space space = getSpaceRandomSampleGenerator();
        SyncRoot syncRootBack = getSyncRootRandomSampleGenerator();

        space.addSyncRoots(syncRootBack);
        assertThat(space.getSyncRootses()).containsOnly(syncRootBack);
        assertThat(syncRootBack.getSpace()).isEqualTo(space);

        space.removeSyncRoots(syncRootBack);
        assertThat(space.getSyncRootses()).doesNotContain(syncRootBack);
        assertThat(syncRootBack.getSpace()).isNull();

        space.syncRootses(new HashSet<>(Set.of(syncRootBack)));
        assertThat(space.getSyncRootses()).containsOnly(syncRootBack);
        assertThat(syncRootBack.getSpace()).isEqualTo(space);

        space.setSyncRootses(new HashSet<>());
        assertThat(space.getSyncRootses()).doesNotContain(syncRootBack);
        assertThat(syncRootBack.getSpace()).isNull();
    }

    @Test
    void pagesTest() {
        Space space = getSpaceRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        space.addPages(pageBack);
        assertThat(space.getPageses()).containsOnly(pageBack);
        assertThat(pageBack.getSpace()).isEqualTo(space);

        space.removePages(pageBack);
        assertThat(space.getPageses()).doesNotContain(pageBack);
        assertThat(pageBack.getSpace()).isNull();

        space.pageses(new HashSet<>(Set.of(pageBack)));
        assertThat(space.getPageses()).containsOnly(pageBack);
        assertThat(pageBack.getSpace()).isEqualTo(space);

        space.setPageses(new HashSet<>());
        assertThat(space.getPageses()).doesNotContain(pageBack);
        assertThat(pageBack.getSpace()).isNull();
    }
}
