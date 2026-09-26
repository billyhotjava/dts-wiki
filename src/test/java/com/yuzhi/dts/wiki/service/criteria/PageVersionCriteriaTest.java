package com.yuzhi.dts.wiki.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class PageVersionCriteriaTest {

    @Test
    void newPageVersionCriteriaHasAllFiltersNullTest() {
        var pageVersionCriteria = new PageVersionCriteria();
        assertThat(pageVersionCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void pageVersionCriteriaFluentMethodsCreatesFiltersTest() {
        var pageVersionCriteria = new PageVersionCriteria();

        setAllFilters(pageVersionCriteria);

        assertThat(pageVersionCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void pageVersionCriteriaCopyCreatesNullFilterTest() {
        var pageVersionCriteria = new PageVersionCriteria();
        var copy = pageVersionCriteria.copy();

        assertThat(pageVersionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(pageVersionCriteria)
        );
    }

    @Test
    void pageVersionCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var pageVersionCriteria = new PageVersionCriteria();
        setAllFilters(pageVersionCriteria);

        var copy = pageVersionCriteria.copy();

        assertThat(pageVersionCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(pageVersionCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var pageVersionCriteria = new PageVersionCriteria();

        assertThat(pageVersionCriteria).hasToString("PageVersionCriteria{}");
    }

    private static void setAllFilters(PageVersionCriteria pageVersionCriteria) {
        pageVersionCriteria.id();
        pageVersionCriteria.versionNo();
        pageVersionCriteria.contentSha256();
        pageVersionCriteria.authorName();
        pageVersionCriteria.authorEmail();
        pageVersionCriteria.source();
        pageVersionCriteria.gitCommit();
        pageVersionCriteria.message();
        pageVersionCriteria.createdAt();
        pageVersionCriteria.authorId();
        pageVersionCriteria.pageId();
        pageVersionCriteria.distinct();
    }

    private static Condition<PageVersionCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getVersionNo()) &&
                condition.apply(criteria.getContentSha256()) &&
                condition.apply(criteria.getAuthorName()) &&
                condition.apply(criteria.getAuthorEmail()) &&
                condition.apply(criteria.getSource()) &&
                condition.apply(criteria.getGitCommit()) &&
                condition.apply(criteria.getMessage()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getAuthorId()) &&
                condition.apply(criteria.getPageId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<PageVersionCriteria> copyFiltersAre(PageVersionCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getVersionNo(), copy.getVersionNo()) &&
                condition.apply(criteria.getContentSha256(), copy.getContentSha256()) &&
                condition.apply(criteria.getAuthorName(), copy.getAuthorName()) &&
                condition.apply(criteria.getAuthorEmail(), copy.getAuthorEmail()) &&
                condition.apply(criteria.getSource(), copy.getSource()) &&
                condition.apply(criteria.getGitCommit(), copy.getGitCommit()) &&
                condition.apply(criteria.getMessage(), copy.getMessage()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getAuthorId(), copy.getAuthorId()) &&
                condition.apply(criteria.getPageId(), copy.getPageId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
