package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxOp;
import com.yuzhi.dts.wiki.domain.enumeration.OutboxStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Outbound (wiki -> git) work item, written in the same transaction as the page change
 */
@Entity
@Table(name = "sync_outbox")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SyncOutbox implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "op", nullable = false)
    private OutboxOp op;

    /**
     * JSON: {fromPath,toPath,versionId,attachmentId,...}
     */
    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @NotNull
    @Size(max = 50)
    @Column(name = "actor_login", length = 50, nullable = false)
    private String actorLogin;

    @Size(max = 100)
    @Column(name = "actor_name", length = 100)
    private String actorName;

    @Size(max = 254)
    @Column(name = "actor_email", length = 254)
    private String actorEmail;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    @NotNull
    @Column(name = "attempts", nullable = false)
    private Integer attempts;

    @Lob
    @Column(name = "last_error")
    private String lastError;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "syncRootses", "pageses" }, allowSetters = true)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(
        value = { "childrens", "versionses", "attachmentses", "commentses", "currentVersion", "labelses", "space", "parent" },
        allowSetters = true
    )
    private Page page;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public SyncOutbox id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OutboxOp getOp() {
        return this.op;
    }

    public SyncOutbox op(OutboxOp op) {
        this.setOp(op);
        return this;
    }

    public void setOp(OutboxOp op) {
        this.op = op;
    }

    public String getPayload() {
        return this.payload;
    }

    public SyncOutbox payload(String payload) {
        this.setPayload(payload);
        return this;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getActorLogin() {
        return this.actorLogin;
    }

    public SyncOutbox actorLogin(String actorLogin) {
        this.setActorLogin(actorLogin);
        return this;
    }

    public void setActorLogin(String actorLogin) {
        this.actorLogin = actorLogin;
    }

    public String getActorName() {
        return this.actorName;
    }

    public SyncOutbox actorName(String actorName) {
        this.setActorName(actorName);
        return this;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorEmail() {
        return this.actorEmail;
    }

    public SyncOutbox actorEmail(String actorEmail) {
        this.setActorEmail(actorEmail);
        return this;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public OutboxStatus getStatus() {
        return this.status;
    }

    public SyncOutbox status(OutboxStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public Integer getAttempts() {
        return this.attempts;
    }

    public SyncOutbox attempts(Integer attempts) {
        this.setAttempts(attempts);
        return this;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return this.lastError;
    }

    public SyncOutbox lastError(String lastError) {
        this.setLastError(lastError);
        return this;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public SyncOutbox createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getProcessedAt() {
        return this.processedAt;
    }

    public SyncOutbox processedAt(Instant processedAt) {
        this.setProcessedAt(processedAt);
        return this;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Space getSpace() {
        return this.space;
    }

    public void setSpace(Space space) {
        this.space = space;
    }

    public SyncOutbox space(Space space) {
        this.setSpace(space);
        return this;
    }

    public Page getPage() {
        return this.page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public SyncOutbox page(Page page) {
        this.setPage(page);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncOutbox)) {
            return false;
        }
        return getId() != null && getId().equals(((SyncOutbox) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SyncOutbox{" +
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
            "}";
    }
}
