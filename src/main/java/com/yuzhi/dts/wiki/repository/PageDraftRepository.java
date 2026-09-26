package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.PageDraft;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PageDraft entity.
 */
@Repository
public interface PageDraftRepository extends JpaRepository<PageDraft, Long> {
    @Query("select pageDraft from PageDraft pageDraft where pageDraft.user.login = ?#{authentication.name}")
    List<PageDraft> findByUserIsCurrentUser();

    default Optional<PageDraft> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<PageDraft> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<PageDraft> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select pageDraft from PageDraft pageDraft left join fetch pageDraft.user",
        countQuery = "select count(pageDraft) from PageDraft pageDraft"
    )
    Page<PageDraft> findAllWithToOneRelationships(Pageable pageable);

    @Query("select pageDraft from PageDraft pageDraft left join fetch pageDraft.user")
    List<PageDraft> findAllWithToOneRelationships();

    @Query("select pageDraft from PageDraft pageDraft left join fetch pageDraft.user where pageDraft.id =:id")
    Optional<PageDraft> findOneWithToOneRelationships(@Param("id") Long id);
}
