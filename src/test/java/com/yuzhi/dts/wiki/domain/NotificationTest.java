package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.NotificationTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Notification.class);
        Notification notification1 = getNotificationSample1();
        Notification notification2 = new Notification();
        assertThat(notification1).isNotEqualTo(notification2);

        notification2.setId(notification1.getId());
        assertThat(notification1).isEqualTo(notification2);

        notification2 = getNotificationSample2();
        assertThat(notification1).isNotEqualTo(notification2);
    }

    @Test
    void pageTest() {
        Notification notification = getNotificationRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        notification.setPage(pageBack);
        assertThat(notification.getPage()).isEqualTo(pageBack);

        notification.page(null);
        assertThat(notification.getPage()).isNull();
    }
}
