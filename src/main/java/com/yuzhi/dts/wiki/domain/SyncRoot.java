package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A directory of the space's git repository mounted into the page tree
 */
@Entity
@Table(name = "sync_root")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class SyncRoot implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    /**
     * path inside the repository, e.g. \"worklog\"
     */
    @NotNull
    @Size(max = 255)
    @Column(name = "repo_path", length = 255, nullable = false)
    private String repoPath;

    @NotNull
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /**
     * mount point of the SyncRoot inside the space's page tree (a FOLDER page)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(
        value = { "childrens", "versionses", "attachmentses", "commentses", "currentVersion", "labelses", "space", "parent" },
        allowSetters = true
    )
    private Page mountPage;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "syncRootses", "pageses" }, allowSetters = true)
    private Space space;

    @JsonIgnoreProperties(value = { "syncRoot" }, allowSetters = true)
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "syncRoot")
    private SyncState syncState;


    public Long getId() {
        return this.id;
    }

    public SyncRoot id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepoPath() {
        return this.repoPath;
    }

    public SyncRoot repoPath(String repoPath) {
        this.setRepoPath(repoPath);
        return this;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public SyncRoot enabled(Boolean enabled) {
        this.setEnabled(enabled);
        return this;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Page getMountPage() {
        return this.mountPage;
    }

    public void setMountPage(Page page) {
        this.mountPage = page;
    }

    public SyncRoot mountPage(Page page) {
        this.setMountPage(page);
        return this;
    }

    public Space getSpace() {
        return this.space;
    }

    public void setSpace(Space space) {
        this.space = space;
    }

    public SyncRoot space(Space space) {
        this.setSpace(space);
        return this;
    }

    public SyncState getSyncState() {
        return this.syncState;
    }

    public void setSyncState(SyncState syncState) {
        if (this.syncState != null) {
            this.syncState.setSyncRoot(null);
        }
        if (syncState != null) {
            syncState.setSyncRoot(this);
        }
        this.syncState = syncState;
    }

    public SyncRoot syncState(SyncState syncState) {
        this.setSyncState(syncState);
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SyncRoot)) {
            return false;
        }
        return getId() != null && getId().equals(((SyncRoot) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SyncRoot{" +
            "id=" + getId() +
            ", repoPath='" + getRepoPath() + "'" +
            ", enabled='" + getEnabled() + "'" +
            "}";
    }
}
