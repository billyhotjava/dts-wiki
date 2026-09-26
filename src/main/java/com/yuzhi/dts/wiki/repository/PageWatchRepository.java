package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.PageWatch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PageWatch entity.
 */
@Repository
public interface PageWatchRepository extends JpaRepository<PageWatch, Long> {
    @Query("select pageWatch from PageWatch pageWatch where pageWatch.user.login = ?#{authentication.name}")
    List<PageWatch> findByUserIsCurrentUser();

    default Optional<PageWatch> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<PageWatch> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<PageWatch> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select pageWatch from PageWatch pageWatch left join fetch pageWatch.user",
        countQuery = "select count(pageWatch) from PageWatch pageWatch"
    )
    Page<PageWatch> findAllWithToOneRelationships(Pageable pageable);

    @Query("select pageWatch from PageWatch pageWatch left join fetch pageWatch.user")
    List<PageWatch> findAllWithToOneRelationships();

    @Query("select pageWatch from PageWatch pageWatch left join fetch pageWatch.user where pageWatch.id =:id")
    Optional<PageWatch> findOneWithToOneRelationships(@Param("id") Long id);

    // DTS-WIKI: customized (Sprint-6 W4).
    boolean existsByPageIdAndUserLogin(Long pageId, String login);
}
