package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.Attachment;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Attachment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    @Query("select a from Attachment a join fetch a.page p where p.space.id = :spaceId and a.gitPath = :path and a.deletedAt is null and p.deletedAt is null")
    java.util.Optional<Attachment> findLiveBySpaceAndGitPath(@org.springframework.data.repository.query.Param("spaceId") Long spaceId, @org.springframework.data.repository.query.Param("path") String path);

    @Query("select a from Attachment a join fetch a.page p where p.space.id = :spaceId and a.gitPath is not null and a.deletedAt is null and p.deletedAt is null order by a.fileName, a.id")
    List<Attachment> findLiveGitBySpace(@org.springframework.data.repository.query.Param("spaceId") Long spaceId);
    // DTS-WIKI: customized (Sprint-6 W5).
    List<Attachment> findByPageIdAndDeletedAtIsNull(Long pageId);

    // DTS-WIKI: customized (Sprint-6 W5): blob GC guard.
    long countBySha256AndDeletedAtIsNull(String sha256);
}
