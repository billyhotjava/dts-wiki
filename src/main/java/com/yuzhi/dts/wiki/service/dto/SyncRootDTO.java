package com.yuzhi.dts.wiki.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.SyncRoot} entity.
 */
@Schema(description = "A directory of the space's git repository mounted into the page tree")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SyncRootDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 255)
    @Schema(description = "path inside the repository, e.g. \"worklog\"", requiredMode = Schema.RequiredMode.REQUIRED)
    private String repoPath;

    @NotNull
    private Boolean enabled;

    @Schema(description = "mount point of the SyncRoot inside the space's page tree (a FOLDER page)")
    private PageDTO mountPage;

    @NotNull
    private SpaceDTO space;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public PageDTO getMountPage() {
        return mountPage;
    }

    public void setMountPage(PageDTO mountPage) {
        this.mountPage = mountPage;
    }

    public SpaceDTO getSpace() {
        return space;
    }

    public void setSpace(SpaceDTO space) {
        this.space = space;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncRootDTO)) {
            return false;
        }

        SyncRootDTO syncRootDTO = (SyncRootDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, syncRootDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SyncRootDTO{" +
            "id=" + getId() +
            ", repoPath='" + getRepoPath() + "'" +
            ", enabled='" + getEnabled() + "'" +
            ", mountPage=" + getMountPage() +
            ", space=" + getSpace() +
            "}";
    }
}
