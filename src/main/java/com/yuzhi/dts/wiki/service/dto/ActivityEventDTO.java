package com.yuzhi.dts.wiki.service.dto;

import com.yuzhi.dts.wiki.domain.enumeration.ActivityType;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.ActivityEvent} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ActivityEventDTO implements Serializable {

    private Long id;

    @NotNull
    private ActivityType type;

    @NotNull
    @Size(max = 100)
    private String actorName;

    @Size(max = 200)
    private String targetTitle;

    @Lob
    private String detail;

    @NotNull
    private Instant createdAt;

    @NotNull
    private SpaceDTO space;

    private PageDTO page;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public SpaceDTO getSpace() {
        return space;
    }

    public void setSpace(SpaceDTO space) {
        this.space = space;
    }

    public PageDTO getPage() {
        return page;
    }

    public void setPage(PageDTO page) {
        this.page = page;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActivityEventDTO)) {
            return false;
        }

        ActivityEventDTO activityEventDTO = (ActivityEventDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, activityEventDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ActivityEventDTO{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", actorName='" + getActorName() + "'" +
            ", targetTitle='" + getTargetTitle() + "'" +
            ", detail='" + getDetail() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", space=" + getSpace() +
            ", page=" + getPage() +
            "}";
    }
}
