package com.airline.apigateway.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

@Configuration
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        // #region agent log
        private static void debugLog(
                        String hypothesisId, String location, String message, Map<String, Object> data) {
                try {
                        String json = "{\"sessionId\":\"5fcc3f\",\"hypothesisId\":\""
                                        + hypothesisId
                                        + "\",\"location\":\""
                                        + location
                                        + "\",\"message\":\""
                                        + message
                                        + "\",\"data\":"
                                        + data.toString().replace('=', ':')
                                        + ",\"timestamp\":"
                                        + System.currentTimeMillis()
                                        + "}";
                        Files.writeString(
                                        Path.of("debug-5fcc3f.log"),
                                        json + System.lineSeparator(),
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.APPEND);
                } catch (Exception ignored) {
                }
        }
        // #endregion

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http.csrf(AbstractHttpConfigurer::disable)
                                .cors(Customizer.withDefaults())
                                .sessionManagement(
                                                session -> session
                                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(
                                                auth -> auth

                                                                // CORS preflight, app errors and public auth endpoints
                                                                .requestMatchers(
                                                                                org.springframework.http.HttpMethod.OPTIONS,
                                                                                "/**")
                                                                .permitAll()
                                                                .requestMatchers("/error")
                                                                .permitAll()
                                                                .requestMatchers(
                                                                                "/api/auth/register",
                                                                                "/api/auth/login",
                                                                                "/api/auth/refresh",
                                                                                // Simulated payment callback: the
                                                                                // payment service
                                                                                // validates its shared demo secret.
                                                                                "/api/payments/webhook")
                                                                .permitAll()
                                                                .requestMatchers(
                                                                                "/api/flights/admin/**",
                                                                                "/api/admin/**",
                                                                                "/api/auth/promote/**")
                                                                .hasRole("ADMIN")

                                                                // Flight search must be available before a user signs
                                                                // in.
                                                                .requestMatchers(
                                                                                org.springframework.http.HttpMethod.GET,
                                                                                "/api/flights/**")
                                                                .permitAll()

                                                                // Eureka / actuator
                                                                .requestMatchers("/actuator/**")
                                                                .permitAll()

                                                                // Everything else
                                                                .anyRequest()
                                                                .authenticated())
                                .exceptionHandling(
                                                exceptions -> exceptions
                                                                .authenticationEntryPoint(
                                                                                (request, response, authException) -> {
                                                                                        // #region agent log
                                                                                        Authentication auth = SecurityContextHolder
                                                                                                        .getContext()
                                                                                                        .getAuthentication();
                                                                                        debugLog(
                                                                                                        "E",
                                                                                                        "SecurityConfig:401-entry-point",
                                                                                                        "unauthenticated request blocked",
                                                                                                        Map.of(
                                                                                                                        "method",
                                                                                                                        request.getMethod(),
                                                                                                                        "uri",
                                                                                                                        request.getRequestURI(),
                                                                                                                        "authPresent",
                                                                                                                        auth != null,
                                                                                                                        "authName",
                                                                                                                        auth != null
                                                                                                                                        ? String.valueOf(
                                                                                                                                                        auth.getName())
                                                                                                                                        : "null",
                                                                                                                        "exception",
                                                                                                                        authException.getClass()
                                                                                                                                        .getSimpleName()));
                                                                                        // #endregion
                                                                                        response.setStatus(
                                                                                                        HttpServletResponse.SC_UNAUTHORIZED);
                                                                                        response.setContentType(
                                                                                                        "application/json");
                                                                                        response
                                                                                                        .getWriter()
                                                                                                        .write(
                                                                                                                        "{\"error\":\"Authentication required\"}");
                                                                                })
                                                                .accessDeniedHandler(
                                                                                (request, response,
                                                                                                accessDeniedException) -> {
                                                                                        // #region agent log
                                                                                        Authentication auth = SecurityContextHolder
                                                                                                        .getContext()
                                                                                                        .getAuthentication();
                                                                                        debugLog(
                                                                                                        "F",
                                                                                                        "SecurityConfig:403-denied",
                                                                                                        "access denied",
                                                                                                        Map.of(
                                                                                                                        "method",
                                                                                                                        request.getMethod(),
                                                                                                                        "uri",
                                                                                                                        request.getRequestURI(),
                                                                                                                        "authPresent",
                                                                                                                        auth != null,
                                                                                                                        "authName",
                                                                                                                        auth != null
                                                                                                                                        ? String.valueOf(
                                                                                                                                                        auth.getName())
                                                                                                                                        : "null",
                                                                                                                        "authorities",
                                                                                                                        auth != null
                                                                                                                                        ? auth.getAuthorities()
                                                                                                                                                        .toString()
                                                                                                                                        : "null",
                                                                                                                        "exception",
                                                                                                                        accessDeniedException
                                                                                                                                        .getClass()
                                                                                                                                        .getSimpleName()));
                                                                                        // #endregion
                                                                                        response.setStatus(
                                                                                                        HttpServletResponse.SC_FORBIDDEN);
                                                                                        response.setContentType(
                                                                                                        "application/json");
                                                                                        response
                                                                                                        .getWriter()
                                                                                                        .write(
                                                                                                                        "{\"error\":\"Access denied\"}");
                                                                                }))
                                .addFilterBefore(
                                                jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
