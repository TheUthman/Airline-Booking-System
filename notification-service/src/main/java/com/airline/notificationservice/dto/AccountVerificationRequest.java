package com.airline.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountVerificationRequest(
        @NotBlank @Email String recipientEmail,
        @NotBlank String verificationUrl,
        String userName
) {}
