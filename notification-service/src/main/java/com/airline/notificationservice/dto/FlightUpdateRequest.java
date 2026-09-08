package com.airline.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FlightUpdateRequest(
        @NotBlank @Email String recipientEmail,
        @NotBlank String flightNumber,
        String origin,
        String destination,
        String scheduledDeparture,
        String newDeparture,
        String gate,
        String statusMessage
) {}
