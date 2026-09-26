package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.AttachmentTestSamples.*;
import static com.yuzhi.dts.wiki.domain.CommentTestSamples.*;
import static com.yuzhi.dts.wiki.domain.LabelTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageVersionTestSamples.*;
import static com.yuzhi.dts.wiki.domain.SpaceTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PageTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Page.class);
        Page page1 = getPageSample1();
        Page page2 = new Page();
        assertThat(page1).isNotEqualTo(page2);

        page2.setId(page1.getId());
        assertThat(page1).isEqualTo(page2);

        page2 = getPageSample2();
        assertThat(page1).isNotEqualTo(page2);
    }

    @Test
    void childrenTest() {
        Page page = getPageRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        page.addChildren(pageBack);
        assertThat(page.getChildrens()).containsOnly(pageBack);
        assertThat(pageBack.getParent()).isEqualTo(page);

        page.removeChildren(pageBack);
        assertThat(page.getChildrens()).doesNotContain(pageBack);
        assertThat(pageBack.getParent()).isNull();

        page.childrens(new HashSet<>(Set.of(pageBack)));
        assertThat(page.getChildrens()).containsOnly(pageBack);
        assertThat(pageBack.getParent()).isEqualTo(page);

        page.setChildrens(new HashSet<>());
        assertThat(page.getChildrens()).doesNotContain(pageBack);
        assertThat(pageBack.getParent()).isNull();
    }

    @Test
    void versionsTest() {
        Page page = getPageRandomSampleGenerator();
        PageVersion pageVersionBack = getPageVersionRandomSampleGenerator();

        page.addVersions(pageVersionBack);
        assertThat(page.getVersionses()).containsOnly(pageVersionBack);
        assertThat(pageVersionBack.getPage()).isEqualTo(page);

        page.removeVersions(pageVersionBack);
        assertThat(page.getVersionses()).doesNotContain(pageVersionBack);
        assertThat(pageVersionBack.getPage()).isNull();

        page.versionses(new HashSet<>(Set.of(pageVersionBack)));
        assertThat(page.getVersionses()).containsOnly(pageVersionBack);
        assertThat(pageVersionBack.getPage()).isEqualTo(page);

        page.setVersionses(new HashSet<>());
        assertThat(page.getVersionses()).doesNotContain(pageVersionBack);
        assertThat(pageVersionBack.getPage()).isNull();
    }

    @Test
    void attachmentsTest() {
        Page page = getPageRandomSampleGenerator();
        Attachment attachmentBack = getAttachmentRandomSampleGenerator();

        page.addAttachments(attachmentBack);
        assertThat(page.getAttachmentses()).containsOnly(attachmentBack);
        assertThat(attachmentBack.getPage()).isEqualTo(page);

        page.removeAttachments(attachmentBack);
        assertThat(page.getAttachmentses()).doesNotContain(attachmentBack);
        assertThat(attachmentBack.getPage()).isNull();

        page.attachmentses(new HashSet<>(Set.of(attachmentBack)));
        assertThat(page.getAttachmentses()).containsOnly(attachmentBack);
        assertThat(attachmentBack.getPage()).isEqualTo(page);

        page.setAttachmentses(new HashSet<>());
        assertThat(page.getAttachmentses()).doesNotContain(attachmentBack);
        assertThat(attachmentBack.getPage()).isNull();
    }

    @Test
    void commentsTest() {
        Page page = getPageRandomSampleGenerator();
        Comment commentBack = getCommentRandomSampleGenerator();

        page.addComments(commentBack);
        assertThat(page.getCommentses()).containsOnly(commentBack);
        assertThat(commentBack.getPage()).isEqualTo(page);

        page.removeComments(commentBack);
        assertThat(page.getCommentses()).doesNotContain(commentBack);
        assertThat(commentBack.getPage()).isNull();

        page.commentses(new HashSet<>(Set.of(commentBack)));
        assertThat(page.getCommentses()).containsOnly(commentBack);
        assertThat(commentBack.getPage()).isEqualTo(page);

        page.setCommentses(new HashSet<>());
        assertThat(page.getCommentses()).doesNotContain(commentBack);
        assertThat(commentBack.getPage()).isNull();
    }

    @Test
    void currentVersionTest() {
        Page page = getPageRandomSampleGenerator();
        PageVersion pageVersionBack = getPageVersionRandomSampleGenerator();

        page.setCurrentVersion(pageVersionBack);
        assertThat(page.getCurrentVersion()).isEqualTo(pageVersionBack);

        page.currentVersion(null);
        assertThat(page.getCurrentVersion()).isNull();
    }

    @Test
    void labelsTest() {
        Page page = getPageRandomSampleGenerator();
        Label labelBack = getLabelRandomSampleGenerator();

        page.addLabels(labelBack);
        assertThat(page.getLabelses()).containsOnly(labelBack);

        page.removeLabels(labelBack);
        assertThat(page.getLabelses()).doesNotContain(labelBack);

        page.labelses(new HashSet<>(Set.of(labelBack)));
        assertThat(page.getLabelses()).containsOnly(labelBack);

        page.setLabelses(new HashSet<>());
        assertThat(page.getLabelses()).doesNotContain(labelBack);
    }

    @Test
    void spaceTest() {
        Page page = getPageRandomSampleGenerator();
        Space spaceBack = getSpaceRandomSampleGenerator();

        page.setSpace(spaceBack);
        assertThat(page.getSpace()).isEqualTo(spaceBack);

        page.space(null);
        assertThat(page.getSpace()).isNull();
    }

    @Test
    void parentTest() {
        Page page = getPageRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        page.setParent(pageBack);
        assertThat(page.getParent()).isEqualTo(pageBack);

        page.parent(null);
        assertThat(page.getParent()).isNull();
    }
}
