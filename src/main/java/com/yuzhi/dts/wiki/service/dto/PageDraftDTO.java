package com.yuzhi.dts.wiki.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.PageDraft} entity.
 */
@Schema(description = "Server-side autosaved draft (one per user and page)")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PageDraftDTO implements Serializable {

    private Long id;

    @Lob
    private String contentMd;

    @NotNull
    private Integer baseVersionNo;

    @NotNull
    private Instant updatedAt;

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

    public String getContentMd() {
        return contentMd;
    }

    public void setContentMd(String contentMd) {
        this.contentMd = contentMd;
    }

    public Integer getBaseVersionNo() {
        return baseVersionNo;
    }

    public void setBaseVersionNo(Integer baseVersionNo) {
        this.baseVersionNo = baseVersionNo;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
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
        if (!(o instanceof PageDraftDTO)) {
            return false;
        }

        PageDraftDTO pageDraftDTO = (PageDraftDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, pageDraftDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PageDraftDTO{" +
            "id=" + getId() +
            ", contentMd='" + getContentMd() + "'" +
            ", baseVersionNo=" + getBaseVersionNo() +
            ", updatedAt='" + getUpdatedAt() + "'" +
            ", page=" + getPage() +
            ", user=" + getUser() +
            "}";
    }
}
