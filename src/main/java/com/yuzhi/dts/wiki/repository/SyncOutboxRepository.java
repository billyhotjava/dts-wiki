package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.SyncOutbox;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SyncOutbox entity.
 */
@Repository
public interface SyncOutboxRepository extends JpaRepository<SyncOutbox, Long> {
    default Optional<SyncOutbox> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<SyncOutbox> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<SyncOutbox> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select syncOutbox from SyncOutbox syncOutbox left join fetch syncOutbox.space",
        countQuery = "select count(syncOutbox) from SyncOutbox syncOutbox"
    )
    Page<SyncOutbox> findAllWithToOneRelationships(Pageable pageable);

    @Query("select syncOutbox from SyncOutbox syncOutbox left join fetch syncOutbox.space")
    List<SyncOutbox> findAllWithToOneRelationships();

    @Query("select syncOutbox from SyncOutbox syncOutbox left join fetch syncOutbox.space where syncOutbox.id =:id")
    Optional<SyncOutbox> findOneWithToOneRelationships(@Param("id") Long id);
}
