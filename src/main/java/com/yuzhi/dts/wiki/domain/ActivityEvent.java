package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.yuzhi.dts.wiki.domain.enumeration.ActivityType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A ActivityEvent.
 */
@Entity
@Table(name = "activity_event")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ActivityEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ActivityType type;

    @NotNull
    @Size(max = 100)
    @Column(name = "actor_name", length = 100, nullable = false)
    private String actorName;

    @Size(max = 200)
    @Column(name = "target_title", length = 200)
    private String targetTitle;

    // DTS-WIKI: customized (W5b): @Lob removed, see PageVersion.contentMd.
    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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


    public Long getId() {
        return this.id;
    }

    public ActivityEvent id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActivityType getType() {
        return this.type;
    }

    public ActivityEvent type(ActivityType type) {
        this.setType(type);
        return this;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public String getActorName() {
        return this.actorName;
    }

    public ActivityEvent actorName(String actorName) {
        this.setActorName(actorName);
        return this;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getTargetTitle() {
        return this.targetTitle;
    }

    public ActivityEvent targetTitle(String targetTitle) {
        this.setTargetTitle(targetTitle);
        return this;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public String getDetail() {
        return this.detail;
    }

    public ActivityEvent detail(String detail) {
        this.setDetail(detail);
        return this;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public ActivityEvent createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Space getSpace() {
        return this.space;
    }

    public void setSpace(Space space) {
        this.space = space;
    }

    public ActivityEvent space(Space space) {
        this.setSpace(space);
        return this;
    }

    public Page getPage() {
        return this.page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    public ActivityEvent page(Page page) {
        this.setPage(page);
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ActivityEvent)) {
            return false;
        }
        return getId() != null && getId().equals(((ActivityEvent) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ActivityEvent{" +
            "id=" + getId() +
            ", type='" + getType() + "'" +
            ", actorName='" + getActorName() + "'" +
            ", targetTitle='" + getTargetTitle() + "'" +
            ", detail='" + getDetail() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
