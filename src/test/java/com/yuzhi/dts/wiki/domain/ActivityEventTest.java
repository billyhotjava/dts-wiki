package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.ActivityEventTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SpaceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ActivityEventTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ActivityEvent.class);
        ActivityEvent activityEvent1 = getActivityEventSample1();
        ActivityEvent activityEvent2 = new ActivityEvent();
        assertThat(activityEvent1).isNotEqualTo(activityEvent2);

        activityEvent2.setId(activityEvent1.getId());
        assertThat(activityEvent1).isEqualTo(activityEvent2);

        activityEvent2 = getActivityEventSample2();
        assertThat(activityEvent1).isNotEqualTo(activityEvent2);
    }

    @Test
    void spaceTest() {
        ActivityEvent activityEvent = getActivityEventRandomSampleGenerator();
        Space spaceBack = getSpaceRandomSampleGenerator();

        activityEvent.setSpace(spaceBack);
        assertThat(activityEvent.getSpace()).isEqualTo(spaceBack);

        activityEvent.space(null);
        assertThat(activityEvent.getSpace()).isNull();
    }

    @Test
    void pageTest() {
        ActivityEvent activityEvent = getActivityEventRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        activityEvent.setPage(pageBack);
        assertThat(activityEvent.getPage()).isEqualTo(pageBack);

        activityEvent.page(null);
        assertThat(activityEvent.getPage()).isNull();
    }
}
