package com.yuzhi.dts.wiki.service.criteria;

import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.yuzhi.dts.wiki.domain.Page} entity. This class is used
 * in {@link com.yuzhi.dts.wiki.web.rest.PageResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /pages?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PageCriteria implements Serializable, Criteria {

    /**
     * Class for filtering PageKind
     */
    public static class PageKindFilter extends Filter<PageKind> {

        public PageKindFilter() {}

        public PageKindFilter(PageKindFilter filter) {
            super(filter);
        }

        @Override
        public PageKindFilter copy() {
            return new PageKindFilter(this);
        }
    }

    /**
     * Class for filtering PageSyncStatus
     */
    public static class PageSyncStatusFilter extends Filter<PageSyncStatus> {

        public PageSyncStatusFilter() {}

        public PageSyncStatusFilter(PageSyncStatusFilter filter) {
            super(filter);
        }

        @Override
        public PageSyncStatusFilter copy() {
            return new PageSyncStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter title;

    private PageKindFilter kind;

    private StringFilter gitPath;

    private IntegerFilter position;

    private PageSyncStatusFilter syncStatus;

    private InstantFilter createdAt;

    private InstantFilter updatedAt;

    private InstantFilter deletedAt;

    private LongFilter childrenId;

    private LongFilter versionsId;

    private LongFilter attachmentsId;

    private LongFilter commentsId;

    private LongFilter currentVersionId;

    private LongFilter labelsId;

    private LongFilter spaceId;

    private LongFilter parentId;

    private Boolean distinct;

    public PageCriteria() {}

    public PageCriteria(PageCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.title = other.optionalTitle().map(StringFilter::copy).orElse(null);
        this.kind = other.optionalKind().map(PageKindFilter::copy).orElse(null);
        this.gitPath = other.optionalGitPath().map(StringFilter::copy).orElse(null);
        this.position = other.optionalPosition().map(IntegerFilter::copy).orElse(null);
        this.syncStatus = other.optionalSyncStatus().map(PageSyncStatusFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.updatedAt = other.optionalUpdatedAt().map(InstantFilter::copy).orElse(null);
        this.deletedAt = other.optionalDeletedAt().map(InstantFilter::copy).orElse(null);
        this.childrenId = other.optionalChildrenId().map(LongFilter::copy).orElse(null);
        this.versionsId = other.optionalVersionsId().map(LongFilter::copy).orElse(null);
        this.attachmentsId = other.optionalAttachmentsId().map(LongFilter::copy).orElse(null);
        this.commentsId = other.optionalCommentsId().map(LongFilter::copy).orElse(null);
        this.currentVersionId = other.optionalCurrentVersionId().map(LongFilter::copy).orElse(null);
        this.labelsId = other.optionalLabelsId().map(LongFilter::copy).orElse(null);
        this.spaceId = other.optionalSpaceId().map(LongFilter::copy).orElse(null);
        this.parentId = other.optionalParentId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public PageCriteria copy() {
        return new PageCriteria(this);
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

    public PageKindFilter getKind() {
        return kind;
    }

    public Optional<PageKindFilter> optionalKind() {
        return Optional.ofNullable(kind);
    }

    public PageKindFilter kind() {
        if (kind == null) {
            setKind(new PageKindFilter());
        }
        return kind;
    }

    public void setKind(PageKindFilter kind) {
        this.kind = kind;
    }

    public StringFilter getGitPath() {
        return gitPath;
    }

    public Optional<StringFilter> optionalGitPath() {
        return Optional.ofNullable(gitPath);
    }

    public StringFilter gitPath() {
        if (gitPath == null) {
            setGitPath(new StringFilter());
        }
        return gitPath;
    }

    public void setGitPath(StringFilter gitPath) {
        this.gitPath = gitPath;
    }

    public IntegerFilter getPosition() {
        return position;
    }

    public Optional<IntegerFilter> optionalPosition() {
        return Optional.ofNullable(position);
    }

    public IntegerFilter position() {
        if (position == null) {
            setPosition(new IntegerFilter());
        }
        return position;
    }

    public void setPosition(IntegerFilter position) {
        this.position = position;
    }

    public PageSyncStatusFilter getSyncStatus() {
        return syncStatus;
    }

    public Optional<PageSyncStatusFilter> optionalSyncStatus() {
        return Optional.ofNullable(syncStatus);
    }

    public PageSyncStatusFilter syncStatus() {
        if (syncStatus == null) {
            setSyncStatus(new PageSyncStatusFilter());
        }
        return syncStatus;
    }

    public void setSyncStatus(PageSyncStatusFilter syncStatus) {
        this.syncStatus = syncStatus;
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

    public InstantFilter getUpdatedAt() {
        return updatedAt;
    }

    public Optional<InstantFilter> optionalUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public InstantFilter updatedAt() {
        if (updatedAt == null) {
            setUpdatedAt(new InstantFilter());
        }
        return updatedAt;
    }

    public void setUpdatedAt(InstantFilter updatedAt) {
        this.updatedAt = updatedAt;
    }

    public InstantFilter getDeletedAt() {
        return deletedAt;
    }

    public Optional<InstantFilter> optionalDeletedAt() {
        return Optional.ofNullable(deletedAt);
    }

    public InstantFilter deletedAt() {
        if (deletedAt == null) {
            setDeletedAt(new InstantFilter());
        }
        return deletedAt;
    }

    public void setDeletedAt(InstantFilter deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LongFilter getChildrenId() {
        return childrenId;
    }

    public Optional<LongFilter> optionalChildrenId() {
        return Optional.ofNullable(childrenId);
    }

    public LongFilter childrenId() {
        if (childrenId == null) {
            setChildrenId(new LongFilter());
        }
        return childrenId;
    }

    public void setChildrenId(LongFilter childrenId) {
        this.childrenId = childrenId;
    }

    public LongFilter getVersionsId() {
        return versionsId;
    }

    public Optional<LongFilter> optionalVersionsId() {
        return Optional.ofNullable(versionsId);
    }

    public LongFilter versionsId() {
        if (versionsId == null) {
            setVersionsId(new LongFilter());
        }
        return versionsId;
    }

    public void setVersionsId(LongFilter versionsId) {
        this.versionsId = versionsId;
    }

    public LongFilter getAttachmentsId() {
        return attachmentsId;
    }

    public Optional<LongFilter> optionalAttachmentsId() {
        return Optional.ofNullable(attachmentsId);
    }

    public LongFilter attachmentsId() {
        if (attachmentsId == null) {
            setAttachmentsId(new LongFilter());
        }
        return attachmentsId;
    }

    public void setAttachmentsId(LongFilter attachmentsId) {
        this.attachmentsId = attachmentsId;
    }

    public LongFilter getCommentsId() {
        return commentsId;
    }

    public Optional<LongFilter> optionalCommentsId() {
        return Optional.ofNullable(commentsId);
    }

    public LongFilter commentsId() {
        if (commentsId == null) {
            setCommentsId(new LongFilter());
        }
        return commentsId;
    }

    public void setCommentsId(LongFilter commentsId) {
        this.commentsId = commentsId;
    }

    public LongFilter getCurrentVersionId() {
        return currentVersionId;
    }

    public Optional<LongFilter> optionalCurrentVersionId() {
        return Optional.ofNullable(currentVersionId);
    }

    public LongFilter currentVersionId() {
        if (currentVersionId == null) {
            setCurrentVersionId(new LongFilter());
        }
        return currentVersionId;
    }

    public void setCurrentVersionId(LongFilter currentVersionId) {
        this.currentVersionId = currentVersionId;
    }

    public LongFilter getLabelsId() {
        return labelsId;
    }

    public Optional<LongFilter> optionalLabelsId() {
        return Optional.ofNullable(labelsId);
    }

    public LongFilter labelsId() {
        if (labelsId == null) {
            setLabelsId(new LongFilter());
        }
        return labelsId;
    }

    public void setLabelsId(LongFilter labelsId) {
        this.labelsId = labelsId;
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

    public LongFilter getParentId() {
        return parentId;
    }

    public Optional<LongFilter> optionalParentId() {
        return Optional.ofNullable(parentId);
    }

    public LongFilter parentId() {
        if (parentId == null) {
            setParentId(new LongFilter());
        }
        return parentId;
    }

    public void setParentId(LongFilter parentId) {
        this.parentId = parentId;
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
        final PageCriteria that = (PageCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(title, that.title) &&
            Objects.equals(kind, that.kind) &&
            Objects.equals(gitPath, that.gitPath) &&
            Objects.equals(position, that.position) &&
            Objects.equals(syncStatus, that.syncStatus) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(updatedAt, that.updatedAt) &&
            Objects.equals(deletedAt, that.deletedAt) &&
            Objects.equals(childrenId, that.childrenId) &&
            Objects.equals(versionsId, that.versionsId) &&
            Objects.equals(attachmentsId, that.attachmentsId) &&
            Objects.equals(commentsId, that.commentsId) &&
            Objects.equals(currentVersionId, that.currentVersionId) &&
            Objects.equals(labelsId, that.labelsId) &&
            Objects.equals(spaceId, that.spaceId) &&
            Objects.equals(parentId, that.parentId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            title,
            kind,
            gitPath,
            position,
            syncStatus,
            createdAt,
            updatedAt,
            deletedAt,
            childrenId,
            versionsId,
            attachmentsId,
            commentsId,
            currentVersionId,
            labelsId,
            spaceId,
            parentId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PageCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalTitle().map(f -> "title=" + f + ", ").orElse("") +
            optionalKind().map(f -> "kind=" + f + ", ").orElse("") +
            optionalGitPath().map(f -> "gitPath=" + f + ", ").orElse("") +
            optionalPosition().map(f -> "position=" + f + ", ").orElse("") +
            optionalSyncStatus().map(f -> "syncStatus=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalUpdatedAt().map(f -> "updatedAt=" + f + ", ").orElse("") +
            optionalDeletedAt().map(f -> "deletedAt=" + f + ", ").orElse("") +
            optionalChildrenId().map(f -> "childrenId=" + f + ", ").orElse("") +
            optionalVersionsId().map(f -> "versionsId=" + f + ", ").orElse("") +
            optionalAttachmentsId().map(f -> "attachmentsId=" + f + ", ").orElse("") +
            optionalCommentsId().map(f -> "commentsId=" + f + ", ").orElse("") +
            optionalCurrentVersionId().map(f -> "currentVersionId=" + f + ", ").orElse("") +
            optionalLabelsId().map(f -> "labelsId=" + f + ", ").orElse("") +
            optionalSpaceId().map(f -> "spaceId=" + f + ", ").orElse("") +
            optionalParentId().map(f -> "parentId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
