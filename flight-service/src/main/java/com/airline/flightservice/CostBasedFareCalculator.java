package com.airline.flightservice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Produces a transparent estimate from route distance, aircraft specifications and configurable
 * assumptions. The defaults are illustrative only and must be calibrated before real-world use.
 */
@Service
public class CostBasedFareCalculator {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private final AirportRepository airports;
    private final AircraftRepository aircraft;

    @Value("${pricing.estimate.fuel-price-per-litre:950}")
    private BigDecimal fuelPricePerLitre;
    @Value("${pricing.estimate.route-distance-factor:1.12}")
    private BigDecimal routeDistanceFactor;
    @Value("${pricing.estimate.taxi-and-overhead-hours:0.50}")
    private BigDecimal taxiAndOverheadHours;
    @Value("${pricing.estimate.crew-cost-per-flight:120000}")
    private BigDecimal crewCostPerFlight;
    @Value("${pricing.estimate.load-factor:0.75}")
    private BigDecimal expectedLoadFactor;
    @Value("${pricing.estimate.target-profit-margin:0.10}")
    private BigDecimal targetProfitMargin;
    @Value("${pricing.estimate.other-revenue-per-flight:0}")
    private BigDecimal otherRevenuePerFlight;
    @Value("${pricing.estimate.business-fare-multiplier:3.0}")
    private BigDecimal businessFareMultiplier;

    public CostBasedFareCalculator(AirportRepository airports, AircraftRepository aircraft) {
        this.airports = airports;
        this.aircraft = aircraft;
    }

