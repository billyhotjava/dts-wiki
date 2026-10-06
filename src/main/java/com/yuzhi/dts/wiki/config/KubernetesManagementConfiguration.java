package com.yuzhi.dts.wiki.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Kubernetes probes and metrics use a separate internal listener, never the application route. */
@Configuration(proxyBeanMethods = false)
@Profile("kubernetes")
public class KubernetesManagementConfiguration {
    @Bean
    @Order(-2)
    SecurityFilterChain managementChain(HttpSecurity http,
        @Value("${management.server.port}") int managementPort,
        @Value("${server.port:8080}") int applicationPort) throws Exception {
        if (managementPort <= 0 || managementPort == applicationPort) {
            throw new IllegalArgumentException("Kubernetes management requires a separate positive port");
        }
        http.securityMatcher(request -> request.getLocalPort() == managementPort
                && request.getRequestURI().startsWith("/management/"))
            .authorizeHttpRequests(access -> access
                .requestMatchers(HttpMethod.GET, "/management/health", "/management/health/**",
                    "/management/info", "/management/prometheus").permitAll()
                .anyRequest().denyAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
