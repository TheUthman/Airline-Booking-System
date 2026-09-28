package com.airline.pricingservice.domain;

import com.airline.pricingservice.api.dto.PriceRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** Pure, deterministic pricing rules. It has no HTTP, database, or service-discovery dependency. */
@Component
public class PricingPolicy {
    public BigDecimal multiplier(PriceRequest request) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), request.departureDate());
        if (days < 0) throw new IllegalArgumentException("Departure date cannot be in the past");
        BigDecimal multiplier = BigDecimal.ONE;
        if (days <= 3) multiplier = multiplier.multiply(new BigDecimal("1.35"));
        else if (days <= 14) multiplier = multiplier.multiply(new BigDecimal("1.15"));
        double loadFactor = 1.0 - ((double) request.availableSeats() / request.totalSeats());
        if (loadFactor >= .85) multiplier = multiplier.multiply(new BigDecimal("1.25"));
        else if (loadFactor >= .65) multiplier = multiplier.multiply(new BigDecimal("1.10"));
        return multiplier.multiply(cabinMultiplier(request.cabin()));
    }
    public boolean eligiblePromo(String code) { return code != null && code.equalsIgnoreCase("WELCOME10"); }
    private BigDecimal cabinMultiplier(String cabin) {
        return switch (cabin.toUpperCase()) {
            case "BUSINESS" -> new BigDecimal("1.80");
            case "FIRST" -> new BigDecimal("2.75");
            default -> BigDecimal.ONE;
        };
    }
}
