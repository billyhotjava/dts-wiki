package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.SyncRoot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SyncRoot entity.
 */
@Repository
public interface SyncRootRepository extends JpaRepository<SyncRoot, Long> {
    default Optional<SyncRoot> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<SyncRoot> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<SyncRoot> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select syncRoot from SyncRoot syncRoot left join fetch syncRoot.mountPage left join fetch syncRoot.space",
        countQuery = "select count(syncRoot) from SyncRoot syncRoot"
    )
    Page<SyncRoot> findAllWithToOneRelationships(Pageable pageable);

    @Query("select syncRoot from SyncRoot syncRoot left join fetch syncRoot.mountPage left join fetch syncRoot.space")
    List<SyncRoot> findAllWithToOneRelationships();

    @Query(
        "select syncRoot from SyncRoot syncRoot left join fetch syncRoot.mountPage left join fetch syncRoot.space where syncRoot.id =:id"
    )
    Optional<SyncRoot> findOneWithToOneRelationships(@Param("id") Long id);

    // DTS-WIKI: customized (Sprint-6 W6): never rely on the inverse in-memory collection
    // (stale within a session after adds); always query roots fresh by space.
    @Query("select syncRoot from SyncRoot syncRoot left join fetch syncRoot.mountPage where syncRoot.space.id = :spaceId")
    List<SyncRoot> findBySpaceWithMount(@Param("spaceId") Long spaceId);
}
