package com.airline.apigateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null
                || path.equals("/error")
                || path.startsWith("/actuator")
                || path.startsWith("/api/auth/")
                || path.startsWith("/api/payments/webhook")
                || ("GET".equalsIgnoreCase(request.getMethod())
                        && path.startsWith("/api/flights/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // #region agent log
        debugLog(
                "A",
                "JwtAuthenticationFilter:entry",
                "request received",
                Map.of(
                        "method", request.getMethod(),
                        "uri", request.getRequestURI(),
                        "servletPath", String.valueOf(request.getServletPath()),
                        "pathInfo", String.valueOf(request.getPathInfo()),
                        "hasAuthHeader", request.getHeader("Authorization") != null));
        // #endregion

        String authHeader = request.getHeader("Authorization");

        // No JWT supplied
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            SecurityContextHolder.clearContext();
            // #region agent log
            debugLog(
                    "B",
                    "JwtAuthenticationFilter:no-token",
                    "passing through without authentication",
                    Map.of("method", request.getMethod(), "uri", request.getRequestURI()));
            // #endregion
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Invalid JWT
        if (!jwtService.isTokenValid(token)) {
            // #region agent log
            debugLog(
                    "C",
                    "JwtAuthenticationFilter:invalid-token",
                    "rejecting invalid token",
                    Map.of("method", request.getMethod(), "uri", request.getRequestURI()));
            // #endregion

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");

            return;
        }

        // Extract information from JWT
        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);

        // Create authenticated user
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        // Tell Spring Security that this request is authenticated
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // #region agent log
        debugLog(
                "D",
                "JwtAuthenticationFilter:authenticated",
                "authentication set",
                Map.of(
                        "method", request.getMethod(),
                        "uri", request.getRequestURI(),
                        "role", role,
                        "authority", "ROLE_" + role));
        // #endregion

        HttpServletRequest decoratedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("X-User-Email".equalsIgnoreCase(name))
                    return email;
                if ("X-User-Role".equalsIgnoreCase(name))
                    return role;
                return super.getHeader(name);
            }

            @Override
            public java.util.Enumeration<String> getHeaders(String name) {
                if ("X-User-Email".equalsIgnoreCase(name))
                    return Collections.enumeration(List.of(email));
                if ("X-User-Role".equalsIgnoreCase(name))
                    return Collections.enumeration(List.of(role));
                return super.getHeaders(name);
            }
        };

        filterChain.doFilter(decoratedRequest, response);
    }
}
