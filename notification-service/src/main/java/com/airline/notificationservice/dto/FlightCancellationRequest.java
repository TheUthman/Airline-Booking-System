package com.airline.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FlightCancellationRequest(
        @NotBlank @Email String recipientEmail,
        @NotBlank String flightNumber,
        String origin,
        String destination,
        String departureDate,
        String reason,
        String refundPolicyUrl
) {}
