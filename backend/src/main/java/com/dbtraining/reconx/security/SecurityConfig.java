package com.dbtraining.reconx.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                /*
                 * Return 401 when authentication is missing or invalid.
                 * Without this, Spring may return 403 for anonymous requests.
                 */
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(
                                (request, response, exception) ->
                                        response.sendError(
                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                "Unauthorized"
                                        )
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // Public endpoints
                        .requestMatchers(
                                "/auth/login",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2/**"
                        ).permitAll()

                        // Anyone with one of the application roles may read trades
                        .requestMatchers(
                                HttpMethod.GET,
                                "/v1/trades/**"
                        ).hasAnyRole(
                                "VIEWER",
                                "TRADER",
                                "RECON_ANALYST",
                                "ADMIN"
                        )

                        // Traders and administrators may create trades
                        .requestMatchers(
                                HttpMethod.POST,
                                "/v1/trades"
                        ).hasAnyRole(
                                "TRADER",
                                "ADMIN"
                        )

                        // Traders and administrators may update trades
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/v1/trades/**"
                        ).hasAnyRole(
                                "TRADER",
                                "ADMIN"
                        )

                        // Traders and administrators may partially update trades
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/v1/trades/**"
                        ).hasAnyRole(
                                "TRADER",
                                "ADMIN"
                        )

                        // Only administrators may delete trades
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/v1/trades/**"
                        ).hasRole("ADMIN")

                        // Reconciliation endpoints
                        .requestMatchers(
                                "/v1/recon/**"
                        ).hasAnyRole(
                                "RECON_ANALYST",
                                "ADMIN"
                        )

                        // Audit endpoints
                        .requestMatchers(
                                "/v1/audit/**"
                        ).hasAnyRole(
                                "RECON_ANALYST",
                                "ADMIN"
                        )

                        // Any unlisted endpoint still requires authentication
                        .anyRequest().authenticated()
                )

                // Allows the H2 console to render in development
                .headers(headers ->
                        headers.frameOptions(frameOptions ->
                                frameOptions.disable()
                        )
                )

                // Process bearer tokens before Spring's standard auth filter
                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}