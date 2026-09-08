package com.airline.notificationservice.controller;

import com.airline.notificationservice.dto.AccountVerificationRequest;
import com.airline.notificationservice.dto.PasswordResetRequest;
import com.airline.notificationservice.service.AccountEmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications/account")
public class AccountNotificationController {

    private final AccountEmailService accountEmailService;

    public AccountNotificationController(AccountEmailService accountEmailService) {
        this.accountEmailService = accountEmailService;
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Map<String, String>> sendPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        accountEmailService.sendPasswordReset(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Password reset email queued for dispatch"));
    }

    @PostMapping("/verification")
    public ResponseEntity<Map<String, String>> sendVerification(@Valid @RequestBody AccountVerificationRequest request) {
        accountEmailService.sendAccountVerification(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Account verification email queued for dispatch"));
    }
}
