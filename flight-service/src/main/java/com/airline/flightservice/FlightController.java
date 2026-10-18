package com.airline.flightservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flights")
public class FlightController {
    private final FlightRepository flights;
    private final AirportRepository airports;
    private final CostBasedFareCalculator fareCalculator;
    private final AircraftRepository aircraft;

    FlightController(
            FlightRepository flights,
            AirportRepository airports,
            CostBasedFareCalculator fareCalculator,
            AircraftRepository aircraft) {
        this.flights = flights;
        this.airports = airports;
        this.fareCalculator = fareCalculator;
        this.aircraft = aircraft;
    }

    @GetMapping
    public List<Flight> all() {
        return flights.findAll();
    }

    @GetMapping("/airports")
    public List<Airport> publicAirports() {
        return airports.findAll();
    }

    @GetMapping("/search")
    public List<Flight> search(
            @RequestParam @Pattern(regexp = "[A-Za-z]{3}") String origin,
            @RequestParam @Pattern(regexp = "[A-Za-z]{3}") String destination,
            @RequestParam LocalDate date,
            @RequestParam(defaultValue = "1") @Min(1) int passengers,
            @RequestParam(defaultValue = "ECONOMY")
                    @Pattern(regexp = "(?i)ECONOMY|BUSINESS") String cabin,
            @RequestParam(required = false) String airline,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal maxPrice,
            @RequestParam(required = false) @Min(0) Integer maxDurationMinutes) {
        // All filters are pushed into the DB query — no in-memory stream filtering
        return flights.search(
                origin, destination,
                date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                passengers, cabin,
                (airline == null || airline.isBlank()) ? "" : airline,
                maxPrice == null ? BigDecimal.valueOf(-1) : maxPrice,
                maxDurationMinutes == null ? -1 : maxDurationMinutes);
    }

    @GetMapping("/{id}")
    public Flight one(@PathVariable Long id) {
        return flights.findById(id).orElseThrow(() -> new FlightNotFoundException(id));
    }

    @PostMapping("/admin/fare-estimate")
    public FareEstimate estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        return fareCalculator.estimate(request.origin(), request.destination(), request.aircraftCode());
    }

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    public Flight create(@Valid @RequestBody FlightRequest request) {
        Flight f = new Flight();
        apply(f, request);
        return flights.save(f);
    }

    @PutMapping("/admin/{id}")
    public Flight update(@PathVariable Long id, @Valid @RequestBody FlightRequest request) {
        Flight f = one(id);
        apply(f, request);
        return flights.save(f);
    }

    @PostMapping("/admin/{id}/delay")
    public Flight delay(@PathVariable Long id, @RequestParam @Min(1) int minutes) {
        Flight f = one(id);
        f.setDelayMinutes((f.getDelayMinutes() == null ? 0 : f.getDelayMinutes()) + minutes);
        f.setDepartureTime(f.getDepartureTime().plusMinutes(minutes));
        f.setArrivalTime(f.getArrivalTime().plusMinutes(minutes));
        f.setStatus(FlightStatus.DELAYED);
        return flights.save(f);
    }

    @PostMapping("/admin/{id}/cancel")
    public Flight cancel(@PathVariable Long id) {
        Flight f = one(id);
        f.setActive(false);
        f.setStatus(FlightStatus.CANCELLED);
        return flights.save(f);
    }

    private void apply(Flight f, FlightRequest r) {
        f.setFlightNumber(r.flightNumber());
        f.setOrigin(r.origin().toUpperCase());
        f.setDestination(r.destination().toUpperCase());
        f.setDepartureTime(r.departureTime());
        f.setArrivalTime(r.arrivalTime());
        // If no fare is supplied, derive both cabin fares from the cost model.
        // Admin clients should omit fare/businessFare to use automatic pricing.
        if (!r.manualFareOverride()) {
            FareEstimate estimate = fareCalculator.estimate(r.origin(), r.destination(), r.aircraftCode());
            f.setFare(estimate.suggestedEconomyBaseFare());
            f.setBusinessFare(estimate.suggestedBusinessBaseFare());
        } else {
            if (r.fare() == null) {
                throw new IllegalArgumentException("fare is required when manualFareOverride is true.");
            }
            f.setFare(r.fare());
            f.setBusinessFare(r.businessFare() == null
                    ? r.fare().multiply(new BigDecimal("3.0")).setScale(2, java.math.RoundingMode.HALF_UP)
                    : r.businessFare());
        }
        Aircraft selectedAircraft = aircraft.findByCodeIgnoreCase(r.aircraftCode().trim())
                .orElseThrow(() -> new IllegalArgumentException("Aircraft was not found: " + r.aircraftCode()));
        if (r.availableSeats() > selectedAircraft.getSeatCapacity()) {
            throw new IllegalArgumentException("Available seats cannot exceed the selected aircraft capacity.");
        }
        f.setAvailableSeats(r.availableSeats());
        // Aircraft catalogue is the source of truth for total seat capacity.
        f.setTotalSeats(selectedAircraft.getSeatCapacity());
        f.setAirline(r.airline());
        f.setAircraftCode(selectedAircraft.getCode());
    }

    record FlightRequest(
            @NotBlank String flightNumber,
            @Pattern(regexp = "[A-Za-z]{3}") String origin,
            @Pattern(regexp = "[A-Za-z]{3}") String destination,
            @NotNull LocalDateTime departureTime,
            @NotNull LocalDateTime arrivalTime,
            @PositiveOrZero BigDecimal fare,
            @PositiveOrZero BigDecimal businessFare,
            @Min(0) int availableSeats,
            @Min(1) int totalSeats,
            @NotBlank String airline,
            @NotBlank String aircraftCode,
            boolean manualFareOverride) {}

    record FareEstimateRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String origin,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String destination,
            @NotBlank String aircraftCode) {}

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class FlightNotFoundException extends RuntimeException {
        FlightNotFoundException(Long id) {
            super("Flight " + id + " was not found");
        }
    }
}
