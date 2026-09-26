package com.yuzhi.dts.wiki.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SyncRootDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SyncRootDTO.class);
        SyncRootDTO syncRootDTO1 = new SyncRootDTO();
        syncRootDTO1.setId(1L);
        SyncRootDTO syncRootDTO2 = new SyncRootDTO();
        assertThat(syncRootDTO1).isNotEqualTo(syncRootDTO2);
        syncRootDTO2.setId(syncRootDTO1.getId());
        assertThat(syncRootDTO1).isEqualTo(syncRootDTO2);
        syncRootDTO2.setId(2L);
        assertThat(syncRootDTO1).isNotEqualTo(syncRootDTO2);
        syncRootDTO1.setId(null);
        assertThat(syncRootDTO1).isNotEqualTo(syncRootDTO2);
    }
}
