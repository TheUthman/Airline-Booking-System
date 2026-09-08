package com.airline.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @NotBlank @Email String recipientEmail,
        @NotBlank String resetUrl,
        String userName,
        Integer expireInMinutes
) {}
