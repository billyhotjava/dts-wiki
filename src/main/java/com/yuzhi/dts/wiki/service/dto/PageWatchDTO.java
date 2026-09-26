package com.yuzhi.dts.wiki.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.PageWatch} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PageWatchDTO implements Serializable {

    private Long id;

    @NotNull
    private Instant createdAt;

    @NotNull
    private PageDTO page;

    @NotNull
    private UserDTO user;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public PageDTO getPage() {
        return page;
    }

    public void setPage(PageDTO page) {
        this.page = page;
    }

    public UserDTO getUser() {
        return user;
    }

    public void setUser(UserDTO user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageWatchDTO)) {
            return false;
        }

        PageWatchDTO pageWatchDTO = (PageWatchDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, pageWatchDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PageWatchDTO{" +
            "id=" + getId() +
            ", createdAt='" + getCreatedAt() + "'" +
            ", page=" + getPage() +
            ", user=" + getUser() +
            "}";
    }
}
