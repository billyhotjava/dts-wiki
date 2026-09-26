package com.yuzhi.dts.wiki.domain;

import static com.yuzhi.dts.wiki.domain.CommentTestSamples.*;
import static com.yuzhi.dts.wiki.domain.CommentTestSamples.*;
import static com.yuzhi.dts.wiki.domain.PageTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.yuzhi.dts.wiki.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CommentTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Comment.class);
        Comment comment1 = getCommentSample1();
        Comment comment2 = new Comment();
        assertThat(comment1).isNotEqualTo(comment2);

        comment2.setId(comment1.getId());
        assertThat(comment1).isEqualTo(comment2);

        comment2 = getCommentSample2();
        assertThat(comment1).isNotEqualTo(comment2);
    }

    @Test
    void repliesTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Comment commentBack = getCommentRandomSampleGenerator();

        comment.addReplies(commentBack);
        assertThat(comment.getReplieses()).containsOnly(commentBack);
        assertThat(commentBack.getParent()).isEqualTo(comment);

        comment.removeReplies(commentBack);
        assertThat(comment.getReplieses()).doesNotContain(commentBack);
        assertThat(commentBack.getParent()).isNull();

        comment.replieses(new HashSet<>(Set.of(commentBack)));
        assertThat(comment.getReplieses()).containsOnly(commentBack);
        assertThat(commentBack.getParent()).isEqualTo(comment);

        comment.setReplieses(new HashSet<>());
        assertThat(comment.getReplieses()).doesNotContain(commentBack);
        assertThat(commentBack.getParent()).isNull();
    }

    @Test
    void pageTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Page pageBack = getPageRandomSampleGenerator();

        comment.setPage(pageBack);
        assertThat(comment.getPage()).isEqualTo(pageBack);

        comment.page(null);
        assertThat(comment.getPage()).isNull();
    }

    @Test
    void parentTest() {
        Comment comment = getCommentRandomSampleGenerator();
        Comment commentBack = getCommentRandomSampleGenerator();

        comment.setParent(commentBack);
        assertThat(comment.getParent()).isEqualTo(commentBack);

        comment.parent(null);
        assertThat(comment.getParent()).isNull();
    }
}
