package com.airline.flightservice;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   GatewayRoleFilter gatewayRoleFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Admin mutation endpoints — require ADMIN role
                .requestMatchers("/api/flights/admin/**").hasRole("ADMIN")
                // Public read endpoints
                .requestMatchers("/api/flights", "/api/flights/search",
                                 "/api/flights/airports", "/api/flights/{id}").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(gatewayRoleFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Reads the role injected by the API Gateway (X-User-Role header) and populates
     * the SecurityContext, so Spring Security role checks work without re-validating
     * the JWT here. If the JWT_SECRET env var is set, the JWT is also verified directly
     * as a defence-in-depth measure.
     */
    @Component
    static class GatewayRoleFilter extends OncePerRequestFilter {

        private final SecretKey secretKey;

        GatewayRoleFilter(@Value("${jwt.secret:}") String secret) {
            this.secretKey = (secret != null && secret.length() >= 32)
                    ? Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))
                    : null;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain)
                throws ServletException, IOException {

            String role = null;
            String subject = "anonymous";

            String authHeader = request.getHeader("Authorization");
            if (secretKey != null && authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    Claims claims = Jwts.parser()
                            .verifyWith(secretKey)
                            .build()
                            .parseSignedClaims(authHeader.substring(7))
                            .getPayload();
                    role = claims.get("role", String.class);
                    subject = claims.getSubject();
                } catch (Exception ignored) {
                    // Invalid token — fall through to header-based check
                }
            }

            // Fall back to gateway-injected header (trusted internal network)
            if (role == null) {
                role = request.getHeader("X-User-Role");
                subject = request.getHeader("X-User-Email");
            }

            if (role != null && subject != null) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                subject, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            chain.doFilter(request, response);
        }
    }
}
