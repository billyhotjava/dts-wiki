package com.yuzhi.dts.wiki.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

/**
 * Unit tests for {@link KeycloakAuthorityMapper} (Sprint-6 design 03 S3).
 */
class KeycloakAuthorityMapperTest {

    private static List<String> names(List<GrantedAuthority> authorities) {
        return authorities.stream().map(GrantedAuthority::getAuthority).sorted().toList();
    }

    @Test
    void mapsBaseRoles() {
        assertThat(names(KeycloakAuthorityMapper.map(List.of("reader")))).containsExactly("ROLE_USER");
        assertThat(names(KeycloakAuthorityMapper.map(List.of("editor")))).containsExactly("ROLE_EDITOR");
        assertThat(names(KeycloakAuthorityMapper.map(List.of("admin")))).containsExactly("ROLE_ADMIN");
    }

    @Test
    void mapsSpaceRoles() {
        assertThat(names(KeycloakAuthorityMapper.map(List.of("space-prs")))).containsExactly("ROLE_SPACE_PRS");
        assertThat(names(KeycloakAuthorityMapper.map(List.of("space-dts")))).containsExactly("ROLE_SPACE_DTS");
        // dashes inside the slug become underscores, case is normalized
        assertThat(names(KeycloakAuthorityMapper.map(List.of("space-my-app")))).containsExactly("ROLE_SPACE_MY_APP");
    }

    @Test
    void passesThroughPrefixedRolesAndDropsUnknown() {
        assertThat(names(KeycloakAuthorityMapper.map(List.of("ROLE_CUSTOM", "whatever", "space-"))))
            .containsExactly("ROLE_CUSTOM");
    }

    @Test
    void nullAndBlankAreSafe() {
        assertThat(KeycloakAuthorityMapper.map(null)).isEmpty();
        assertThat(KeycloakAuthorityMapper.map(List.of())).isEmpty();
    }

    @Test
    void spaceAuthorityHelperMatchesMapping() {
        assertThat(KeycloakAuthorityMapper.spaceAuthority("prs")).isEqualTo("ROLE_SPACE_PRS");
        assertThat(KeycloakAuthorityMapper.spaceAuthority("my-app")).isEqualTo("ROLE_SPACE_MY_APP");
    }
}
