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

import java.util.Map;

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
}
