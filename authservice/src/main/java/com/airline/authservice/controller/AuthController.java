package com.airline.authservice.controller;

import com.airline.authservice.dto.AuthResponse;
import com.airline.authservice.dto.LoginRequest;
import com.airline.authservice.dto.RefreshTokenRequest;
import com.airline.authservice.dto.RegisterRequest;
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
    public List<Map<String, Object>> users() {
        return authService.listUsers();
    }

    @PostMapping("/promote/{userId}")
    public ResponseEntity<Map<String, Object>> promote(@PathVariable Long userId) {

        User promoted = authService.promoteToAdmin(userId);

        return ResponseEntity.ok(
                Map.of(
                        "message", "User promoted to ADMIN",
                        "userId", promoted.getId(),
                        "email", promoted.getEmail(),
                        "role", promoted.getRole().name()));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody EmailRequest request) {
        authService.requestPasswordReset(request.email());
        // Do not disclose whether an account exists; notification-service delivers the reset link.
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
