package com.airline.pricingservice.api.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceRequest(
        @NotNull @Positive Long flightId,
        @NotNull @Positive BigDecimal baseFare,
        @NotNull @FutureOrPresent LocalDate departureDate,
        @Min(0) int availableSeats,
        @Min(1) int totalSeats,
        @NotBlank String cabin,
        String promoCode,
        @PositiveOrZero int frequentFlyerPoints) {}
