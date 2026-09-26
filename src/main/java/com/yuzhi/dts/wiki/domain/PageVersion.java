package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yuzhi.dts.wiki.domain.enumeration.VersionSource;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Immutable full-content snapshot of a page
 */
@Entity
@Table(name = "page_version")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PageVersion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Min(value = 1)
    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Lob
    @Column(name = "content_md", nullable = false)
    private String contentMd;

    @NotNull
    @Size(min = 64, max = 64)
    @Column(name = "content_sha_256", length = 64, nullable = false)
    private String contentSha256;

    /**
     * jhi_user login when authored in the wiki; git author name otherwise
     */
    @NotNull
    @Size(max = 100)
    @Column(name = "author_name", length = 100, nullable = false)
    private String authorName;

    @Size(max = 254)
    @Column(name = "author_email", length = 254)
    private String authorEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private VersionSource source;

    @Size(max = 40)
    @Column(name = "git_commit", length = 40)
    private String gitCommit;

    @Size(max = 500)
    @Column(name = "message", length = 500)
    private String message;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = { "childrens", "versionses", "attachmentses", "commentses", "currentVersion", "labelses", "space", "parent" },
        allowSetters = true
    )
    private Page page;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public PageVersion id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersionNo() {
        return this.versionNo;
    }

    public PageVersion versionNo(Integer versionNo) {
        this.setVersionNo(versionNo);
        return this;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getContentMd() {
        return this.contentMd;
    }

    public PageVersion contentMd(String contentMd) {
        this.setContentMd(contentMd);
        return this;
    }

    public void setContentMd(String contentMd) {
        this.contentMd = contentMd;
    }

    public String getContentSha256() {
        return this.contentSha256;
    }

    public PageVersion contentSha256(String contentSha256) {
        this.setContentSha256(contentSha256);
        return this;
    }

    public void setContentSha256(String contentSha256) {
        this.contentSha256 = contentSha256;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public PageVersion authorName(String authorName) {
        this.setAuthorName(authorName);
        return this;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return this.authorEmail;
    }

    public PageVersion authorEmail(String authorEmail) {
        this.setAuthorEmail(authorEmail);
        return this;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public VersionSource getSource() {
        return this.source;
    }

    public PageVersion source(VersionSource source) {
        this.setSource(source);
        return this;
    }

    public void setSource(VersionSource source) {
        this.source = source;
    }

    public String getGitCommit() {
        return this.gitCommit;
    }

    public PageVersion gitCommit(String gitCommit) {
        this.setGitCommit(gitCommit);
        return this;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public String getMessage() {
        return this.message;
    }

    public PageVersion message(String message) {
        this.setMessage(message);
        return this;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public PageVersion createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User user) {
        this.author = user;
    }

    public PageVersion author(User user) {
        this.setAuthor(user);
        return this;
    }

    public Page getPage() {
        return this.page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public PageVersion page(Page page) {
        this.setPage(page);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageVersion)) {
            return false;
        }
        return getId() != null && getId().equals(((PageVersion) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PageVersion{" +
            "id=" + getId() +
            ", versionNo=" + getVersionNo() +
            ", contentMd='" + getContentMd() + "'" +
            ", contentSha256='" + getContentSha256() + "'" +
            ", authorName='" + getAuthorName() + "'" +
            ", authorEmail='" + getAuthorEmail() + "'" +
            ", source='" + getSource() + "'" +
            ", gitCommit='" + getGitCommit() + "'" +
            ", message='" + getMessage() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
