package com.airline.flightservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {
    private final FlightRepository flights;
    private final AirportRepository airports;

    FlightController(FlightRepository flights, AirportRepository airports) {
        this.flights = flights;
        this.airports = airports;
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
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer maxDurationMinutes) {
        // All filters are pushed into the DB query — no in-memory stream filtering
        return flights.search(
                origin, destination,
                date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                passengers, cabin,
                (airline == null || airline.isBlank()) ? null : airline,
                maxPrice,
                maxDurationMinutes);
    }

    @GetMapping("/{id}")
    public Flight one(@PathVariable Long id) {
        return flights.findById(id).orElseThrow(() -> new FlightNotFoundException(id));
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
        f.setFare(r.fare());
        f.setBusinessFare(r.businessFare());
        f.setAvailableSeats(r.availableSeats());
        f.setTotalSeats(r.totalSeats());
        f.setAirline(r.airline());
        f.setAircraftCode(r.aircraftCode());
    }

    record FlightRequest(
            @NotBlank String flightNumber,
            @Pattern(regexp = "[A-Za-z]{3}") String origin,
            @Pattern(regexp = "[A-Za-z]{3}") String destination,
            @NotNull LocalDateTime departureTime,
            @NotNull LocalDateTime arrivalTime,
            @NotNull @PositiveOrZero BigDecimal fare,
            @PositiveOrZero BigDecimal businessFare,
            @Min(0) int availableSeats,
            @Min(1) int totalSeats,
            @NotBlank String airline,
            String aircraftCode) {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class FlightNotFoundException extends RuntimeException {
        FlightNotFoundException(Long id) {
            super("Flight " + id + " was not found");
        }
    }
}
