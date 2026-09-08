package com.airline.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentConfirmationRequest(
        @NotBlank @Email String recipientEmail,
        @NotNull @Positive Long bookingId,
        Long paymentId,
        String providerReference,
        @NotNull @Positive BigDecimal amount,
        String paymentMethod
) {}
