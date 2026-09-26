package com.yuzhi.dts.wiki.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ActivityEventDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(ActivityEventDTO.class);
        ActivityEventDTO activityEventDTO1 = new ActivityEventDTO();
        activityEventDTO1.setId(1L);
        ActivityEventDTO activityEventDTO2 = new ActivityEventDTO();
        assertThat(activityEventDTO1).isNotEqualTo(activityEventDTO2);
        activityEventDTO2.setId(activityEventDTO1.getId());
        assertThat(activityEventDTO1).isEqualTo(activityEventDTO2);
        activityEventDTO2.setId(2L);
        assertThat(activityEventDTO1).isNotEqualTo(activityEventDTO2);
        activityEventDTO1.setId(null);
        assertThat(activityEventDTO1).isNotEqualTo(activityEventDTO2);
    }
}
