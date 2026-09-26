package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A node of the page tree
 */
@Entity
@Table(name = "page")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Page implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 200)
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private PageKind kind;

    /**
     * repository-relative path for GIT/FOLDER pages under a SyncRoot, e.g. worklog/v1.0.0/README.md
     */
    @Size(max = 1024)
    @Column(name = "git_path", length = 1024)
    private String gitPath;

    @NotNull
    @Column(name = "position", nullable = false)
    private Integer position;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false)
    private PageSyncStatus syncStatus;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(
        value = { "childrens", "versionses", "attachmentses", "commentses", "currentVersion", "labelses", "space", "parent" },
        allowSetters = true
    )
    private Set<Page> childrens = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "page")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "author", "page" }, allowSetters = true)
    private Set<PageVersion> versionses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "page")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "page" }, allowSetters = true)
    private Set<Attachment> attachmentses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "page")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "replieses", "author", "page", "parent" }, allowSetters = true)
    private Set<Comment> commentses = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "author", "page" }, allowSetters = true)
    private PageVersion currentVersion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "rel_page__labels", joinColumns = @JoinColumn(name = "page_id"), inverseJoinColumns = @JoinColumn(name = "labels_id"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "pageses" }, allowSetters = true)
    private Set<Label> labelses = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "syncRootses", "pageses" }, allowSetters = true)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(
        value = { "childrens", "versionses", "attachmentses", "commentses", "currentVersion", "labelses", "space", "parent" },
        allowSetters = true
    )
    private Page parent;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Page id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Page title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PageKind getKind() {
        return this.kind;
    }

    public Page kind(PageKind kind) {
        this.setKind(kind);
        return this;
    }

    public void setKind(PageKind kind) {
        this.kind = kind;
    }

    public String getGitPath() {
        return this.gitPath;
    }

    public Page gitPath(String gitPath) {
        this.setGitPath(gitPath);
        return this;
    }

    public void setGitPath(String gitPath) {
        this.gitPath = gitPath;
    }

    public Integer getPosition() {
        return this.position;
    }

    public Page position(Integer position) {
        this.setPosition(position);
        return this;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public PageSyncStatus getSyncStatus() {
        return this.syncStatus;
    }

    public Page syncStatus(PageSyncStatus syncStatus) {
        this.setSyncStatus(syncStatus);
        return this;
    }

    public void setSyncStatus(PageSyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Page createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Page updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return this.deletedAt;
    }

    public Page deletedAt(Instant deletedAt) {
        this.setDeletedAt(deletedAt);
        return this;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Set<Page> getChildrens() {
        return this.childrens;
    }

    public void setChildrens(Set<Page> pages) {
        if (this.childrens != null) {
            this.childrens.forEach(i -> i.setParent(null));
        }
        if (pages != null) {
            pages.forEach(i -> i.setParent(this));
        }
        this.childrens = pages;
    }

    public Page childrens(Set<Page> pages) {
        this.setChildrens(pages);
        return this;
    }

    public Page addChildren(Page page) {
        this.childrens.add(page);
        page.setParent(this);
        return this;
    }

    public Page removeChildren(Page page) {
        this.childrens.remove(page);
        page.setParent(null);
        return this;
    }

    public Set<PageVersion> getVersionses() {
        return this.versionses;
    }

    public void setVersionses(Set<PageVersion> pageVersions) {
        if (this.versionses != null) {
            this.versionses.forEach(i -> i.setPage(null));
        }
        if (pageVersions != null) {
            pageVersions.forEach(i -> i.setPage(this));
        }
        this.versionses = pageVersions;
    }

    public Page versionses(Set<PageVersion> pageVersions) {
        this.setVersionses(pageVersions);
        return this;
    }

    public Page addVersions(PageVersion pageVersion) {
        this.versionses.add(pageVersion);
        pageVersion.setPage(this);
        return this;
    }

    public Page removeVersions(PageVersion pageVersion) {
        this.versionses.remove(pageVersion);
        pageVersion.setPage(null);
        return this;
    }

    public Set<Attachment> getAttachmentses() {
        return this.attachmentses;
    }

    public void setAttachmentses(Set<Attachment> attachments) {
        if (this.attachmentses != null) {
            this.attachmentses.forEach(i -> i.setPage(null));
        }
        if (attachments != null) {
            attachments.forEach(i -> i.setPage(this));
        }
        this.attachmentses = attachments;
    }

    public Page attachmentses(Set<Attachment> attachments) {
        this.setAttachmentses(attachments);
        return this;
    }

    public Page addAttachments(Attachment attachment) {
        this.attachmentses.add(attachment);
        attachment.setPage(this);
        return this;
    }

    public Page removeAttachments(Attachment attachment) {
        this.attachmentses.remove(attachment);
        attachment.setPage(null);
        return this;
    }

    public Set<Comment> getCommentses() {
        return this.commentses;
    }

    public void setCommentses(Set<Comment> comments) {
        if (this.commentses != null) {
            this.commentses.forEach(i -> i.setPage(null));
        }
        if (comments != null) {
            comments.forEach(i -> i.setPage(this));
        }
        this.commentses = comments;
    }

    public Page commentses(Set<Comment> comments) {
        this.setCommentses(comments);
        return this;
    }

    public Page addComments(Comment comment) {
        this.commentses.add(comment);
        comment.setPage(this);
        return this;
    }

    public Page removeComments(Comment comment) {
        this.commentses.remove(comment);
        comment.setPage(null);
        return this;
    }

    public PageVersion getCurrentVersion() {
        return this.currentVersion;
    }

    public void setCurrentVersion(PageVersion pageVersion) {
        this.currentVersion = pageVersion;
    }

    public Page currentVersion(PageVersion pageVersion) {
        this.setCurrentVersion(pageVersion);
        return this;
    }

    public Set<Label> getLabelses() {
        return this.labelses;
    }

    public void setLabelses(Set<Label> labels) {
        this.labelses = labels;
    }

    public Page labelses(Set<Label> labels) {
        this.setLabelses(labels);
        return this;
    }

    public Page addLabels(Label label) {
        this.labelses.add(label);
        return this;
    }

    public Page removeLabels(Label label) {
        this.labelses.remove(label);
        return this;
    }

    public Space getSpace() {
        return this.space;
    }

    public void setSpace(Space space) {
        this.space = space;
    }

    public Page space(Space space) {
        this.setSpace(space);
        return this;
    }

    public Page getParent() {
        return this.parent;
    }

    public void setParent(Page page) {
        this.parent = page;
    }

    public Page parent(Page page) {
        this.setParent(page);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Page)) {
            return false;
        }
        return getId() != null && getId().equals(((Page) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Page{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", kind='" + getKind() + "'" +
            ", gitPath='" + getGitPath() + "'" +
            ", position=" + getPosition() +
            ", syncStatus='" + getSyncStatus() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", deletedAt='" + getDeletedAt() + "'" +
            "}";
    }
}
