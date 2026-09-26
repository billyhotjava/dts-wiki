package com.yuzhi.dts.wiki.service.criteria;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

class ActivityEventCriteriaTest {

    @Test
    void newActivityEventCriteriaHasAllFiltersNullTest() {
        var activityEventCriteria = new ActivityEventCriteria();
        assertThat(activityEventCriteria).is(criteriaFiltersAre(Objects::isNull));
    }

    @Test
    void activityEventCriteriaFluentMethodsCreatesFiltersTest() {
        var activityEventCriteria = new ActivityEventCriteria();

        setAllFilters(activityEventCriteria);

        assertThat(activityEventCriteria).is(criteriaFiltersAre(Objects::nonNull));
    }

    @Test
    void activityEventCriteriaCopyCreatesNullFilterTest() {
        var activityEventCriteria = new ActivityEventCriteria();
        var copy = activityEventCriteria.copy();

        assertThat(activityEventCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::isNull)),
            criteria -> assertThat(criteria).isEqualTo(activityEventCriteria)
        );
    }

    @Test
    void activityEventCriteriaCopyDuplicatesEveryExistingFilterTest() {
        var activityEventCriteria = new ActivityEventCriteria();
        setAllFilters(activityEventCriteria);

        var copy = activityEventCriteria.copy();

        assertThat(activityEventCriteria).satisfies(
            criteria ->
                assertThat(criteria).is(copyFiltersAre(copy, (a, b) -> a == null || a instanceof Boolean ? a == b : a != b && a.equals(b))),
            criteria -> assertThat(criteria).isEqualTo(copy),
            criteria -> assertThat(criteria).hasSameHashCodeAs(copy)
        );

        assertThat(copy).satisfies(
            criteria -> assertThat(criteria).is(criteriaFiltersAre(Objects::nonNull)),
            criteria -> assertThat(criteria).isEqualTo(activityEventCriteria)
        );
    }

    @Test
    void toStringVerifier() {
        var activityEventCriteria = new ActivityEventCriteria();

        assertThat(activityEventCriteria).hasToString("ActivityEventCriteria{}");
    }

    private static void setAllFilters(ActivityEventCriteria activityEventCriteria) {
        activityEventCriteria.id();
        activityEventCriteria.type();
        activityEventCriteria.actorName();
        activityEventCriteria.targetTitle();
        activityEventCriteria.createdAt();
        activityEventCriteria.spaceId();
        activityEventCriteria.pageId();
        activityEventCriteria.distinct();
    }

    private static Condition<ActivityEventCriteria> criteriaFiltersAre(Function<Object, Boolean> condition) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId()) &&
                condition.apply(criteria.getType()) &&
                condition.apply(criteria.getActorName()) &&
                condition.apply(criteria.getTargetTitle()) &&
                condition.apply(criteria.getCreatedAt()) &&
                condition.apply(criteria.getSpaceId()) &&
                condition.apply(criteria.getPageId()) &&
                condition.apply(criteria.getDistinct()),
            "every filter matches"
        );
    }

    private static Condition<ActivityEventCriteria> copyFiltersAre(
        ActivityEventCriteria copy,
        BiFunction<Object, Object, Boolean> condition
    ) {
        return new Condition<>(
            criteria ->
                condition.apply(criteria.getId(), copy.getId()) &&
                condition.apply(criteria.getType(), copy.getType()) &&
                condition.apply(criteria.getActorName(), copy.getActorName()) &&
                condition.apply(criteria.getTargetTitle(), copy.getTargetTitle()) &&
                condition.apply(criteria.getCreatedAt(), copy.getCreatedAt()) &&
                condition.apply(criteria.getSpaceId(), copy.getSpaceId()) &&
                condition.apply(criteria.getPageId(), copy.getPageId()) &&
                condition.apply(criteria.getDistinct(), copy.getDistinct()),
            "every filter matches"
        );
    }
}
