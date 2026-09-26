package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * A Comment.
 */
@Entity
@Table(name = "comment")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Comment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Lob
    @Column(name = "body_md", nullable = false)
    private String bodyMd;

    /**
     * reserved for inline comments (v2)
     */
    @Size(max = 500)
    @Column(name = "anchor", length = 500)
    private String anchor;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "parent")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "replieses", "author", "page", "parent" }, allowSetters = true)
    private Set<Comment> replieses = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    private User author;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = { "childrens", "versionses", "attachmentses", "commentses", "currentVersion", "labelses", "space", "parent" },
        allowSetters = true
    )
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "replieses", "author", "page", "parent" }, allowSetters = true)
    private Comment parent;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Comment id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBodyMd() {
        return this.bodyMd;
    }

    public Comment bodyMd(String bodyMd) {
        this.setBodyMd(bodyMd);
        return this;
    }

    public void setBodyMd(String bodyMd) {
        this.bodyMd = bodyMd;
    }

    public String getAnchor() {
        return this.anchor;
    }

    public Comment anchor(String anchor) {
        this.setAnchor(anchor);
        return this;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Comment createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public Comment updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getResolvedAt() {
        return this.resolvedAt;
    }

    public Comment resolvedAt(Instant resolvedAt) {
        this.setResolvedAt(resolvedAt);
        return this;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getDeletedAt() {
        return this.deletedAt;
    }

    public Comment deletedAt(Instant deletedAt) {
        this.setDeletedAt(deletedAt);
        return this;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Set<Comment> getReplieses() {
        return this.replieses;
    }

    public void setReplieses(Set<Comment> comments) {
        if (this.replieses != null) {
            this.replieses.forEach(i -> i.setParent(null));
        }
        if (comments != null) {
            comments.forEach(i -> i.setParent(this));
        }
        this.replieses = comments;
    }

    public Comment replieses(Set<Comment> comments) {
        this.setReplieses(comments);
        return this;
    }

    public Comment addReplies(Comment comment) {
        this.replieses.add(comment);
        comment.setParent(this);
        return this;
    }

    public Comment removeReplies(Comment comment) {
        this.replieses.remove(comment);
        comment.setParent(null);
        return this;
    }

    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User user) {
        this.author = user;
    }

    public Comment author(User user) {
        this.setAuthor(user);
        return this;
    }

    public Page getPage() {
        return this.page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public Comment page(Page page) {
        this.setPage(page);
        return this;
    }

    public Comment getParent() {
        return this.parent;
    }

    public void setParent(Comment comment) {
        this.parent = comment;
    }

    public Comment parent(Comment comment) {
        this.setParent(comment);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Comment)) {
            return false;
        }
        return getId() != null && getId().equals(((Comment) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Comment{" +
            "id=" + getId() +
            ", bodyMd='" + getBodyMd() + "'" +
            ", anchor='" + getAnchor() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", resolvedAt='" + getResolvedAt() + "'" +
            ", deletedAt='" + getDeletedAt() + "'" +
            "}";
    }
}
