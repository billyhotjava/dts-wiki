package com.yuzhi.dts.wiki.service.wiki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuzhi.dts.wiki.config.WikiProperties;
import com.yuzhi.dts.wiki.domain.Space;
import com.yuzhi.dts.wiki.security.KeycloakAuthorityMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Authenticated Keycloak adapter. Local role projections cannot authorize recipients. */
@Service
public class CurrentIdentityService {
    public record Identity(boolean enabled, Set<String> authorities, String verifiedEmail) {
        public boolean canRead(String slug, String accessRole) {
            String authority = accessRole == null ? KeycloakAuthorityMapper.spaceAuthority(slug) : KeycloakAuthorityMapper.spaceRoleAuthority(accessRole);
            return enabled && (authorities.contains("ROLE_ADMIN") || authority != null && authorities.contains(authority));
        }
        public boolean canRead(Space space) { return canRead(space.getSlug(), space.getAccessRole()); }
    }
    private final WikiProperties.Directory settings;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();
    private String token;
    private Instant tokenExpires = Instant.EPOCH;

    public CurrentIdentityService(WikiProperties properties, ObjectMapper json) {
        this.settings = properties.getDirectory(); this.json = json;
    }

    public Identity lookup(String userId) {
        // No identity call can hold a database transaction during an external timeout.
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException("Identity lookup must run outside a database transaction");
        validateSettings();
        String admin = settings.getBaseUrl().replaceAll("/+$", "") + "/admin/realms/" + segment(settings.getRealm());
        JsonNode user = get(admin + "/users/" + segment(userId), true);
        if (user == null || !user.path("enabled").asBoolean(false)) return new Identity(false, Set.of(), null);
        JsonNode roles = get(admin + "/users/" + segment(userId) + "/role-mappings/clients/" + segment(settings.getWikiClientUuid()) + "/composite", false);
        if (!roles.isArray()) throw new IdentityUnavailableException();
        java.util.List<String> names = new java.util.ArrayList<>();
        roles.forEach(role -> names.add(role.path("name").asText()));
        Set<String> authorities = KeycloakAuthorityMapper.map(names).stream().map(org.springframework.security.core.GrantedAuthority::getAuthority).collect(Collectors.toUnmodifiableSet());
        String email = user.path("emailVerified").asBoolean(false) ? user.path("email").asText(null) : null;
        return new Identity(true, authorities, email);
    }

    private JsonNode get(String url, boolean missingUserAllowed) {
        RemoteResponse response = request(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).header("Authorization", "Bearer " + credential()).GET().build());
        if (missingUserAllowed && response.statusCode() == 404) return null;
        if (response.statusCode() != 200) { if (response.statusCode() == 401) expireToken(); throw new IdentityUnavailableException(); }
        return parse(response.body());
    }

    private synchronized String credential() {
        if (token != null && Instant.now().isBefore(tokenExpires)) return token;
        String body = "grant_type=client_credentials&client_id=" + segment(settings.getClientId()) + "&client_secret=" + segment(settings.getClientSecret());
        String url = settings.getBaseUrl().replaceAll("/+$", "") + "/realms/" + segment(settings.getRealm()) + "/protocol/openid-connect/token";
        var response = request(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(2)).header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build());
        if (response.statusCode() != 200) throw new IdentityUnavailableException();
        JsonNode result = parse(response.body()); token = result.path("access_token").asText();
        if (token.isBlank()) throw new IdentityUnavailableException();
        tokenExpires = Instant.now().plusSeconds(Math.max(1, Math.min(60, result.path("expires_in").asLong(60) - 10)));
        return token;
    }
    private synchronized void expireToken() { tokenExpires = Instant.EPOCH; }
    private record RemoteResponse(int statusCode, String body) {}
    private RemoteResponse request(HttpRequest request) {
        var future = http.sendAsync(request, ignored -> new BoundedBody());
        try {
            var response = future.get(3, java.util.concurrent.TimeUnit.SECONDS);
            return new RemoteResponse(response.statusCode(), response.body());
        }
        catch (InterruptedException e) { future.cancel(true); Thread.currentThread().interrupt(); throw new IdentityUnavailableException(); }
        catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) { future.cancel(true); throw new IdentityUnavailableException(); }
    }
    private static final class BoundedBody implements HttpResponse.BodySubscriber<String> {
        private final HttpResponse.BodySubscriber<String> delegate = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
        private java.util.concurrent.Flow.Subscription subscription;
        private long size;
        public java.util.concurrent.CompletionStage<String> getBody() { return delegate.getBody(); }
        public void onSubscribe(java.util.concurrent.Flow.Subscription value) { subscription = value; delegate.onSubscribe(value); }
        public void onNext(java.util.List<java.nio.ByteBuffer> buffers) {
            for (var buffer : buffers) size += buffer.remaining();
            if (size > 1_000_000) { subscription.cancel(); delegate.onError(new IOException("Identity response too large")); }
            else delegate.onNext(buffers);
        }
        public void onError(Throwable error) { delegate.onError(error); }
        public void onComplete() { delegate.onComplete(); }
    }
    private JsonNode parse(String body) {
        if (body.length() > 1_000_000) throw new IdentityUnavailableException();
        try { return json.readTree(body); } catch (IOException e) { throw new IdentityUnavailableException(); }
    }
    private void validateSettings() {
        try {
            URI uri = URI.create(settings.getBaseUrl());
            if (uri.getHost() == null || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()) && Set.of("localhost", "127.0.0.1").contains(uri.getHost()))
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || settings.getRealm().isBlank() || settings.getWikiClientUuid().isBlank() || settings.getClientId().isBlank() || settings.getClientSecret().isBlank()) throw new IdentityUnavailableException();
        } catch (IllegalArgumentException e) { throw new IdentityUnavailableException(); }
    }
    private static String segment(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
}
