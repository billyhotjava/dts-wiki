package com.yuzhi.dts.wiki.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class SyncOutboxDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(SyncOutboxDTO.class);
        SyncOutboxDTO syncOutboxDTO1 = new SyncOutboxDTO();
        syncOutboxDTO1.setId(1L);
        SyncOutboxDTO syncOutboxDTO2 = new SyncOutboxDTO();
        assertThat(syncOutboxDTO1).isNotEqualTo(syncOutboxDTO2);
        syncOutboxDTO2.setId(syncOutboxDTO1.getId());
        assertThat(syncOutboxDTO1).isEqualTo(syncOutboxDTO2);
        syncOutboxDTO2.setId(2L);
        assertThat(syncOutboxDTO1).isNotEqualTo(syncOutboxDTO2);
        syncOutboxDTO1.setId(null);
        assertThat(syncOutboxDTO1).isNotEqualTo(syncOutboxDTO2);
    }
}
