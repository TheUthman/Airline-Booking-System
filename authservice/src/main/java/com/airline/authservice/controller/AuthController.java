package com.airline.authservice.controller;

import com.airline.authservice.dto.AuthResponse;
import com.airline.authservice.dto.LoginRequest;
import com.airline.authservice.dto.RefreshTokenRequest;
import com.airline.authservice.dto.RegisterRequest;
import com.airline.authservice.entity.Role;
import com.airline.authservice.entity.User;
import com.airline.authservice.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public Map<String, Object> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return authService.listUsers(page, size);
    }

    @PostMapping("/promote/{userId}")
    public ResponseEntity<Map<String, Object>> promote(
            @PathVariable Long userId,
            @RequestParam(required = false) String role,
            @RequestBody(required = false) Map<String, String> body) {

        Role targetRole = Role.ADMIN;
        String requestedRole = (body != null && body.get("role") != null) ? body.get("role") : role;
        if (requestedRole != null && !requestedRole.isBlank()) {
            targetRole = parseRole(requestedRole);
        }

        User updated = authService.updateUserRole(userId, targetRole);

        return ResponseEntity.ok(
                Map.of(
                        "message", "User role updated to " + updated.getRole().name(),
                        "userId", updated.getId(),
                        "email", updated.getEmail(),
                        "role", updated.getRole().name()));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        String roleStr = body != null ? body.get("role") : null;
        if (roleStr == null || roleStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
        }
        Role targetRole = parseRole(roleStr);
        User updated = authService.updateUserRole(userId, targetRole);

        return ResponseEntity.ok(
                Map.of(
                        "message", "User role updated successfully",
                        "userId", updated.getId(),
                        "email", updated.getEmail(),
                        "role", updated.getRole().name()));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Long userId) {
        authService.deleteUser(userId);
        return ResponseEntity.ok(
                Map.of(
                        "message", "User deleted successfully",
                        "userId", userId));
    }

    private Role parseRole(String roleStr) {
        String normalized = roleStr.trim().toUpperCase();
        if ("ADMINISTRATOR".equals(normalized)) {
            return Role.ADMIN;
        } else if ("USER".equals(normalized) || "CUSTOMER".equals(normalized)) {
            return Role.PASSENGER;
        }
        return Role.valueOf(normalized);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody EmailRequest request) {
        authService.requestPasswordReset(request.email());
        // Always return 202 regardless of whether the account exists \u2014 authService silently no-ops for unknown emails
        return ResponseEntity.accepted().body(Map.of("message", "If the account exists, a reset email will be sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verification")
    public ResponseEntity<Map<String, String>> requestVerification(@Valid @RequestBody EmailRequest request) {
        authService.requestEmailVerification(request.email());
        return ResponseEntity.accepted().body(Map.of("message", "Verification email requested"));
    }

    @PostMapping("/verification/confirm")
    public ResponseEntity<Void> confirmVerification(@Valid @RequestBody VerificationRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    record EmailRequest(@Email @NotBlank String email) {}
    record VerificationRequest(@NotBlank String token) {}
    record ResetPasswordRequest(@NotBlank String token, @NotBlank @Size(min = 8) String password) {}
}
