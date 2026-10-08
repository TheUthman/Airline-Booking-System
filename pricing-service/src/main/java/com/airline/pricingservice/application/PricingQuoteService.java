package com.airline.pricingservice.application;

import com.airline.pricingservice.api.dto.PriceQuote;
import com.airline.pricingservice.api.dto.PriceRequest;
import com.airline.pricingservice.domain.PricingPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PricingQuoteService {
    private final PricingPolicy pricingPolicy;
    public PricingQuoteService(PricingPolicy pricingPolicy) { this.pricingPolicy = pricingPolicy; }
    public PriceQuote quote(PriceRequest request) {
        BigDecimal multiplier = pricingPolicy.multiplier(request);
        BigDecimal subtotal = request.baseFare().multiply(multiplier);
        BigDecimal promoDiscount = pricingPolicy.eligiblePromo(request.promoCode()) ? subtotal.multiply(new BigDecimal("0.10")) : BigDecimal.ZERO;
        BigDecimal pointsDiscount = BigDecimal.valueOf(Math.min(request.frequentFlyerPoints() / 100, 100));
        BigDecimal total = subtotal.subtract(promoDiscount).subtract(pointsDiscount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new PriceQuote(request.flightId(), request.cabin(), request.baseFare(), multiplier.setScale(2, RoundingMode.HALF_UP),
                promoDiscount.setScale(2, RoundingMode.HALF_UP), pointsDiscount.setScale(2, RoundingMode.HALF_UP), total,
                List.of("Published cabin fare", "Advance-purchase adjustment", "Seat-demand adjustment", "Eligible discounts"));
    }
}
