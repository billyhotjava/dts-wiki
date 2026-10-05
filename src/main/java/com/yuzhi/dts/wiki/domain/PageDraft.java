package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Server-side autosaved draft (one per user and page)
 */
@Entity
@Table(name = "page_draft")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PageDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    // DTS-WIKI: customized (W5b): @Lob removed, see PageVersion.contentMd.
    @Column(name = "content_md", nullable = false, columnDefinition = "TEXT")
    private String contentMd;

    @NotNull
    @Column(name = "base_version_no", nullable = false)
    private Integer baseVersionNo;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = { "childrens", "versionses", "attachmentses", "commentses", "currentVersion", "labelses", "space", "parent" },
        allowSetters = true
    )
    private Page page;

    @ManyToOne(optional = false)
    @NotNull
    private User user;


    public Long getId() {
        return this.id;
    }

    public PageDraft id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContentMd() {
        return this.contentMd;
    }

    public PageDraft contentMd(String contentMd) {
        this.setContentMd(contentMd);
        return this;
    }

    public void setContentMd(String contentMd) {
        this.contentMd = contentMd;
    }

    public Integer getBaseVersionNo() {
        return this.baseVersionNo;
    }

    public PageDraft baseVersionNo(Integer baseVersionNo) {
        this.setBaseVersionNo(baseVersionNo);
        return this;
    }

    public void setBaseVersionNo(Integer baseVersionNo) {
        this.baseVersionNo = baseVersionNo;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public PageDraft updatedAt(Instant updatedAt) {
        this.setUpdatedAt(updatedAt);
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Page getPage() {
        return this.page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public PageDraft page(Page page) {
        this.setPage(page);
        return this;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PageDraft user(User user) {
        this.setUser(user);
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageDraft)) {
            return false;
        }
        return getId() != null && getId().equals(((PageDraft) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PageDraft{" +
            "id=" + getId() +
            ", contentMd='" + getContentMd() + "'" +
            ", baseVersionNo=" + getBaseVersionNo() +
            ", updatedAt='" + getUpdatedAt() + "'" +
            "}";
    }
}
