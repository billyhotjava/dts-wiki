package com.yuzhi.dts.wiki.service.criteria;

import com.yuzhi.dts.wiki.domain.enumeration.VersionSource;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.yuzhi.dts.wiki.domain.PageVersion} entity. This class is used
 * in {@link com.yuzhi.dts.wiki.web.rest.PageVersionResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /page-versions?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PageVersionCriteria implements Serializable, Criteria {

    /**
     * Class for filtering VersionSource
     */
    public static class VersionSourceFilter extends Filter<VersionSource> {

        public VersionSourceFilter() {}

        public VersionSourceFilter(VersionSourceFilter filter) {
            super(filter);
        }

        @Override
        public VersionSourceFilter copy() {
            return new VersionSourceFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private IntegerFilter versionNo;

    private StringFilter contentSha256;

    private StringFilter authorName;

    private StringFilter authorEmail;

    private VersionSourceFilter source;

    private StringFilter gitCommit;

    private StringFilter message;

    private InstantFilter createdAt;

    private StringFilter authorId;

    private LongFilter pageId;

    private Boolean distinct;

    public PageVersionCriteria() {}

    public PageVersionCriteria(PageVersionCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.versionNo = other.optionalVersionNo().map(IntegerFilter::copy).orElse(null);
        this.contentSha256 = other.optionalContentSha256().map(StringFilter::copy).orElse(null);
        this.authorName = other.optionalAuthorName().map(StringFilter::copy).orElse(null);
        this.authorEmail = other.optionalAuthorEmail().map(StringFilter::copy).orElse(null);
        this.source = other.optionalSource().map(VersionSourceFilter::copy).orElse(null);
        this.gitCommit = other.optionalGitCommit().map(StringFilter::copy).orElse(null);
        this.message = other.optionalMessage().map(StringFilter::copy).orElse(null);
        this.createdAt = other.optionalCreatedAt().map(InstantFilter::copy).orElse(null);
        this.authorId = other.optionalAuthorId().map(StringFilter::copy).orElse(null);
        this.pageId = other.optionalPageId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public PageVersionCriteria copy() {
        return new PageVersionCriteria(this);
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

    public IntegerFilter getVersionNo() {
        return versionNo;
    }

    public Optional<IntegerFilter> optionalVersionNo() {
        return Optional.ofNullable(versionNo);
    }

    public IntegerFilter versionNo() {
        if (versionNo == null) {
            setVersionNo(new IntegerFilter());
        }
        return versionNo;
    }

    public void setVersionNo(IntegerFilter versionNo) {
        this.versionNo = versionNo;
    }

    public StringFilter getContentSha256() {
        return contentSha256;
    }

    public Optional<StringFilter> optionalContentSha256() {
        return Optional.ofNullable(contentSha256);
    }

    public StringFilter contentSha256() {
        if (contentSha256 == null) {
            setContentSha256(new StringFilter());
        }
        return contentSha256;
    }

    public void setContentSha256(StringFilter contentSha256) {
        this.contentSha256 = contentSha256;
    }

    public StringFilter getAuthorName() {
        return authorName;
    }

    public Optional<StringFilter> optionalAuthorName() {
        return Optional.ofNullable(authorName);
    }

    public StringFilter authorName() {
        if (authorName == null) {
            setAuthorName(new StringFilter());
        }
        return authorName;
    }

    public void setAuthorName(StringFilter authorName) {
        this.authorName = authorName;
    }

    public StringFilter getAuthorEmail() {
        return authorEmail;
    }

    public Optional<StringFilter> optionalAuthorEmail() {
        return Optional.ofNullable(authorEmail);
    }

    public StringFilter authorEmail() {
        if (authorEmail == null) {
            setAuthorEmail(new StringFilter());
        }
        return authorEmail;
    }

    public void setAuthorEmail(StringFilter authorEmail) {
        this.authorEmail = authorEmail;
    }

    public VersionSourceFilter getSource() {
        return source;
    }

    public Optional<VersionSourceFilter> optionalSource() {
        return Optional.ofNullable(source);
    }

    public VersionSourceFilter source() {
        if (source == null) {
            setSource(new VersionSourceFilter());
        }
        return source;
    }

    public void setSource(VersionSourceFilter source) {
        this.source = source;
    }

    public StringFilter getGitCommit() {
        return gitCommit;
    }

    public Optional<StringFilter> optionalGitCommit() {
        return Optional.ofNullable(gitCommit);
    }

    public StringFilter gitCommit() {
        if (gitCommit == null) {
            setGitCommit(new StringFilter());
        }
        return gitCommit;
    }

    public void setGitCommit(StringFilter gitCommit) {
        this.gitCommit = gitCommit;
    }

    public StringFilter getMessage() {
        return message;
    }

    public Optional<StringFilter> optionalMessage() {
        return Optional.ofNullable(message);
    }

    public StringFilter message() {
        if (message == null) {
            setMessage(new StringFilter());
        }
        return message;
    }

    public void setMessage(StringFilter message) {
        this.message = message;
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

    public StringFilter getAuthorId() {
        return authorId;
    }

    public Optional<StringFilter> optionalAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public StringFilter authorId() {
        if (authorId == null) {
            setAuthorId(new StringFilter());
        }
        return authorId;
    }

    public void setAuthorId(StringFilter authorId) {
        this.authorId = authorId;
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
        final PageVersionCriteria that = (PageVersionCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(versionNo, that.versionNo) &&
            Objects.equals(contentSha256, that.contentSha256) &&
            Objects.equals(authorName, that.authorName) &&
            Objects.equals(authorEmail, that.authorEmail) &&
            Objects.equals(source, that.source) &&
            Objects.equals(gitCommit, that.gitCommit) &&
            Objects.equals(message, that.message) &&
            Objects.equals(createdAt, that.createdAt) &&
            Objects.equals(authorId, that.authorId) &&
            Objects.equals(pageId, that.pageId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            id,
            versionNo,
            contentSha256,
            authorName,
            authorEmail,
            source,
            gitCommit,
            message,
            createdAt,
            authorId,
            pageId,
            distinct
        );
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PageVersionCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalVersionNo().map(f -> "versionNo=" + f + ", ").orElse("") +
            optionalContentSha256().map(f -> "contentSha256=" + f + ", ").orElse("") +
            optionalAuthorName().map(f -> "authorName=" + f + ", ").orElse("") +
            optionalAuthorEmail().map(f -> "authorEmail=" + f + ", ").orElse("") +
            optionalSource().map(f -> "source=" + f + ", ").orElse("") +
            optionalGitCommit().map(f -> "gitCommit=" + f + ", ").orElse("") +
            optionalMessage().map(f -> "message=" + f + ", ").orElse("") +
            optionalCreatedAt().map(f -> "createdAt=" + f + ", ").orElse("") +
            optionalAuthorId().map(f -> "authorId=" + f + ", ").orElse("") +
            optionalPageId().map(f -> "pageId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
