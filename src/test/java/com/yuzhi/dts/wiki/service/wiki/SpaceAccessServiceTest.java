package com.yuzhi.dts.wiki.service.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.yuzhi.dts.wiki.domain.Page;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.repository.SpaceRepository;
import com.yuzhi.dts.wiki.security.AuthoritiesConstants;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Permission matrix at service level (Sprint-6 design 07 S2 rows that do not need HTTP):
 * A = ROLE_USER + ROLE_SPACE_PRS, B = A + ROLE_EDITOR, C = ROLE_ADMIN, D = ROLE_USER only.
 */
@ExtendWith(MockitoExtension.class)
class SpaceAccessServiceTest {

    private static final String PRS = "prs";
    private static final String DTS = "dts";

    @Mock
    private SpaceRepository spaceRepository;

    private SpaceAccessService service;

    private Space prsSpace;
    private Space dtsSpace;

    @BeforeEach
    void setUp() {
        service = new SpaceAccessService(spaceRepository);
        prsSpace = new Space().slug(PRS);
        prsSpace.setId(1L);
        dtsSpace = new Space().slug(DTS);
        dtsSpace.setId(2L);
    }

    private void stubSpaces() {
        when(spaceRepository.findAll()).thenReturn(List.of(prsSpace, dtsSpace));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void login(String... authorities) {
        TestingAuthenticationToken token = new TestingAuthenticationToken("user", "pw", authorities);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Test
    void userA_readsPrsOnly() {
        stubSpaces();
        login(AuthoritiesConstants.USER, "ROLE_SPACE_PRS");
        assertThat(service.canRead(PRS)).isTrue();
        assertThat(service.canRead(DTS)).isFalse();
        assertThat(service.canWrite(PRS)).isFalse();
        assertThat(service.readableSpaceIds()).containsExactly(1L);
        // readable but not writable -> 403 on write, silent pass on read
        service.requireRead(PRS);
        assertThatThrownBy(() -> service.requireWrite(PRS)).isInstanceOf(AccessDeniedException.class);
        // not readable at all -> 404-style on both
        assertThatThrownBy(() -> service.requireRead(DTS)).isInstanceOf(SpaceNotVisibleException.class);
        assertThatThrownBy(() -> service.requireWrite(DTS)).isInstanceOf(SpaceNotVisibleException.class);
    }

    @Test
    void userB_writesPrs() {
        login(AuthoritiesConstants.USER, AuthoritiesConstants.EDITOR, "ROLE_SPACE_PRS");
        assertThat(service.canRead(PRS)).isTrue();
        assertThat(service.canWrite(PRS)).isTrue();
        assertThat(service.canRead(DTS)).isFalse();
        service.requireWrite(PRS);
        assertThatThrownBy(() -> service.requireWrite(DTS)).isInstanceOf(SpaceNotVisibleException.class);
    }

    @Test
    void adminC_seesEverything() {
        stubSpaces();
        login(AuthoritiesConstants.ADMIN);
        assertThat(service.canRead(PRS)).isTrue();
        assertThat(service.canWrite(DTS)).isTrue();
        assertThat(service.readableSpaceIds()).containsExactlyInAnyOrder(1L, 2L);
        service.requireWrite(DTS);
    }

    @Test
    void userD_seesNothing() {
        stubSpaces();
        login(AuthoritiesConstants.USER);
        assertThat(service.canRead(PRS)).isFalse();
        assertThat(service.readableSpaceIds()).isEmpty();
        assertThatThrownBy(() -> service.requireRead(PRS)).isInstanceOf(SpaceNotVisibleException.class);
    }

    @Test
    void pageDelegatesToItsSpace() {
        login(AuthoritiesConstants.USER, "ROLE_SPACE_PRS");
        com.yuzhi.dts.wiki.domain.Page page = new Page();
        page.setSpace(prsSpace);
        assertThat(service.canRead(page)).isTrue();
        assertThat(service.canWrite(page)).isFalse();
    }
}
