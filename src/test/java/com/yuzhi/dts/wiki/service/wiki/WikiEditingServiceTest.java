package com.yuzhi.dts.wiki.service.wiki;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.yuzhi.dts.wiki.domain.User;
import java.time.*;
import org.junit.jupiter.api.Test;

class WikiEditingServiceTest {
    @Test void presenceExpiresAfterNinetySecondsWithoutHardLocks() {
        var personal=mock(WikiPersonalService.class);var time=new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2026-10-06T00:00:00Z"));
        Clock clock=new Clock(){ public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId zone){return this;} public Instant instant(){return time.get();} };
        var service=new WikiEditingService(personal,mock(com.yuzhi.dts.wiki.repository.PageRepository.class),mock(SpaceAccessService.class),mock(PageWritePolicy.class),mock(org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate.class),clock);
        User alice=new User();alice.setId("alice");alice.setLogin("alice");User bob=new User();bob.setId("bob");bob.setLogin("bob");
        when(personal.current()).thenReturn(alice);assertThat(service.heartbeat(1)).isEmpty();
        when(personal.current()).thenReturn(bob);assertThat(service.heartbeat(1)).extracting(WikiEditingService.Presence::login).containsExactly("alice");
        time.set(time.get().plusSeconds(91));assertThat(service.heartbeat(1)).isEmpty();
        when(personal.current()).thenReturn(alice);assertThat(service.presence(1)).extracting(WikiEditingService.Presence::login).containsExactly("bob");
    }
}
