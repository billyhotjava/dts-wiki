package com.yuzhi.dts.wiki.service.wiki;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.security.AuthoritiesConstants;
import com.yuzhi.dts.wiki.security.KeycloakAuthorityMapper;
import com.yuzhi.dts.wiki.security.SecurityUtils;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product-space (tenant-lite) access checks (Sprint-6 design 03 S3).
 *
 * <ul>
 *   <li>read(space) = {@code ROLE_ADMIN} or {@code ROLE_SPACE_&lt;SLUG&gt;}</li>
 *   <li>write(space) = read and ({@code ROLE_EDITOR} or {@code ROLE_ADMIN})</li>
 * </ul>
 * All business REST under {@code /api/wiki/**} must go through this service;
 * generated entity endpoints are {@code ROLE_ADMIN} only (see SecurityConfiguration).
 * Filters belong in the queries themselves ({@link #readableSpaceIds()}), never applied after fetching.
 */
@Service
@Transactional(readOnly = true)
public class SpaceAccessService {

    private final SpaceRepository spaceRepository;

    public SpaceAccessService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    public boolean canRead(String spaceSlug) {
        if (spaceSlug == null || spaceSlug.isBlank()) {
            return false;
        }
        return spaceRepository.findOneBySlug(spaceSlug).map(this::canRead).orElse(false);
    }

    public boolean canRead(Space space) {
        if (space == null || space.getSlug() == null) { return false; }
        String authority = space.getAccessRole() == null
            ? KeycloakAuthorityMapper.spaceAuthority(space.getSlug())
            : KeycloakAuthorityMapper.spaceRoleAuthority(space.getAccessRole());
        return SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)
            || authority != null && SecurityUtils.hasCurrentUserThisAuthority(authority);
    }

    public String requiredReadAuthority(String slug) {
        requireRead(slug);
        Space space = spaceRepository.findOneBySlug(slug).orElseThrow(() -> new SpaceNotVisibleException(slug));
        return space.getAccessRole() == null ? KeycloakAuthorityMapper.spaceAuthority(slug) : KeycloakAuthorityMapper.spaceRoleAuthority(space.getAccessRole());
    }

    public boolean canWrite(String spaceSlug) {
        return (
            canRead(spaceSlug) &&
            SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.EDITOR, AuthoritiesConstants.ADMIN)
        );
    }

    public boolean canRead(Page page) {
        return page != null && canRead(page.getSpace());
    }

    public boolean canWrite(Page page) {
        return canRead(page) && SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.EDITOR, AuthoritiesConstants.ADMIN);
    }

    /**
     * Ids of spaces visible to the current user, for SQL-level filtering.
     * Admins see every space id present in the database.
     */
    public Set<Long> readableSpaceIds() {
        if (SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)) {
            return spaceRepository.findAll().stream().map(Space::getId).collect(Collectors.toSet());
        }
        return spaceRepository
            .findAll()
            .stream()
            .filter(this::canRead)
            .map(Space::getId)
            .collect(Collectors.toSet());
    }

    /**
     * @throws SpaceNotVisibleException (mapped to 404) when the space is not readable.
     */
    public void requireRead(String spaceSlug) {
        if (!canRead(spaceSlug)) {
            throw new SpaceNotVisibleException(spaceSlug);
        }
    }

    /**
     * @throws SpaceNotVisibleException (mapped to 404) when the space is not readable.
     * @throws AccessDeniedException (mapped to 403) when readable but not writable.
     */
    public void requireWrite(String spaceSlug) {
        requireRead(spaceSlug);
        if (!canWrite(spaceSlug)) {
            throw new AccessDeniedException("No write access to space: " + spaceSlug);
        }
    }

    public void requireRead(Page page) {
        if (!canRead(page)) {
            throw new SpaceNotVisibleException(page == null || page.getSpace() == null ? null : page.getSpace().getSlug());
        }
    }

    public void requireWrite(Page page) {
        if (!canRead(page)) {
            throw new SpaceNotVisibleException(page == null || page.getSpace() == null ? null : page.getSpace().getSlug());
        }
        if (!canWrite(page)) {
            throw new AccessDeniedException("No write access to space: " + page.getSpace().getSlug());
        }
    }
}
