package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yuzhi.dts.wiki.domain.enumeration.SyncRunStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Sync progress of one SyncRoot
 */
@Entity
@Table(name = "sync_state")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SyncState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @Size(max = 40)
    @Column(name = "last_synced_commit", length = 40)
    private String lastSyncedCommit;

    @Column(name = "last_fetch_at")
    private Instant lastFetchAt;

    @Column(name = "last_push_at")
    private Instant lastPushAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SyncRunStatus status;

    @Lob
    @Column(name = "message")
    private String message;

    @JsonIgnoreProperties(value = { "mountPage", "space", "syncState" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @NotNull
    @JoinColumn(unique = true)
    private SyncRoot syncRoot;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public SyncState id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLastSyncedCommit() {
        return this.lastSyncedCommit;
    }

    public SyncState lastSyncedCommit(String lastSyncedCommit) {
        this.setLastSyncedCommit(lastSyncedCommit);
        return this;
    }

    public void setLastSyncedCommit(String lastSyncedCommit) {
        this.lastSyncedCommit = lastSyncedCommit;
    }

    public Instant getLastFetchAt() {
        return this.lastFetchAt;
    }

    public SyncState lastFetchAt(Instant lastFetchAt) {
        this.setLastFetchAt(lastFetchAt);
        return this;
    }

    public void setLastFetchAt(Instant lastFetchAt) {
        this.lastFetchAt = lastFetchAt;
    }

    public Instant getLastPushAt() {
        return this.lastPushAt;
    }

    public SyncState lastPushAt(Instant lastPushAt) {
        this.setLastPushAt(lastPushAt);
        return this;
    }

    public void setLastPushAt(Instant lastPushAt) {
        this.lastPushAt = lastPushAt;
    }

    public SyncRunStatus getStatus() {
        return this.status;
    }

    public SyncState status(SyncRunStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(SyncRunStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return this.message;
    }

    public SyncState message(String message) {
        this.setMessage(message);
        return this;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SyncRoot getSyncRoot() {
        return this.syncRoot;
    }

    public void setSyncRoot(SyncRoot syncRoot) {
        this.syncRoot = syncRoot;
    }

    public SyncState syncRoot(SyncRoot syncRoot) {
        this.setSyncRoot(syncRoot);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncState)) {
            return false;
        }
        return getId() != null && getId().equals(((SyncState) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SyncState{" +
            "id=" + getId() +
            ", lastSyncedCommit='" + getLastSyncedCommit() + "'" +
            ", lastFetchAt='" + getLastFetchAt() + "'" +
            ", lastPushAt='" + getLastPushAt() + "'" +
            ", status='" + getStatus() + "'" +
            ", message='" + getMessage() + "'" +
            "}";
    }
}
