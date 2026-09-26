package com.yuzhi.dts.wiki.service.dto;

import com.yuzhi.dts.wiki.domain.enumeration.VersionSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.PageVersion} entity.
 */
@Schema(description = "Immutable full-content snapshot of a page")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PageVersionDTO implements Serializable {

    private Long id;

    @NotNull
    @Min(value = 1)
    private Integer versionNo;

    @Lob
    private String contentMd;

    @NotNull
    @Size(min = 64, max = 64)
    private String contentSha256;

    @NotNull
    @Size(max = 100)
    @Schema(
        description = "jhi_user login when authored in the wiki; git author name otherwise",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String authorName;

    @Size(max = 254)
    private String authorEmail;

    @NotNull
    private VersionSource source;

    @Size(max = 40)
    private String gitCommit;

    @Size(max = 500)
    private String message;

    @NotNull
    private Instant createdAt;

    private UserDTO author;

    @NotNull
    private PageDTO page;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getContentMd() {
        return contentMd;
    }

    public void setContentMd(String contentMd) {
        this.contentMd = contentMd;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public void setContentSha256(String contentSha256) {
        this.contentSha256 = contentSha256;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public VersionSource getSource() {
        return source;
    }

    public void setSource(VersionSource source) {
        this.source = source;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UserDTO getAuthor() {
        return author;
    }

    public void setAuthor(UserDTO author) {
        this.author = author;
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
        if (!(o instanceof PageVersionDTO)) {
            return false;
        }

        PageVersionDTO pageVersionDTO = (PageVersionDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, pageVersionDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PageVersionDTO{" +
            "id=" + getId() +
            ", versionNo=" + getVersionNo() +
            ", contentMd='" + getContentMd() + "'" +
            ", contentSha256='" + getContentSha256() + "'" +
            ", authorName='" + getAuthorName() + "'" +
            ", authorEmail='" + getAuthorEmail() + "'" +
            ", source='" + getSource() + "'" +
            ", gitCommit='" + getGitCommit() + "'" +
            ", message='" + getMessage() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", author=" + getAuthor() +
            ", page=" + getPage() +
            "}";
    }
}
