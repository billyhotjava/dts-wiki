package com.yuzhi.dts.wiki.repository;

import com.yuzhi.dts.wiki.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    String USERS_BY_EMAIL_CACHE = "usersByEmail";
    Optional<User> findOneByLogin(String login);

    @Query("select u from User u where locate(lower(:q),lower(u.login))>0 or locate(lower(:q),lower(u.firstName))>0 or locate(lower(:q),lower(u.lastName))>0 order by u.login,u.id")
    List<User> findMentionDisplayCandidates(@Param("q") String query, Pageable pageable);

    // DTS-WIKI: customized (Sprint-6 W5c): @mention candidates.
    @Query("select user from User user where lower(user.login) like lower(concat('%', :q, '%')) or lower(user.email) like lower(concat('%', :q, '%'))")
    List<User> searchByLoginOrEmail(@Param("q") String q, Pageable pageable);

    @Query("select distinct u from User u join u.authorities a where u.activated = true and (a.name = :role or a.name = 'ROLE_ADMIN') and (lower(u.login) like lower(concat('%', :q, '%')) or lower(u.firstName) like lower(concat('%', :q, '%')) or lower(u.lastName) like lower(concat('%', :q, '%')))")
    List<User> findMentionCandidates(@Param("role") String role, @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = "authorities")
    @Cacheable(cacheNames = USERS_BY_LOGIN_CACHE, unless = "#result == null")
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);
}
