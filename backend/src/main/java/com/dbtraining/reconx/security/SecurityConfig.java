package com.dbtraining.reconx.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT authentication and HTTP-method RBAC for the ReconX API.
 *
 * Paths in this configuration are relative to the servlet context path. The
 * configured {@code /api} context therefore makes {@code /v1/trades/**}
 * available at {@code /api/v1/trades/**}.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v1/api-docs",
                                "/v1/api-docs/**",
                                "/v3/api-docs/**",
                                "/h2/**",
                                // Native EventSource cannot attach the API's Bearer header.
                                // Keep this read-only stream public; mutations remain protected.
                                "/v1/trades/stream"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/trades/**")
                        .hasAnyRole("VIEWER", "TRADER", "RECON_ANALYST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/v1/trades")
                        .hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/v1/trades/**")
                        .hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/v1/trades/**")
                        .hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/trades/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/v1/admin/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/recon/**")
                        .hasAnyRole("VIEWER", "RECON_ANALYST", "ADMIN")
                        .requestMatchers("/v1/recon/**")
                        .hasAnyRole("RECON_ANALYST", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/audit/**")
                        .hasAnyRole("VIEWER", "RECON_ANALYST", "ADMIN")
                        .requestMatchers("/v1/audit/**")
                        .hasAnyRole("RECON_ANALYST", "ADMIN")
                        .anyRequest().authenticated())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
