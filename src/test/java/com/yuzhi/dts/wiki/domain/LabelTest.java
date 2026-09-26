package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.LabelTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LabelTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Label.class);
        Label label1 = getLabelSample1();
        Label label2 = new Label();
        assertThat(label1).isNotEqualTo(label2);

        label2.setId(label1.getId());
        assertThat(label1).isEqualTo(label2);

        label2 = getLabelSample2();
        assertThat(label1).isNotEqualTo(label2);
    }

    @Test
    void pagesTest() {
        Label label = getLabelRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        label.addPages(pageBack);
        assertThat(label.getPageses()).containsOnly(pageBack);
        assertThat(pageBack.getLabelses()).containsOnly(label);

        label.removePages(pageBack);
        assertThat(label.getPageses()).doesNotContain(pageBack);
        assertThat(pageBack.getLabelses()).doesNotContain(label);

        label.pageses(new HashSet<>(Set.of(pageBack)));
        assertThat(label.getPageses()).containsOnly(pageBack);
        assertThat(pageBack.getLabelses()).containsOnly(label);

        label.setPageses(new HashSet<>());
        assertThat(label.getPageses()).doesNotContain(pageBack);
        assertThat(pageBack.getLabelses()).doesNotContain(label);
    }
}
