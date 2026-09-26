package com.yuzhi.dts.wiki.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.PREFERRED_USERNAME;

import com.yuzhi.dts.wiki.security.*;
import com.yuzhi.dts.wiki.security.oauth2.AudienceValidator;
import com.yuzhi.dts.wiki.security.oauth2.CustomClaimConverter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    private final boolean apiDocsOpen;

    @Value("${spring.security.oauth2.client.provider.oidc.issuer-uri}")
    private String issuerUri;

    // DTS-WIKI: customized (Sprint-6 design 03 S3): apiDocsOpen gates /v3/api-docs
    // (public only with the api-docs profile, closed in prod).
    public SecurityConfiguration(JHipsterProperties jHipsterProperties, Environment environment) {
        this.jHipsterProperties = jHipsterProperties;
        this.apiDocsOpen = environment.acceptsProfiles(Profiles.of("api-docs"));
    }

    // DTS-WIKI: customized (design 10 S3.7): static frontend resources carry their own
    // Cache-Control from WebConfigurer; Spring Security's default no-store header must
    // not override them. This chain only matches hashed assets and the entry page.
    @Bean
    @org.springframework.core.annotation.Order(-1)
    public SecurityFilterChain staticResourcesChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/assets/**", "/index.html", "/favicon.ico").authorizeHttpRequests(authz -> authz.anyRequest().permitAll()).headers(h -> h.cacheControl(c -> c.disable()));
        return http.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {        http.cors(withDefaults())
            .csrf(csrf ->
                csrf
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            // DTS-WIKI: customized (Sprint-6 W3 fix): session-bearing anonymous API calls must
            // get 401 (so the SPA redirects to Keycloak itself) instead of a 302 to the login
            // page (which axios follows silently and resolves as bogus "account" HTML).
            .exceptionHandling(handling ->
                handling.defaultAuthenticationEntryPointFor(
                    new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                        org.springframework.http.HttpStatus.UNAUTHORIZED
                    ),
                    org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.withDefaults()
                        .matcher("/api/**")
                )
            )
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                // DTS-WIKI: customized (Sprint-6 design 03 S3): business APIs live under
                // /api/wiki/** (authenticated, fine-grained checks in SpaceAccessService);
                // every generated entity endpoint is ROLE_ADMIN only. Keep this list in sync
                // with jhipster/dts-wiki.jdl entities when the model changes.
                authz
                    .requestMatchers("/api/authenticate").permitAll()
                    .requestMatchers("/api/auth-info").permitAll()
                    .requestMatchers("/api/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(
                        "/api/spaces/**",
                        "/api/sync-roots/**",
                        "/api/sync-states/**",
                        "/api/pages/**",
                        "/api/page-versions/**",
                        "/api/attachments/**",
                        "/api/comments/**",
                        "/api/page-drafts/**",
                        "/api/page-watches/**",
                        "/api/notifications/**",
                        "/api/activity-events/**",
                        "/api/labels/**",
                        "/api/sync-conflicts/**",
                        "/api/sync-outboxes/**",
                        "/api/authorities/**",
                        "/api/users/**"
                    ).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers("/api/wiki/**").authenticated()
                    .requestMatchers("/api/**").authenticated()
                    // DTS-WIKI: customized (Sprint-6 design 03 S3): the frontend shell is
                    // public; data APIs still require login.
                    .requestMatchers("/", "/index.html", "/assets/**", "/favicon*").permitAll()
                    .requestMatchers("/s/**", "/p/**", "/docs/**", "/worklog/**", "/sandbox/**").permitAll()
                    .requestMatchers("/search", "/conflicts/**", "/admin/**").permitAll()
                    .requestMatchers("/v3/api-docs/**").access((authentication, context) ->
                        new org.springframework.security.authorization.AuthorizationDecision(apiDocsOpen))
                    .requestMatchers("/management/health").permitAll()
                    .requestMatchers("/management/health/**").permitAll()
                    .requestMatchers("/management/info").permitAll()
                    .requestMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .oauth2Login(oauth2 -> oauth2.loginPage("/").userInfoEndpoint(userInfo -> userInfo.oidcUserService(this.oidcUserService())))
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter())))
            .oauth2Client(withDefaults());
        return http.build();
    }

    Converter<Jwt, AbstractAuthenticationToken> authenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
            new Converter<Jwt, Collection<GrantedAuthority>>() {
                @Override
                public Collection<GrantedAuthority> convert(Jwt jwt) {
                    return SecurityUtils.extractAuthorityFromClaims(jwt.getClaims());
                }
            }
        );
        jwtAuthenticationConverter.setPrincipalClaimName(PREFERRED_USERNAME);
        return jwtAuthenticationConverter;
    }

    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return userRequest -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);
            return new DefaultOidcUser(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo(), PREFERRED_USERNAME);
        };
    }

    /**
     * Map authorities from "groups" or "roles" claim in ID Token.
     *
     * @return a {@link GrantedAuthoritiesMapper} that maps groups from
     * the IdP to Spring Security Authorities.
     */
    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                // Check for OidcUserAuthority because Spring Security 5.2 returns
                // each scope as a GrantedAuthority, which we don't care about.
                if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                    mappedAuthorities.addAll(SecurityUtils.extractAuthorityFromClaims(oidcUserAuthority.getUserInfo().getClaims()));
                }
            });
            return mappedAuthorities;
        };
    }

    @Bean
    JwtDecoder jwtDecoder(ClientRegistrationRepository clientRegistrationRepository, RestTemplateBuilder restTemplateBuilder) {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(jHipsterProperties.getSecurity().getOauth2().getAudience());
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);
        jwtDecoder.setClaimSetConverter(
            new CustomClaimConverter(clientRegistrationRepository.findByRegistrationId("oidc"), restTemplateBuilder.build())
        );

        return jwtDecoder;
    }

    /**
     * Custom CSRF handler to provide BREACH protection for Single-Page Applications (SPA).
     *
     * @see <a href="https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript-spa">Spring Security Documentation - Integrating with CSRF Protection</a>
     * @see <a href="https://github.com/jhipster/generator-jhipster/pull/25907">JHipster - use customized SpaCsrfTokenRequestHandler to handle CSRF token</a>
     * @see <a href="https://stackoverflow.com/q/74447118/65681">CSRF protection not working with Spring Security 6</a>
     */
    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            /*
             * Always use XorCsrfTokenRequestAttributeHandler to provide BREACH protection of
             * the CsrfToken when it is rendered in the response body.
             */
            this.xor.handle(request, response, csrfToken);

            // Render the token value to a cookie by causing the deferred token to be loaded.
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            /*
             * If the request contains a request header, use CsrfTokenRequestAttributeHandler
             * to resolve the CsrfToken. This applies when a single-page application includes
             * the header value automatically, which was obtained via a cookie containing the
             * raw CsrfToken.
             */
            if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
                return this.plain.resolveCsrfTokenValue(request, csrfToken);
            }
            /*
             * In all other cases (e.g. if the request contains a request parameter), use
             * XorCsrfTokenRequestAttributeHandler to resolve the CsrfToken. This applies
             * when a server-side rendered form includes the _csrf request parameter as a
             * hidden input.
             */
            return this.xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
