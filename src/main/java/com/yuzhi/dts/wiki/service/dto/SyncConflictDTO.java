package com.yuzhi.dts.wiki.service.dto;

import com.yuzhi.dts.wiki.domain.enumeration.ConflictResolution;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.SyncConflict} entity.
 */
@Schema(description = "Both sides changed a GIT page since the last sync point")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SyncConflictDTO implements Serializable {

    private Long id;

    @Lob
    private String gitContentMd;

    @NotNull
    @Size(max = 40)
    private String gitCommit;

    @NotNull
    private Instant detectedAt;

    private Instant resolvedAt;

    private ConflictResolution resolution;

    @NotNull
    private PageDTO page;

    private PageVersionDTO baseVersion;

    @NotNull
    private PageVersionDTO wikiVersion;

    private UserDTO resolvedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGitContentMd() {
        return gitContentMd;
    }

    public void setGitContentMd(String gitContentMd) {
        this.gitContentMd = gitContentMd;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public ConflictResolution getResolution() {
        return resolution;
    }

    public void setResolution(ConflictResolution resolution) {
        this.resolution = resolution;
    }

    public PageDTO getPage() {
        return page;
    }

    public void setPage(PageDTO page) {
        this.page = page;
    }

    public PageVersionDTO getBaseVersion() {
        return baseVersion;
    }

    public void setBaseVersion(PageVersionDTO baseVersion) {
        this.baseVersion = baseVersion;
    }

    public PageVersionDTO getWikiVersion() {
        return wikiVersion;
    }

    public void setWikiVersion(PageVersionDTO wikiVersion) {
        this.wikiVersion = wikiVersion;
    }

    public UserDTO getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(UserDTO resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncConflictDTO)) {
            return false;
        }

        SyncConflictDTO syncConflictDTO = (SyncConflictDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, syncConflictDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SyncConflictDTO{" +
            "id=" + getId() +
            ", gitContentMd='" + getGitContentMd() + "'" +
            ", gitCommit='" + getGitCommit() + "'" +
            ", detectedAt='" + getDetectedAt() + "'" +
            ", resolvedAt='" + getResolvedAt() + "'" +
            ", resolution='" + getResolution() + "'" +
            ", page=" + getPage() +
            ", baseVersion=" + getBaseVersion() +
            ", wikiVersion=" + getWikiVersion() +
            ", resolvedBy=" + getResolvedBy() +
            "}";
    }
}
