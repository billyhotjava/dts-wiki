package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.PageVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PageVersion entity.
 */
@Repository
public interface PageVersionRepository extends JpaRepository<PageVersion, Long>, JpaSpecificationExecutor<PageVersion> {
    @Query("select pageVersion from PageVersion pageVersion where pageVersion.author.login = ?#{authentication.name}")
    List<PageVersion> findByAuthorIsCurrentUser();

    default Optional<PageVersion> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<PageVersion> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<PageVersion> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select pageVersion from PageVersion pageVersion left join fetch pageVersion.author",
        countQuery = "select count(pageVersion) from PageVersion pageVersion"
    )
    Page<PageVersion> findAllWithToOneRelationships(Pageable pageable);

    @Query("select pageVersion from PageVersion pageVersion left join fetch pageVersion.author")
    List<PageVersion> findAllWithToOneRelationships();

    @Query("select pageVersion from PageVersion pageVersion left join fetch pageVersion.author where pageVersion.id =:id")
    Optional<PageVersion> findOneWithToOneRelationships(@Param("id") Long id);
}
