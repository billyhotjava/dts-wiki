package com.yuzhi.dts.wiki.config;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class KubernetesManagementConfigurationTest {
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void start() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.getEnvironment().setActiveProfiles("kubernetes");
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("ports",
            Map.of("management.server.port", 9091, "server.port", 8080)));
        context.register(Fixture.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    void stop() { context.close(); }

    @Test
    void probesAndMetricsAreReadableOnTheInternalListener() throws Exception {
        for (String path : new String[] { "/management/health/liveness", "/management/health/readiness",
            "/management/info", "/management/prometheus" }) {
            mvc.perform(get(path).with(request -> { request.setLocalPort(9091); return request; }))
                .andExpect(status().isOk());
        }
    }

    @Test
    void forwardedPortsCannotExposeMetricsOnTheApplicationListener() throws Exception {
        mvc.perform(get("/management/prometheus").header("X-Forwarded-Port", "9091")
            .header("Forwarded", "host=wiki.example.invalid:9091;proto=https")
            .with(request -> { request.setLocalPort(8080); return request; }))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void managementAccessDoesNotAllowWritesOtherActuatorsOrBusinessApis() throws Exception {
        mvc.perform(post("/management/prometheus").with(request -> { request.setLocalPort(9091); return request; }))
            .andExpect(status().isForbidden());
        mvc.perform(get("/management/env").with(request -> { request.setLocalPort(9091); return request; }))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/wiki/spaces").with(request -> { request.setLocalPort(9091); return request; }))
            .andExpect(status().isUnauthorized());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableWebMvc
    @Import({ KubernetesManagementConfiguration.class, Probe.class })
    static class Fixture {
        @Bean
        SecurityFilterChain fallback(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(access -> access.anyRequest().authenticated())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .csrf(csrf -> csrf.disable()).build();
        }
    }

    @RestController
    static class Probe {
        @GetMapping({ "/management/health/liveness", "/management/health/readiness", "/management/info",
            "/management/prometheus", "/management/env", "/api/wiki/spaces" })
        String read() { return "fixture"; }
    }
}
