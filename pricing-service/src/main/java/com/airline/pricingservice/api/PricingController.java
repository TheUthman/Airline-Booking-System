package com.airline.pricingservice.api;

import com.airline.pricingservice.api.dto.PriceQuote;
import com.airline.pricingservice.api.dto.PriceRequest;
import com.airline.pricingservice.application.PricingQuoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** Stateless quote calculator. Flight Service remains the owner of its published base fare. */
@RestController
@RequestMapping("/api/pricing")
public class PricingController {
    private final PricingQuoteService pricingQuoteService;
    public PricingController(PricingQuoteService pricingQuoteService) { this.pricingQuoteService = pricingQuoteService; }
    @PostMapping("/quote")
    PriceQuote quote(@Valid @RequestBody PriceRequest request) {
        return pricingQuoteService.quote(request);
    }
}
