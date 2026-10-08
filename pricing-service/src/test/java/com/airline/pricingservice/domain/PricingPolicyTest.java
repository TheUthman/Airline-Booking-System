package com.airline.pricingservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.airline.pricingservice.api.dto.PriceRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingPolicyTest {
    @Test
    void doesNotApplyACabinMultiplierToAdminManagedBaseFares() {
        PricingPolicy policy = new PricingPolicy();
        LocalDate departure = LocalDate.now().plusDays(30);

        BigDecimal economyMultiplier = policy.multiplier(
                new PriceRequest(1L, BigDecimal.valueOf(100), departure, 180, 180,
                        "ECONOMY", null, 0));
        BigDecimal businessMultiplier = policy.multiplier(
                new PriceRequest(1L, BigDecimal.valueOf(500), departure, 180, 180,
                        "BUSINESS", null, 0));

        assertThat(economyMultiplier).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(businessMultiplier).isEqualByComparingTo(BigDecimal.ONE);
    }
}
