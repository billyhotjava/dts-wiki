package com.yuzhi.dts.wiki.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SyncConflictDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SyncConflictDTO.class);
        SyncConflictDTO syncConflictDTO1 = new SyncConflictDTO();
        syncConflictDTO1.setId(1L);
        SyncConflictDTO syncConflictDTO2 = new SyncConflictDTO();
        assertThat(syncConflictDTO1).isNotEqualTo(syncConflictDTO2);
        syncConflictDTO2.setId(syncConflictDTO1.getId());
        assertThat(syncConflictDTO1).isEqualTo(syncConflictDTO2);
        syncConflictDTO2.setId(2L);
        assertThat(syncConflictDTO1).isNotEqualTo(syncConflictDTO2);
        syncConflictDTO1.setId(null);
        assertThat(syncConflictDTO1).isNotEqualTo(syncConflictDTO2);
    }
}
