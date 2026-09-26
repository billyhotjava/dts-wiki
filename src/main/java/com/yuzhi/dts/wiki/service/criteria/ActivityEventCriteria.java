package com.yuzhi.dts.wiki.service.criteria;

import com.yuzhi.dts.wiki.domain.enumeration.ActivityType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.yuzhi.dts.wiki.domain.ActivityEvent} entity. This class is used
 * in {@link com.yuzhi.dts.wiki.web.rest.ActivityEventResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /activity-events?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ActivityEventCriteria implements Serializable, Criteria {

    /**
     * Class for filtering ActivityType
     */
    public static class ActivityTypeFilter extends Filter<ActivityType> {

        public ActivityTypeFilter() {}

        public ActivityTypeFilter(ActivityTypeFilter filter) {
            super(filter);
        }

        @Override
        public ActivityTypeFilter copy() {
            return new ActivityTypeFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private ActivityTypeFilter type;

    private StringFilter actorName;

    private StringFilter targetTitle;

    private InstantFilter createdAt;

    private LongFilter spaceId;

    private LongFilter pageId;

    private Boolean distinct;

    public ActivityEventCriteria() {}

    public ActivityEventCriteria(ActivityEventCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.type = other.optionalType().map(ActivityTypeFilter::copy).orElse(null);
        this.actorName = other.optionalActorName().map(StringFilter::copy).orElse(null);
        this.targetTitle = other.optionalTargetTitle().map(StringFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.spaceId = other.optionalSpaceId().map(LongFilter::copy).orElse(null);
        this.pageId = other.optionalPageId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public ActivityEventCriteria copy() {
        return new ActivityEventCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public ActivityTypeFilter getType() {
        return type;
    }

    public Optional<ActivityTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public ActivityTypeFilter type() {
        if (type == null) {
            setType(new ActivityTypeFilter());
        }
        return type;
    }

    public void setType(ActivityTypeFilter type) {
        this.type = type;
    }

    public StringFilter getActorName() {
        return actorName;
    }

    public Optional<StringFilter> optionalActorName() {
        return Optional.ofNullable(actorName);
    }

    public StringFilter actorName() {
        if (actorName == null) {
            setActorName(new StringFilter());
        }
        return actorName;
    }

    public void setActorName(StringFilter actorName) {
        this.actorName = actorName;
    }

    public StringFilter getTargetTitle() {
        return targetTitle;
    }

    public Optional<StringFilter> optionalTargetTitle() {
        return Optional.ofNullable(targetTitle);
    }

    public StringFilter targetTitle() {
        if (targetTitle == null) {
            setTargetTitle(new StringFilter());
        }
        return targetTitle;
    }

    public void setTargetTitle(StringFilter targetTitle) {
        this.targetTitle = targetTitle;
    }

    public InstantFilter getCreatedAt() {
        return createdAt;
    }

    public Optional<InstantFilter> optionalCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public InstantFilter createdAt() {
        if (createdAt == null) {
            setCreatedAt(new InstantFilter());
        }
        return createdAt;
    }

    public void setCreatedAt(InstantFilter createdAt) {
        this.createdAt = createdAt;
    }

    public LongFilter getSpaceId() {
        return spaceId;
    }

    public Optional<LongFilter> optionalSpaceId() {
        return Optional.ofNullable(spaceId);
    }

    public LongFilter spaceId() {
        if (spaceId == null) {
            setSpaceId(new LongFilter());
        }
        return spaceId;
    }

    public void setSpaceId(LongFilter spaceId) {
        this.spaceId = spaceId;
    }

    public LongFilter getPageId() {
        return pageId;
    }

    public Optional<LongFilter> optionalPageId() {
        return Optional.ofNullable(pageId);
    }

    public LongFilter pageId() {
        if (pageId == null) {
            setPageId(new LongFilter());
        }
        return pageId;
    }

    public void setPageId(LongFilter pageId) {
        this.pageId = pageId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ActivityEventCriteria that = (ActivityEventCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(type, that.type) &&
            Objects.equals(actorName, that.actorName) &&
            Objects.equals(targetTitle, that.targetTitle) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(spaceId, that.spaceId) &&
            Objects.equals(pageId, that.pageId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, actorName, targetTitle, createdAt, spaceId, pageId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ActivityEventCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalActorName().map(f -> "actorName=" + f + ", ").orElse("") +
            optionalTargetTitle().map(f -> "targetTitle=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalSpaceId().map(f -> "spaceId=" + f + ", ").orElse("") +
            optionalPageId().map(f -> "pageId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
