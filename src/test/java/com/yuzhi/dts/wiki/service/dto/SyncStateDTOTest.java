package com.yuzhi.dts.wiki.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SyncStateDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SyncStateDTO.class);
        SyncStateDTO syncStateDTO1 = new SyncStateDTO();
        syncStateDTO1.setId(1L);
        SyncStateDTO syncStateDTO2 = new SyncStateDTO();
        assertThat(syncStateDTO1).isNotEqualTo(syncStateDTO2);
        syncStateDTO2.setId(syncStateDTO1.getId());
        assertThat(syncStateDTO1).isEqualTo(syncStateDTO2);
        syncStateDTO2.setId(2L);
        assertThat(syncStateDTO1).isNotEqualTo(syncStateDTO2);
        syncStateDTO1.setId(null);
        assertThat(syncStateDTO1).isNotEqualTo(syncStateDTO2);
    }
}
