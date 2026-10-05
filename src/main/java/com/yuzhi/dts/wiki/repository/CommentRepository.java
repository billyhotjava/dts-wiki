package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Comment entity.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comment c where c.id=:id")
    Optional<Comment> findForUpdate(@Param("id") long id);

    long countByParentId(Long id);
    @Query("select c from Comment c where c.page.id=:page and c.parent is null and (c.deletedAt is null or exists(select r.id from Comment r where r.parent=c and r.deletedAt is null)) order by c.createdAt,c.id")
    Page<Comment> findThreads(@Param("page") long page, Pageable pageable);

    @Query("select c from Comment c join fetch c.author where c.parent.id=:parent and c.deletedAt is null order by c.createdAt,c.id")
    List<Comment> findReplies(@Param("parent") long parent);
    @Query("select comment from Comment comment where comment.author.login = ?#{authentication.name}")
    List<Comment> findByAuthorIsCurrentUser();

    default Optional<Comment> findOneWithEagerRelationships(Long id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Comment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Comment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select comment from Comment comment left join fetch comment.author",
        countQuery = "select count(comment) from Comment comment"
    )
    Page<Comment> findAllWithToOneRelationships(Pageable pageable);

    @Query("select comment from Comment comment left join fetch comment.author")
    List<Comment> findAllWithToOneRelationships();

    @Query("select comment from Comment comment left join fetch comment.author where comment.id =:id")
    Optional<Comment> findOneWithToOneRelationships(@Param("id") Long id);
}
