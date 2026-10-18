package com.airline.pricingservice.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read-only subset returned by Flight Service. */
public record FlightPricingData(
        Long id,
        BigDecimal fare,
        BigDecimal businessFare,
        int availableSeats,
        int totalSeats,
        LocalDateTime departureTime,
        boolean active) {}
