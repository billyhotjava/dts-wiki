package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yuzhi.dts.wiki.domain.enumeration.ConflictResolution;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Both sides changed a GIT page since the last sync point
 */
@Entity
@Table(name = "sync_conflict")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SyncConflict implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    // DTS-WIKI: customized (W5b): @Lob removed, see PageVersion.contentMd.
    @Column(name = "git_content_md", nullable = false, columnDefinition = "TEXT")
    private String gitContentMd;

    @NotNull
    @Size(max = 40)
    @Column(name = "git_commit", length = 40, nullable = false)
    private String gitCommit;

    @NotNull
    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution")
    private ConflictResolution resolution;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = { "childrens", "versionses", "attachmentses", "commentses", "currentVersion", "labelses", "space", "parent" },
        allowSetters = true
    )
    private Page page;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "author", "page" }, allowSetters = true)
    private PageVersion baseVersion;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "author", "page" }, allowSetters = true)
    private PageVersion wikiVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    private User resolvedBy;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public SyncConflict id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGitContentMd() {
        return this.gitContentMd;
    }

    public SyncConflict gitContentMd(String gitContentMd) {
        this.setGitContentMd(gitContentMd);
        return this;
    }

    public void setGitContentMd(String gitContentMd) {
        this.gitContentMd = gitContentMd;
    }

    public String getGitCommit() {
        return this.gitCommit;
    }

    public SyncConflict gitCommit(String gitCommit) {
        this.setGitCommit(gitCommit);
        return this;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public Instant getDetectedAt() {
        return this.detectedAt;
    }

    public SyncConflict detectedAt(Instant detectedAt) {
        this.setDetectedAt(detectedAt);
        return this;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Instant getResolvedAt() {
        return this.resolvedAt;
    }

    public SyncConflict resolvedAt(Instant resolvedAt) {
        this.setResolvedAt(resolvedAt);
        return this;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public ConflictResolution getResolution() {
        return this.resolution;
    }

    public SyncConflict resolution(ConflictResolution resolution) {
        this.setResolution(resolution);
        return this;
    }

    public void setResolution(ConflictResolution resolution) {
        this.resolution = resolution;
    }

    public Page getPage() {
        return this.page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public SyncConflict page(Page page) {
        this.setPage(page);
        return this;
    }

    public PageVersion getBaseVersion() {
        return this.baseVersion;
    }

    public void setBaseVersion(PageVersion pageVersion) {
        this.baseVersion = pageVersion;
    }

    public SyncConflict baseVersion(PageVersion pageVersion) {
        this.setBaseVersion(pageVersion);
        return this;
    }

    public PageVersion getWikiVersion() {
        return this.wikiVersion;
    }

    public void setWikiVersion(PageVersion pageVersion) {
        this.wikiVersion = pageVersion;
    }

    public SyncConflict wikiVersion(PageVersion pageVersion) {
        this.setWikiVersion(pageVersion);
        return this;
    }

    public User getResolvedBy() {
        return this.resolvedBy;
    }

    public void setResolvedBy(User user) {
        this.resolvedBy = user;
    }

    public SyncConflict resolvedBy(User user) {
        this.setResolvedBy(user);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncConflict)) {
            return false;
        }
        return getId() != null && getId().equals(((SyncConflict) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SyncConflict{" +
            "id=" + getId() +
            ", gitContentMd='" + getGitContentMd() + "'" +
            ", gitCommit='" + getGitCommit() + "'" +
            ", detectedAt='" + getDetectedAt() + "'" +
            ", resolvedAt='" + getResolvedAt() + "'" +
            ", resolution='" + getResolution() + "'" +
            "}";
    }
}
