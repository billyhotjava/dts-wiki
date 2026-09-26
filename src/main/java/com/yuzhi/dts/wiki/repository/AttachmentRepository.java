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
    // DTS-WIKI: customized (Sprint-6 W5).
    List<Attachment> findByPageIdAndDeletedAtIsNull(Long pageId);

    // DTS-WIKI: customized (Sprint-6 W5): blob GC guard.
    long countBySha256AndDeletedAtIsNull(String sha256);
}
