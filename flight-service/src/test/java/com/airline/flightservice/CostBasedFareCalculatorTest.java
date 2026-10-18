package com.airline.flightservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CostBasedFareCalculatorTest {
    private AirportRepository airports;
    private AircraftRepository aircraft;
    private CostBasedFareCalculator calculator;

    @BeforeEach
    void setUp() {
        airports = mock(AirportRepository.class);
        aircraft = mock(AircraftRepository.class);
        calculator = new CostBasedFareCalculator(airports, aircraft);
        ReflectionTestUtils.setField(calculator, "fuelPricePerLitre", new BigDecimal("950"));
        ReflectionTestUtils.setField(calculator, "routeDistanceFactor", new BigDecimal("1.12"));
        ReflectionTestUtils.setField(calculator, "taxiAndOverheadHours", new BigDecimal("0.50"));
        ReflectionTestUtils.setField(calculator, "crewCostPerFlight", new BigDecimal("120000"));
        ReflectionTestUtils.setField(calculator, "expectedLoadFactor", new BigDecimal("0.75"));
        ReflectionTestUtils.setField(calculator, "targetProfitMargin", new BigDecimal("0.10"));
        ReflectionTestUtils.setField(calculator, "otherRevenuePerFlight", BigDecimal.ZERO);
        ReflectionTestUtils.setField(calculator, "businessFareMultiplier", new BigDecimal("3.0"));
    }

    @Test
    void estimatesPositiveFareWhenSpecificationsExist() {
        Airport lagos = airport("LOS", 6.5774, 3.3212, "10000");
        Airport abuja = airport("ABV", 9.0068, 7.2631, "12000");
        Aircraft plane = new Aircraft();
        plane.setCode("TEST-100");
        plane.setModel("Test aircraft");
        plane.setSeatCapacity(100);
        plane.setFuelConsumptionLitresPerHour(new BigDecimal("900"));
        plane.setCruiseSpeedKmh(new BigDecimal("800"));
        plane.setOperatingCostPerHour(new BigDecimal("200000"));
        plane.setMaintenanceCostPerHour(new BigDecimal("50000"));
        when(airports.findByCodeIgnoreCase("LOS")).thenReturn(Optional.of(lagos));
        when(airports.findByCodeIgnoreCase("ABV")).thenReturn(Optional.of(abuja));
        when(aircraft.findByCodeIgnoreCase("TEST-100")).thenReturn(Optional.of(plane));

        FareEstimate result = calculator.estimate("LOS", "ABV", "TEST-100");

        assertTrue(result.routeDistanceKm() > 0);
        assertTrue(result.totalEstimatedOperatingCost().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.suggestedEconomyBaseFare().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(result.suggestedEconomyBaseFare().multiply(new BigDecimal("3.0")).setScale(2),
                result.suggestedBusinessBaseFare());
        assertEquals(75, result.expectedPassengers());
    }

    @Test
    void rejectsAirportWithoutCoordinates() {
        Airport missingCoordinates = airport("LOS", null, null, "0");
        Airport destination = airport("ABV", 9.0, 7.0, "0");
        when(airports.findByCodeIgnoreCase("LOS")).thenReturn(Optional.of(missingCoordinates));
        when(airports.findByCodeIgnoreCase("ABV")).thenReturn(Optional.of(destination));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> calculator.estimate("LOS", "ABV", "TEST-100"));
        assertTrue(error.getMessage().contains("latitude and longitude"));
    }

    private static Airport airport(String code, Double lat, Double lon, String charge) {
        Airport airport = new Airport();
        airport.setCode(code);
        airport.setName(code + " Airport");
        airport.setCity(code);
        airport.setCountry("Nigeria");
        airport.setLatitude(lat);
        airport.setLongitude(lon);
        airport.setEstimatedAirportCharge(new BigDecimal(charge));
        return airport;
    }
}
