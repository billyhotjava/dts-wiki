package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.ActivityEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ActivityEvent entity.
 */
@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long>, JpaSpecificationExecutor<ActivityEvent> {
    default Optional<ActivityEvent> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ActivityEvent> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ActivityEvent> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select activityEvent from ActivityEvent activityEvent left join fetch activityEvent.space",
        countQuery = "select count(activityEvent) from ActivityEvent activityEvent"
    )
    Page<ActivityEvent> findAllWithToOneRelationships(Pageable pageable);

    @Query("select activityEvent from ActivityEvent activityEvent left join fetch activityEvent.space")
    List<ActivityEvent> findAllWithToOneRelationships();

    @Query("select activityEvent from ActivityEvent activityEvent left join fetch activityEvent.space where activityEvent.id =:id")
    Optional<ActivityEvent> findOneWithToOneRelationships(@Param("id") Long id);
}
