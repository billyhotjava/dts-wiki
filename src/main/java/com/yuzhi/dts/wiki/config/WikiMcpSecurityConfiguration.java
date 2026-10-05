package com.yuzhi.dts.wiki.config;

import com.yuzhi.dts.wiki.security.SecurityUtils;
import com.yuzhi.dts.wiki.security.WikiMcpTokenPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@ConditionalOnProperty(name = "application.wiki.mcp.enabled", havingValue = "true")
public class WikiMcpSecurityConfiguration {
    @Bean
    @Order(-2)
    SecurityFilterChain wikiMcpChain(HttpSecurity http, JwtDecoder jwtDecoder, WikiMcpProperties settings,
        @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}") String issuer) throws Exception {
        URI resource = URI.create(settings.getResourceUri());
        if (resource.getHost() == null || !"/mcp".equals(resource.getPath()) || resource.getQuery() != null || resource.getFragment() != null
            || resource.getUserInfo() != null || !("https".equals(resource.getScheme())
                || "http".equals(resource.getScheme()) && java.util.Set.of("localhost", "127.0.0.1").contains(resource.getHost()))) {
            throw new IllegalArgumentException("MCP resource URI must be HTTPS /mcp, or localhost HTTP for development");
        }
        var converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("preferred_username");
        converter.setJwtGrantedAuthoritiesConverter(token -> SecurityUtils.extractAuthorityFromClaims(token.getClaims()));
        var entry = (org.springframework.security.web.AuthenticationEntryPoint) (request, response, failure) -> {
            response.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + settings.metadataUri() + "\", scope=\"wiki.read\"");
            response.setStatus(401);
        };
        http.securityMatcher("/mcp", "/.well-known/oauth-protected-resource", "/.well-known/oauth-protected-resource/mcp")
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.requestMatchers("/.well-known/**").permitAll()
                .anyRequest().access((authentication, context) -> {
                    var value = authentication.get();
                    return new org.springframework.security.authorization.AuthorizationDecision(value instanceof JwtAuthenticationToken jwt
                        && jwt.isAuthenticated() && WikiMcpTokenPolicy.hasScope(jwt.getToken(), "wiki.read"));
                }))
            .oauth2ResourceServer(oauth -> oauth.protectedResourceMetadata(metadata -> metadata.protectedResourceMetadataCustomizer(builder ->
                builder.resource(settings.getResourceUri()).authorizationServer(issuer).scope("wiki.read").scope("wiki.write")
                    .bearerMethod("header").resourceName("DTS Wiki"))).authenticationEntryPoint(entry).jwt(jwt -> jwt
                .decoder(raw -> WikiMcpTokenPolicy.validate(jwtDecoder.decode(raw), settings, issuer)).jwtAuthenticationConverter(converter)))
            .exceptionHandling(errors -> errors.authenticationEntryPoint(entry).accessDeniedHandler((request, response, failure) -> {
                response.setHeader("WWW-Authenticate", "Bearer error=\"insufficient_scope\", scope=\"wiki.read\", resource_metadata=\"" + settings.metadataUri() + "\"");
                response.setStatus(403);
            }))
            .addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                    String origin = request.getHeader("Origin");
                    String ownOrigin = resource.getScheme() + "://" + resource.getRawAuthority();
                    if (origin != null && !origin.equals(ownOrigin) && !settings.getAllowedOrigins().contains(origin)) {
                        response.setStatus(403); return;
                    }
                    chain.doFilter(request, response);
                }
            }, BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
