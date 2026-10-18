package com.airline.flightservice;

import java.math.BigDecimal;

/** Transparent, cost-based fare estimate. Values are estimates based on configured assumptions. */
public record FareEstimate(
        String origin,
        String destination,
        String aircraftCode,
        double routeDistanceKm,
        BigDecimal estimatedFlightHours,
        int seatCapacity,
        BigDecimal expectedLoadFactor,
        int expectedPassengers,
        BigDecimal fuelCost,
        BigDecimal operatingCost,
        BigDecimal maintenanceCost,
        BigDecimal crewCost,
        BigDecimal airportCharges,
        BigDecimal totalEstimatedOperatingCost,
        BigDecimal estimatedOtherRevenue,
        BigDecimal targetProfitMargin,
        BigDecimal suggestedEconomyBaseFare,
        BigDecimal suggestedBusinessBaseFare,
        String currency,
        String note) {}
