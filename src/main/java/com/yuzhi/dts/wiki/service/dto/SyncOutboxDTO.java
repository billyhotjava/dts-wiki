package com.yuzhi.dts.wiki.service.dto;

import com.yuzhi.dts.wiki.domain.enumeration.OutboxOp;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.SyncOutbox} entity.
 */
@Schema(description = "Outbound (wiki -> git) work item, written in the same transaction as the page change")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SyncOutboxDTO implements Serializable {

    private Long id;

    @NotNull
    private OutboxOp op;

    @Schema(description = "JSON: {fromPath,toPath,versionId,attachmentId,...}", requiredMode = Schema.RequiredMode.REQUIRED)
    @Lob
    private String payload;

    @NotNull
    @Size(max = 50)
    private String actorLogin;

    @Size(max = 100)
    private String actorName;

    @Size(max = 254)
    private String actorEmail;

    @NotNull
    private OutboxStatus status;

    @NotNull
    private Integer attempts;

    @Lob
    private String lastError;

    @NotNull
    private Instant createdAt;

    private Instant processedAt;

    @NotNull
    private SpaceDTO space;

    private PageDTO page;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OutboxOp getOp() {
        return op;
    }

    public void setOp(OutboxOp op) {
        this.op = op;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getActorLogin() {
        return actorLogin;
    }

    public void setActorLogin(String actorLogin) {
        this.actorLogin = actorLogin;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
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
        if (!(o instanceof SyncOutboxDTO)) {
            return false;
        }

        SyncOutboxDTO syncOutboxDTO = (SyncOutboxDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, syncOutboxDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SyncOutboxDTO{" +
            "id=" + getId() +
            ", op='" + getOp() + "'" +
            ", payload='" + getPayload() + "'" +
            ", actorLogin='" + getActorLogin() + "'" +
            ", actorName='" + getActorName() + "'" +
            ", actorEmail='" + getActorEmail() + "'" +
            ", status='" + getStatus() + "'" +
            ", attempts=" + getAttempts() +
            ", lastError='" + getLastError() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", processedAt='" + getProcessedAt() + "'" +
            ", space=" + getSpace() +
            ", page=" + getPage() +
            "}";
    }
}
