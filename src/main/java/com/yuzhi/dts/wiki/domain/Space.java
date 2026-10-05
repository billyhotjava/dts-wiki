package com.yuzhi.dts.wiki.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * 产品空间
 */
@Entity
@Table(name = "space")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Space implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Pattern(regexp = "^[a-z][a-z0-9-]{1,31}$")
    @Column(name = "slug", nullable = false, unique = true)
    private String slug;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 38)
    @Column(name = "access_role", length = 38)
    private String accessRole;

    @Column(name = "manifest_managed", nullable = false)
    private boolean manifestManaged;

    public String getAccessRole() { return accessRole; }
    public void setAccessRole(String value) { accessRole = value; }
    public boolean isManifestManaged() { return manifestManaged; }
    public void setManifestManaged(boolean value) { manifestManaged = value; }

    /**
     * SSH URL, e.g. git@github.com:billyhotjava/prs-stack.git ; null = no git sync
     */
    @Size(max = 255)
    @Column(name = "git_repo_url", length = 255)
    private String gitRepoUrl;

    @Size(max = 100)
    @Column(name = "git_branch", length = 100)
    private String gitBranch;

    @Column(name = "position")
    private Integer position;

    @NotNull
    @Column(name = "archived", nullable = false)
    private Boolean archived;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "space")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "mountPage", "space", "syncState" }, allowSetters = true)
    private Set<SyncRoot> syncRootses = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "space")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(
        value = { "childrens", "versionses", "attachmentses", "commentses", "currentVersion", "labelses", "space", "parent" },
        allowSetters = true
    )
    private Set<Page> pageses = new HashSet<>();


    public Long getId() {
        return this.id;
    }

    public Space id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSlug() {
        return this.slug;
    }

    public Space slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return this.name;
    }

    public Space name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public Space description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGitRepoUrl() {
        return this.gitRepoUrl;
    }

    public Space gitRepoUrl(String gitRepoUrl) {
        this.setGitRepoUrl(gitRepoUrl);
        return this;
    }

    public void setGitRepoUrl(String gitRepoUrl) {
        this.gitRepoUrl = gitRepoUrl;
    }

    public String getGitBranch() {
        return this.gitBranch;
    }

    public Space gitBranch(String gitBranch) {
        this.setGitBranch(gitBranch);
        return this;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public Integer getPosition() {
        return this.position;
    }

    public Space position(Integer position) {
        this.setPosition(position);
        return this;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Boolean getArchived() {
        return this.archived;
    }

    public Space archived(Boolean archived) {
        this.setArchived(archived);
        return this;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Set<SyncRoot> getSyncRootses() {
        return this.syncRootses;
    }

    public void setSyncRootses(Set<SyncRoot> syncRoots) {
        if (this.syncRootses != null) {
            this.syncRootses.forEach(i -> i.setSpace(null));
        }
        if (syncRoots != null) {
            syncRoots.forEach(i -> i.setSpace(this));
        }
        this.syncRootses = syncRoots;
    }

    public Space syncRootses(Set<SyncRoot> syncRoots) {
        this.setSyncRootses(syncRoots);
        return this;
    }

    public Space addSyncRoots(SyncRoot syncRoot) {
        this.syncRootses.add(syncRoot);
        syncRoot.setSpace(this);
        return this;
    }

    public Space removeSyncRoots(SyncRoot syncRoot) {
        this.syncRootses.remove(syncRoot);
        syncRoot.setSpace(null);
        return this;
    }

    public Set<Page> getPageses() {
        return this.pageses;
    }

    public void setPageses(Set<Page> pages) {
        if (this.pageses != null) {
            this.pageses.forEach(i -> i.setSpace(null));
        }
        if (pages != null) {
            pages.forEach(i -> i.setSpace(this));
        }
        this.pageses = pages;
    }

    public Space pageses(Set<Page> pages) {
        this.setPageses(pages);
        return this;
    }

    public Space addPages(Page page) {
        this.pageses.add(page);
        page.setSpace(this);
        return this;
    }

    public Space removePages(Page page) {
        this.pageses.remove(page);
        page.setSpace(null);
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Space)) {
            return false;
        }
        return getId() != null && getId().equals(((Space) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Space{" +
            "id=" + getId() +
            ", slug='" + getSlug() + "'" +
            ", name='" + getName() + "'" +
            ", description='" + getDescription() + "'" +
            ", gitRepoUrl='" + getGitRepoUrl() + "'" +
            ", gitBranch='" + getGitBranch() + "'" +
            ", position=" + getPosition() +
            ", archived='" + getArchived() + "'" +
            "}";
    }
}
