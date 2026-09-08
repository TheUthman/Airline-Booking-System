package com.airline.notificationservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record BookingConfirmationRequest(
        @NotBlank @Email String recipientEmail,
        @NotNull @Positive Long bookingId,
        String bookingReference,
        String flightNumber,
        String origin,
        String destination,
        String departureTime,
        String arrivalTime,
        BigDecimal totalAmount,
        List<String> passengerNames
) {}
