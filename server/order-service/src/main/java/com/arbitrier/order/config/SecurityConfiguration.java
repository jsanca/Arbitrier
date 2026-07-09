package com.arbitrier.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Resource Server configuration for order-service.
 *
 * <p>All API endpoints require a valid JWT (Bearer token). Actuator endpoints are open
 * to allow health checks from Kubernetes without credentials.
 *
 * <p>The {@link JwtDecoder} bean is expected to be provided by Spring Boot's
 * {@code oauth2ResourceServer} auto-configuration when
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} is set in the deployment
 * environment. In tests, a mock {@link JwtDecoder} is supplied by
 * {@code OrderServiceTestConfiguration}.
 *
 * <p>W3C Trace Context propagation ({@code traceparent}/{@code tracestate}) is handled by
 * the OpenTelemetry SDK — Spring Security does not need to be aware of these headers.
 *
 * <p>Layer: config
 * <p>Module: order-service
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    /**
     * Configures the security filter chain.
     *
     * <ul>
     *   <li>Stateless sessions — no HTTP session created or used.</li>
     *   <li>CSRF disabled — this is a stateless REST API.</li>
     *   <li>Actuator endpoints ({@code /actuator/**}) are open for Kubernetes probes.</li>
     *   <li>All other requests require a valid JWT Bearer token.</li>
     * </ul>
     *
     * @param http       the {@link HttpSecurity} builder
     * @param jwtDecoder the JWT decoder; provided by auto-config or a test override
     * @return the configured {@link SecurityFilterChain}
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                )
                .build();
    }
}
