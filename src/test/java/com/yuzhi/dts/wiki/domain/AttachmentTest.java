package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.AttachmentTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AttachmentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Attachment.class);
        Attachment attachment1 = getAttachmentSample1();
        Attachment attachment2 = new Attachment();
        assertThat(attachment1).isNotEqualTo(attachment2);

        attachment2.setId(attachment1.getId());
        assertThat(attachment1).isEqualTo(attachment2);

        attachment2 = getAttachmentSample2();
        assertThat(attachment1).isNotEqualTo(attachment2);
    }

    @Test
    void pageTest() {
        Attachment attachment = getAttachmentRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        attachment.setPage(pageBack);
        assertThat(attachment.getPage()).isEqualTo(pageBack);

        attachment.page(null);
        assertThat(attachment.getPage()).isNull();
    }
}
