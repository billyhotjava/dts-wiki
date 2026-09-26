package com.yuzhi.dts.wiki.service.criteria;

import com.yuzhi.dts.wiki.domain.enumeration.NotificationType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.yuzhi.dts.wiki.domain.Notification} entity. This class is used
 * in {@link com.yuzhi.dts.wiki.web.rest.NotificationResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /notifications?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NotificationCriteria implements Serializable, Criteria {

    /**
     * Class for filtering NotificationType
     */
    public static class NotificationTypeFilter extends Filter<NotificationType> {

        public NotificationTypeFilter() {}

        public NotificationTypeFilter(NotificationTypeFilter filter) {
            super(filter);
        }

        @Override
        public NotificationTypeFilter copy() {
            return new NotificationTypeFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private NotificationTypeFilter type;

    private StringFilter title;

    private StringFilter link;

    private InstantFilter createdAt;

    private InstantFilter readAt;

    private StringFilter recipientId;

    private LongFilter pageId;

    private Boolean distinct;

    public NotificationCriteria() {}

    public NotificationCriteria(NotificationCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.type = other.optionalType().map(NotificationTypeFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.link = other.optionalLink().map(StringFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.readAt = other.optionalReadAt().map(InstantFilter::copy).orElse(null);
        this.recipientId = other.optionalRecipientId().map(StringFilter::copy).orElse(null);
        this.pageId = other.optionalPageId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public NotificationCriteria copy() {
        return new NotificationCriteria(this);
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

    public NotificationTypeFilter getType() {
        return type;
    }

    public Optional<NotificationTypeFilter> optionalType() {
        return Optional.ofNullable(type);
    }

    public NotificationTypeFilter type() {
        if (type == null) {
            setType(new NotificationTypeFilter());
        }
        return type;
    }

    public void setType(NotificationTypeFilter type) {
        this.type = type;
    }

    public StringFilter getTitle() {
        return title;
    }

    public Optional<StringFilter> optionalTitle() {
        return Optional.ofNullable(title);
    }

    public StringFilter title() {
        if (title == null) {
            setTitle(new StringFilter());
        }
        return title;
    }

    public void setTitle(StringFilter title) {
        this.title = title;
    }

    public StringFilter getLink() {
        return link;
    }

    public Optional<StringFilter> optionalLink() {
        return Optional.ofNullable(link);
    }

    public StringFilter link() {
        if (link == null) {
            setLink(new StringFilter());
        }
        return link;
    }

    public void setLink(StringFilter link) {
        this.link = link;
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

    public InstantFilter getReadAt() {
        return readAt;
    }

    public Optional<InstantFilter> optionalReadAt() {
        return Optional.ofNullable(readAt);
    }

    public InstantFilter readAt() {
        if (readAt == null) {
            setReadAt(new InstantFilter());
        }
        return readAt;
    }

    public void setReadAt(InstantFilter readAt) {
        this.readAt = readAt;
    }

    public StringFilter getRecipientId() {
        return recipientId;
    }

    public Optional<StringFilter> optionalRecipientId() {
        return Optional.ofNullable(recipientId);
    }

    public StringFilter recipientId() {
        if (recipientId == null) {
            setRecipientId(new StringFilter());
        }
        return recipientId;
    }

    public void setRecipientId(StringFilter recipientId) {
        this.recipientId = recipientId;
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
        final NotificationCriteria that = (NotificationCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(type, that.type) &&
            Objects.equals(title, that.title) &&
            Objects.equals(link, that.link) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(readAt, that.readAt) &&
            Objects.equals(recipientId, that.recipientId) &&
            Objects.equals(pageId, that.pageId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, title, link, createdAt, readAt, recipientId, pageId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NotificationCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalType().map(f -> "type=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalLink().map(f -> "link=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalReadAt().map(f -> "readAt=" + f + ", ").orElse("") +
            optionalRecipientId().map(f -> "recipientId=" + f + ", ").orElse("") +
            optionalPageId().map(f -> "pageId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
