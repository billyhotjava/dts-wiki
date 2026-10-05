package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.Page;
// DTS-WIKI: customized (entity named Page clashes with Spring Data Page):
// Spring's Page is fully qualified below, the import is intentionally absent.
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Page entity.
 *
 * When extending this class, extend PageRepositoryWithBagRelationships too.
 */
@Repository
public interface PageRepository extends PageRepositoryWithBagRelationships, JpaRepository<Page, Long>, JpaSpecificationExecutor<Page> {
    default Optional<Page> findOneWithEagerRelationships(Long id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<Page> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default org.springframework.data.domain.Page<Page> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select page from Page page left join fetch page.space left join fetch page.parent",
        countQuery = "select count(page) from Page page"
    )
    org.springframework.data.domain.Page<Page> findAllWithToOneRelationships(Pageable pageable);

    @Query("select page from Page page left join fetch page.space left join fetch page.parent")
    List<Page> findAllWithToOneRelationships();

    @Query("select page from Page page left join fetch page.space left join fetch page.parent where page.id =:id")
    Optional<Page> findOneWithToOneRelationships(@Param("id") Long id);

    // DTS-WIKI: customized (Sprint-6 W4/W5a): wiki-tree queries. Soft-deleted pages are
    // invisible to the tree, search and activity feeds (invariant I10).

    @Query(
        "select page from Page page left join fetch page.space left join fetch page.parent " +
        "where page.space.id = :spaceId and page.deletedAt is null order by page.parent.id nulls first, page.position"
    )
    List<Page> findLiveBySpace(@Param("spaceId") Long spaceId);

    @Query("select page from Page page left join fetch page.space left join fetch page.currentVersion where page.id = :id and page.deletedAt is null")
    Optional<Page> findLive(@Param("id") Long id);

    @Query("select page from Page page where page.id = :id")
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<Page> findForUpdate(@Param("id") Long id);

    @Query("select coalesce(max(page.position), 0) from Page page where page.space.id = :spaceId and page.deletedAt is null and ((:parentId is null and page.parent is null) or (page.parent.id = :parentId))")
    int maxSiblingPosition(@Param("spaceId") Long spaceId, @Param("parentId") Long parentId);
}
