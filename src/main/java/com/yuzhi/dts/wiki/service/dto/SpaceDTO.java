package com.yuzhi.dts.wiki.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.Space} entity.
 */
@Schema(description = "产品空间")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SpaceDTO implements Serializable {

    private Long id;

    @NotNull
    @Pattern(regexp = "^[a-z][a-z0-9-]{1,30}$")
    private String slug;

    @NotNull
    @Size(max = 40)
    private String name;

    @Size(max = 200)
    private String description;

    @Size(max = 255)
    @Schema(description = "SSH URL, e.g. git@github.com:billyhotjava/prs-stack.git ; null = no git sync")
    private String gitRepoUrl;

    @Size(max = 100)
    private String gitBranch;

    private Integer position;

    @NotNull
    private Boolean archived;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGitRepoUrl() {
        return gitRepoUrl;
    }

    public void setGitRepoUrl(String gitRepoUrl) {
        this.gitRepoUrl = gitRepoUrl;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpaceDTO)) {
            return false;
        }

        SpaceDTO spaceDTO = (SpaceDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, spaceDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SpaceDTO{" +
            "id=" + getId() +
            ", slug='" + getSlug() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", gitRepoUrl='" + getGitRepoUrl() + "'" +
            ", gitBranch='" + getGitBranch() + "'" +
            ", position=" + getPosition() +
            ", archived='" + getArchived() + "'" +
            "}";
    }
}
