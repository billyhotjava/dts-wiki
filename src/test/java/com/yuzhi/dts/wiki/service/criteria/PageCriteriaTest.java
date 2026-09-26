package com.yuzhi.dts.wiki.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class PageCriteriaTest {

    @Test
    void newPageCriteriaHasAllFiltersNullTest() {
        var pageCriteria = new PageCriteria();
        assertThat(pageCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void pageCriteriaFluentMethodsCreatesFiltersTest() {
        var pageCriteria = new PageCriteria();

        setAllFilters(pageCriteria);

        assertThat(pageCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void pageCriteriaCopyCreatesNullFilterTest() {
        var pageCriteria = new PageCriteria();
        var copy = pageCriteria.copy();

        assertThat(pageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(pageCriteria)
        );
    }

    @Test
    void pageCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var pageCriteria = new PageCriteria();
        setAllFilters(pageCriteria);

        var copy = pageCriteria.copy();

        assertThat(pageCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(pageCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var pageCriteria = new PageCriteria();

        assertThat(pageCriteria).hasToString("PageCriteria{}");
    }

    private static void setAllFilters(PageCriteria pageCriteria) {
        pageCriteria.id();
        pageCriteria.title();
        pageCriteria.kind();
        pageCriteria.gitPath();
        pageCriteria.position();
        pageCriteria.syncStatus();
        pageCriteria.createdAt();
        pageCriteria.updatedAt();
        pageCriteria.deletedAt();
        pageCriteria.childrenId();
        pageCriteria.versionsId();
        pageCriteria.attachmentsId();
        pageCriteria.commentsId();
        pageCriteria.currentVersionId();
        pageCriteria.labelsId();
        pageCriteria.spaceId();
        pageCriteria.parentId();
        pageCriteria.distinct();
    }

    private static Condition<PageCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getTitle()) &&
                condition.apply(criteria.getKind()) &&
                condition.apply(criteria.getGitPath()) &&
                condition.apply(criteria.getPosition()) &&
                condition.apply(criteria.getSyncStatus()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt()) &&
                condition.apply(criteria.getDeletedAt()) &&
                condition.apply(criteria.getChildrenId()) &&
                condition.apply(criteria.getVersionsId()) &&
                condition.apply(criteria.getAttachmentsId()) &&
                condition.apply(criteria.getCommentsId()) &&
                condition.apply(criteria.getCurrentVersionId()) &&
                condition.apply(criteria.getLabelsId()) &&
                condition.apply(criteria.getSpaceId()) &&
                condition.apply(criteria.getParentId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<PageCriteria> copyFiltersAre(PageCriteria copy, BiFunction<Object, Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getTitle(), copy.getTitle()) &&
                condition.apply(criteria.getKind(), copy.getKind()) &&
                condition.apply(criteria.getGitPath(), copy.getGitPath()) &&
                condition.apply(criteria.getPosition(), copy.getPosition()) &&
                condition.apply(criteria.getSyncStatus(), copy.getSyncStatus()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getUpdatedAt(), copy.getUpdatedAt()) &&
                condition.apply(criteria.getDeletedAt(), copy.getDeletedAt()) &&
                condition.apply(criteria.getChildrenId(), copy.getChildrenId()) &&
                condition.apply(criteria.getVersionsId(), copy.getVersionsId()) &&
                condition.apply(criteria.getAttachmentsId(), copy.getAttachmentsId()) &&
                condition.apply(criteria.getCommentsId(), copy.getCommentsId()) &&
                condition.apply(criteria.getCurrentVersionId(), copy.getCurrentVersionId()) &&
                condition.apply(criteria.getLabelsId(), copy.getLabelsId()) &&
                condition.apply(criteria.getSpaceId(), copy.getSpaceId()) &&
                condition.apply(criteria.getParentId(), copy.getParentId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
