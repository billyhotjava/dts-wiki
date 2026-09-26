package com.yuzhi.dts.wiki.service.dto;

import com.yuzhi.dts.wiki.domain.enumeration.PageKind;
import com.yuzhi.dts.wiki.domain.enumeration.PageSyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.Page} entity.
 */
@Schema(description = "A node of the page tree")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PageDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 200)
    private String title;

    @NotNull
    private PageKind kind;

    @Size(max = 1024)
    @Schema(description = "repository-relative path for GIT/FOLDER pages under a SyncRoot, e.g. worklog/v1.0.0/README.md")
    private String gitPath;

    @NotNull
    private Integer position;

    @NotNull
    private PageSyncStatus syncStatus;

    @NotNull
    private Instant createdAt;

    @NotNull
    private Instant updatedAt;

    private Instant deletedAt;

    private PageVersionDTO currentVersion;

    private Set<LabelDTO> labelses = new HashSet<>();

    @NotNull
    private SpaceDTO space;

    private PageDTO parent;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PageKind getKind() {
        return kind;
    }

    public void setKind(PageKind kind) {
        this.kind = kind;
    }

    public String getGitPath() {
        return gitPath;
    }

    public void setGitPath(String gitPath) {
        this.gitPath = gitPath;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public PageSyncStatus getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(PageSyncStatus syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public PageVersionDTO getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(PageVersionDTO currentVersion) {
        this.currentVersion = currentVersion;
    }

    public Set<LabelDTO> getLabelses() {
        return labelses;
    }

    public void setLabelses(Set<LabelDTO> labelses) {
        this.labelses = labelses;
    }

    public SpaceDTO getSpace() {
        return space;
    }

    public void setSpace(SpaceDTO space) {
        this.space = space;
    }

    public PageDTO getParent() {
        return parent;
    }

    public void setParent(PageDTO parent) {
        this.parent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageDTO)) {
            return false;
        }

        PageDTO pageDTO = (PageDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, pageDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PageDTO{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", kind='" + getKind() + "'" +
            ", gitPath='" + getGitPath() + "'" +
            ", position=" + getPosition() +
            ", syncStatus='" + getSyncStatus() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", deletedAt='" + getDeletedAt() + "'" +
            ", currentVersion=" + getCurrentVersion() +
            ", labelses=" + getLabelses() +
            ", space=" + getSpace() +
            ", parent=" + getParent() +
            "}";
    }
}
