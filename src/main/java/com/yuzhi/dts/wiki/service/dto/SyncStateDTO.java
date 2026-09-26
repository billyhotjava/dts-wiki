package com.yuzhi.dts.wiki.service.dto;

import com.yuzhi.dts.wiki.domain.enumeration.SyncRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.yuzhi.dts.wiki.domain.SyncState} entity.
 */
@Schema(description = "Sync progress of one SyncRoot")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SyncStateDTO implements Serializable {

    private Long id;

    @Size(max = 40)
    private String lastSyncedCommit;

    private Instant lastFetchAt;

    private Instant lastPushAt;

    @NotNull
    private SyncRunStatus status;

    @Lob
    private String message;

    @NotNull
    private SyncRootDTO syncRoot;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLastSyncedCommit() {
        return lastSyncedCommit;
    }

    public void setLastSyncedCommit(String lastSyncedCommit) {
        this.lastSyncedCommit = lastSyncedCommit;
    }

    public Instant getLastFetchAt() {
        return lastFetchAt;
    }

    public void setLastFetchAt(Instant lastFetchAt) {
        this.lastFetchAt = lastFetchAt;
    }

    public Instant getLastPushAt() {
        return lastPushAt;
    }

    public void setLastPushAt(Instant lastPushAt) {
        this.lastPushAt = lastPushAt;
    }

    public SyncRunStatus getStatus() {
        return status;
    }

    public void setStatus(SyncRunStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SyncRootDTO getSyncRoot() {
        return syncRoot;
    }

    public void setSyncRoot(SyncRootDTO syncRoot) {
        this.syncRoot = syncRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncStateDTO)) {
            return false;
        }

        SyncStateDTO syncStateDTO = (SyncStateDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, syncStateDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SyncStateDTO{" +
            "id=" + getId() +
            ", lastSyncedCommit='" + getLastSyncedCommit() + "'" +
            ", lastFetchAt='" + getLastFetchAt() + "'" +
            ", lastPushAt='" + getLastPushAt() + "'" +
            ", status='" + getStatus() + "'" +
            ", message='" + getMessage() + "'" +
            ", syncRoot=" + getSyncRoot() +
            "}";
    }
}
