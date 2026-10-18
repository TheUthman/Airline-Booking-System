package com.airline.pricingservice.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Client quote request. baseFare, date and seat counts remain optional for backwards
 * compatibility but are never trusted by PricingQuoteService; authoritative values are loaded
 * from Flight Service.
 */
public record PriceRequest(
        @NotNull @Positive Long flightId,
        @PositiveOrZero BigDecimal baseFare,
        @FutureOrPresent LocalDate departureDate,
        @Min(0) int availableSeats,
        @Min(0) int totalSeats,
        @NotBlank @Pattern(regexp = "(?i)ECONOMY|BUSINESS") String cabin,
        String promoCode,
        @PositiveOrZero int frequentFlyerPoints) {}
