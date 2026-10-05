package com.yuzhi.dts.wiki.security;

import com.yuzhi.dts.wiki.config.WikiMcpProperties;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidationException;

/** MCP tokens are bound to this resource and a personal, preregistered client. */
public final class WikiMcpTokenPolicy {
    private WikiMcpTokenPolicy() {}
    public static Jwt validate(Jwt token, WikiMcpProperties properties, String issuer) {
        String username = token.getClaimAsString("preferred_username");
        if (!token.getAudience().contains(properties.getResourceUri()) || token.getIssuer() == null
            || !issuer.equals(token.getIssuer().toString()) || !properties.getClientIds().contains(token.getClaimAsString("azp"))
            || username == null || username.isBlank() || username.length() > 100 || username.startsWith("service-account-")
            || token.getSubject() == null || token.getSubject().isBlank() || token.getExpiresAt() == null
            || !token.getExpiresAt().isAfter(Instant.now())) {
            throw new JwtValidationException("Invalid MCP personal access token", List.of(new OAuth2Error("invalid_token")));
        }
        return token;
    }
    public static boolean hasScope(Jwt token, String required) {
        String scope = token.getClaimAsString("scope");
        return scope != null && Arrays.asList(scope.split("\\s+")).contains(required);
    }
}
