package com.airline.pricingservice.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record PriceQuote(
        Long flightId, String cabin, BigDecimal baseFare, BigDecimal multiplier,
        BigDecimal promoDiscount, BigDecimal frequentFlyerPointsDiscount,
        BigDecimal total, List<String> appliedRules) {}
