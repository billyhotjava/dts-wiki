package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.SyncConflict;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SyncConflict entity.
 */
@Repository
public interface SyncConflictRepository extends JpaRepository<SyncConflict, Long> {
    @Query("select syncConflict from SyncConflict syncConflict where syncConflict.resolvedBy.login = ?#{authentication.name}")
    List<SyncConflict> findByResolvedByIsCurrentUser();

    default Optional<SyncConflict> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<SyncConflict> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<SyncConflict> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select syncConflict from SyncConflict syncConflict left join fetch syncConflict.page left join fetch syncConflict.resolvedBy",
        countQuery = "select count(syncConflict) from SyncConflict syncConflict"
    )
    Page<SyncConflict> findAllWithToOneRelationships(Pageable pageable);

    @Query("select syncConflict from SyncConflict syncConflict left join fetch syncConflict.page left join fetch syncConflict.resolvedBy")
    List<SyncConflict> findAllWithToOneRelationships();

    @Query(
        "select syncConflict from SyncConflict syncConflict left join fetch syncConflict.page left join fetch syncConflict.resolvedBy where syncConflict.id =:id"
    )
    Optional<SyncConflict> findOneWithToOneRelationships(@Param("id") Long id);
}
