package com.yuzhi.dts.wiki.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Maps Keycloak {@code dts-wiki} client roles (exposed in the {@code roles} claim
 * without prefix by the {@code client-roles-to-roles} protocol mapper,
 * see {@code deploy/keycloak/wiki-v2.sh}) to application authorities.
 *
 * <p>Mapping (Sprint-6 design 03 S3):
 * <table>
 *   <tr><th>Keycloak client role</th><th>Application authority</th></tr>
 *   <tr><td>{@code reader}</td><td>{@code ROLE_USER}</td></tr>
 *   <tr><td>{@code editor}</td><td>{@code ROLE_EDITOR}</td></tr>
 *   <tr><td>{@code admin}</td><td>{@code ROLE_ADMIN}</td></tr>
 *   <tr><td>{@code space-&lt;slug&gt;}</td><td>{@code ROLE_SPACE_&lt;SLUG&gt;}</td></tr>
 * </table>
 * Values already starting with {@code ROLE_} (e.g. realm-level assignments) pass through untouched.
 */
public final class KeycloakAuthorityMapper {

    public static final String SPACE_AUTHORITY_PREFIX = "ROLE_SPACE_";

    private KeycloakAuthorityMapper() {}

    public static List<GrantedAuthority> map(Collection<String> roles) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (roles == null) {
            return authorities;
        }
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            String mapped = mapRole(role);
            if (mapped != null) {
                authorities.add(new SimpleGrantedAuthority(mapped));
            }
        }
        return authorities;
    }

    static String mapRole(String role) {
        return switch (role) {
            case "reader" -> AuthoritiesConstants.USER;
            case "editor" -> AuthoritiesConstants.EDITOR;
            case "admin" -> AuthoritiesConstants.ADMIN;
            default -> {
                if (role.startsWith("ROLE_")) {
                    yield role;
                }
                if (role.startsWith("space-") && role.length() > "space-".length()) {
                    yield SPACE_AUTHORITY_PREFIX + role.substring("space-".length()).toUpperCase(Locale.ROOT).replace('-', '_');
                }
                yield null;
            }
        };
    }

    public static String spaceAuthority(String slug) {
        return SPACE_AUTHORITY_PREFIX + slug.toUpperCase(Locale.ROOT).replace('-', '_');
    }

    public static String spaceRoleAuthority(String role) {
        return role != null && role.matches("space-[a-z][a-z0-9-]{1,31}") ? mapRole(role) : null;
    }
}