    public FareEstimate estimate(String originCode, String destinationCode, String aircraftCode) {
        if (originCode == null || destinationCode == null || originCode.equalsIgnoreCase(destinationCode)) {
            throw new IllegalArgumentException("Origin and destination must be different airport codes.");
        }
        Airport origin = airports.findByCodeIgnoreCase(originCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Origin airport was not found: " + originCode));
        Airport destination = airports.findByCodeIgnoreCase(destinationCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Destination airport was not found: " + destinationCode));
        Aircraft plane = aircraft.findByCodeIgnoreCase(aircraftCode == null ? "" : aircraftCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Aircraft was not found: " + aircraftCode));

        requireCoordinates(origin);
        requireCoordinates(destination);
        requirePositive(plane.getSeatCapacity() > 0, "Aircraft seat capacity must be positive.");
        requirePositive(plane.getFuelConsumptionLitresPerHour(), "Aircraft fuel consumption (litres/hour) must be configured.");
        requirePositive(plane.getCruiseSpeedKmh(), "Aircraft cruise speed (km/hour) must be configured.");
        requireNonNegative(plane.getOperatingCostPerHour(), "Aircraft operating cost per hour must be configured.");
        requireNonNegative(plane.getMaintenanceCostPerHour(), "Aircraft maintenance cost per hour must be configured.");
        requirePositive(fuelPricePerLitre, "Fuel price per litre must be positive.");
        requirePositive(expectedLoadFactor != null && expectedLoadFactor.compareTo(BigDecimal.ZERO) > 0
                && expectedLoadFactor.compareTo(ONE) <= 0, "Expected load factor must be greater than 0 and at most 1.");
        requirePositive(targetProfitMargin != null && targetProfitMargin.compareTo(BigDecimal.ZERO) >= 0
                && targetProfitMargin.compareTo(new BigDecimal("0.80")) < 0,
                "Target profit margin must be between 0% and 80%.");
        requirePositive(routeDistanceFactor != null && routeDistanceFactor.compareTo(ONE) >= 0,
                "Route distance factor must be at least 1.");
        requireNonNegative(taxiAndOverheadHours, "Taxi and overhead hours cannot be negative.");
        requireNonNegative(crewCostPerFlight, "Crew cost cannot be negative.");
        requirePositive(businessFareMultiplier, "Business fare multiplier must be positive.");
        requireNonNegative(origin.getEstimatedAirportCharge(), "Origin airport charge cannot be negative.");
        requireNonNegative(destination.getEstimatedAirportCharge(), "Destination airport charge cannot be negative.");

        double straightLineKm = haversineKm(origin.getLatitude(), origin.getLongitude(),
                destination.getLatitude(), destination.getLongitude());
        BigDecimal routeKm = BigDecimal.valueOf(straightLineKm).multiply(routeDistanceFactor);
        BigDecimal flightHours = routeKm.divide(plane.getCruiseSpeedKmh(), 4, RoundingMode.HALF_UP)
                .add(taxiAndOverheadHours);

        BigDecimal fuelLitres = flightHours.multiply(plane.getFuelConsumptionLitresPerHour());
        BigDecimal fuelCost = money(fuelLitres.multiply(fuelPricePerLitre));
        BigDecimal operatingCost = money(flightHours.multiply(plane.getOperatingCostPerHour()));
        BigDecimal maintenanceCost = money(flightHours.multiply(plane.getMaintenanceCostPerHour()));
        BigDecimal airportCharges = money(origin.getEstimatedAirportCharge().add(destination.getEstimatedAirportCharge()));
        BigDecimal crewCost = money(crewCostPerFlight);
        BigDecimal totalCost = money(fuelCost.add(operatingCost).add(maintenanceCost).add(airportCharges).add(crewCost));

        BigDecimal otherRevenue = money(otherRevenuePerFlight == null ? BigDecimal.ZERO : otherRevenuePerFlight);
        requireNonNegative(otherRevenue, "Other revenue cannot be negative.");
        // Target margin is measured against total revenue (ticket revenue + ancillary revenue).
        BigDecimal requiredTotalRevenue = totalCost.divide(ONE.subtract(targetProfitMargin), 2, RoundingMode.HALF_UP);
        BigDecimal requiredTicketRevenue = requiredTotalRevenue.subtract(otherRevenue);
        if (requiredTicketRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Other revenue must be lower than the ticket revenue required by this cost model.");
        }
        BigDecimal expectedPassengerCount = BigDecimal.valueOf(plane.getSeatCapacity()).multiply(expectedLoadFactor);
        int expectedPassengers = expectedPassengerCount.setScale(0, RoundingMode.HALF_UP).intValue();
        if (expectedPassengers < 1) throw new IllegalArgumentException("Expected passenger count must be at least one.");

        BigDecimal fare = requiredTicketRevenue.divide(BigDecimal.valueOf(expectedPassengers), 2, RoundingMode.HALF_UP)
                .divide(HUNDRED, 0, RoundingMode.CEILING).multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
        BigDecimal businessFare = fare.multiply(businessFareMultiplier).setScale(2, RoundingMode.HALF_UP);

        return new FareEstimate(origin.getCode(), destination.getCode(), plane.getCode(),
                round(routeKm.doubleValue(), 2), flightHours.setScale(2, RoundingMode.HALF_UP),
                plane.getSeatCapacity(), expectedLoadFactor, expectedPassengers, fuelCost, operatingCost,
                maintenanceCost, crewCost, airportCharges, totalCost, otherRevenue, targetProfitMargin,
                fare, businessFare, "NGN",
                "Estimated from configurable assumptions; not a live airline operating-cost quote.");
    }

    private static void requireCoordinates(Airport airport) {
        if (airport.getLatitude() == null || airport.getLongitude() == null
                || airport.getLatitude() < -90 || airport.getLatitude() > 90
                || airport.getLongitude() < -180 || airport.getLongitude() > 180) {
            throw new IllegalArgumentException("Valid latitude and longitude are required for airport " + airport.getCode() + ".");
        }
    }

    private static void requirePositive(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException(message);
    }
    private static void requireNonNegative(BigDecimal value, String message) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException(message);
    }
    private static void requirePositive(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
    private static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
    private static double round(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
