package com.yuzhi.dts.wiki.service.wiki;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.yuzhi.dts.wiki.config.WikiProperties;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CurrentIdentityServiceTest {
    @Test void usesAuthenticatedEffectiveRolesWithoutCachingRecipientPermissions() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger tokenCalls = new AtomicInteger(), roleCalls = new AtomicInteger();
        AtomicReference<String> roles = new AtomicReference<>("[{\"name\":\"space-test-team\"},{\"name\":\"editor\"}]");
        AtomicReference<String> user = new AtomicReference<>("{\"enabled\":true,\"emailVerified\":true,\"email\":\"test@example.invalid\"}");
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath(), response;
            if (path.endsWith("/token")) { tokenCalls.incrementAndGet(); response = "{\"access_token\":\"test-credential\",\"expires_in\":300}"; }
            else {
                assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-credential");
                if (path.endsWith("/composite")) { roleCalls.incrementAndGet(); response = roles.get(); }
                else response = user.get();
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            WikiProperties properties = new WikiProperties(); var settings = properties.getDirectory();
            settings.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort()); settings.setRealm("fixture"); settings.setWikiClientUuid("wiki-client");
            settings.setClientId("directory-client"); settings.setClientSecret("fixture-only");
            var adapter = new CurrentIdentityService(properties, new ObjectMapper());
            assertThat(adapter.lookup("person").canRead("test-team", null)).isTrue();
            roles.set("[]"); assertThat(adapter.lookup("person").canRead("test-team", null)).isFalse();
            roles.set("[{\"name\":\"admin\"}]"); assertThat(adapter.lookup("person").canRead("test-team", null)).isTrue();
            user.set("{\"enabled\":false}"); assertThat(adapter.lookup("person").enabled()).isFalse();
            assertThat(roleCalls).hasValue(3); assertThat(tokenCalls).hasValue(1);
        } finally { server.stop(0); }
    }
    @Test void absentConfigurationFailsClosed() {
        var adapter = new CurrentIdentityService(new WikiProperties(), new ObjectMapper());
        assertThatThrownBy(() -> adapter.lookup("person")).isInstanceOf(IdentityUnavailableException.class);
    }
    @Test void mentionExtractionExcludesEmailAndCode() {
        assertThat(WikiNotificationIntents.mentionLogins("Hi @alice @alice mail@example.com `@inline`\n```java\n@fenced\n```\n~~~\n@tilde\n~~~\nHi @bob."))
            .containsExactly("alice", "bob");
    }
}
