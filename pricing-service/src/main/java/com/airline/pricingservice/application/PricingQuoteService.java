package com.airline.pricingservice.application;

import com.airline.pricingservice.api.dto.PriceQuote;
import com.airline.pricingservice.api.dto.PriceRequest;
import com.airline.pricingservice.domain.PricingPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PricingQuoteService {
    private final PricingPolicy pricingPolicy;
    private final RestClient flightServiceClient;

    public PricingQuoteService(PricingPolicy pricingPolicy,
            @Value("${app.flight-service-url:http://localhost:8083}") String flightServiceUrl) {
        this.pricingPolicy = pricingPolicy;
        this.flightServiceClient = RestClient.builder().baseUrl(flightServiceUrl).build();
    }

    public PriceQuote quote(PriceRequest request) {
        FlightPricingData flight;
        try {
            flight = flightServiceClient.get()
                    .uri("/api/flights/{id}", request.flightId())
                    .retrieve()
                    .body(FlightPricingData.class);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not retrieve authoritative flight pricing data. Please retry.", ex);
        }
        if (flight == null || flight.id() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight was not found.");
        }
        if (!flight.active()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This flight is not available for booking.");
        }
        String cabin = request.cabin().toUpperCase();
        BigDecimal authoritativeFare = "BUSINESS".equals(cabin) ? flight.businessFare() : flight.fare();
        if (authoritativeFare == null || authoritativeFare.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A valid fare is not configured for this cabin.");
        }
        if (flight.totalSeats() < 1 || flight.availableSeats() < 0
                || flight.availableSeats() > flight.totalSeats()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Flight seat inventory is invalid.");
        }
        PriceRequest trustedRequest = new PriceRequest(
                flight.id(), authoritativeFare, flight.departureTime().toLocalDate(),
                flight.availableSeats(), flight.totalSeats(), cabin,
                request.promoCode(), request.frequentFlyerPoints());

        BigDecimal multiplier = pricingPolicy.multiplier(trustedRequest);
        BigDecimal subtotal = authoritativeFare.multiply(multiplier);
        BigDecimal promoDiscount = pricingPolicy.eligiblePromo(trustedRequest.promoCode())
                ? subtotal.multiply(new BigDecimal("0.10")) : BigDecimal.ZERO;
        BigDecimal pointsDiscount = BigDecimal.valueOf(Math.min(trustedRequest.frequentFlyerPoints() / 100, 100));
        BigDecimal total = subtotal.subtract(promoDiscount).subtract(pointsDiscount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new PriceQuote(flight.id(), cabin, authoritativeFare,
                multiplier.setScale(2, RoundingMode.HALF_UP),
                promoDiscount.setScale(2, RoundingMode.HALF_UP),
                pointsDiscount.setScale(2, RoundingMode.HALF_UP), total,
                List.of("Cost-based published cabin fare", "Advance-purchase adjustment",
                        "Seat-demand adjustment", "Eligible discounts"));
    }
}
