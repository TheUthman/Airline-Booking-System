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

    FlightController(FlightRepository flights) {
        this.flights = flights;
    }

    @GetMapping("/search")
    public List<Flight> search(
            @RequestParam @Pattern(regexp = "[A-Za-z]{3}") String origin,
            @RequestParam @Pattern(regexp = "[A-Za-z]{3}") String destination,
            @RequestParam LocalDate date,
            @RequestParam(defaultValue = "1") @Min(1) int passengers) {
        return flights
                .findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetweenAndActiveTrue(
                        origin, destination, date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .filter(f -> f.getAvailableSeats() >= passengers)
                .toList();
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

    private void apply(Flight f, FlightRequest r) {
        f.setFlightNumber(r.flightNumber());
        f.setOrigin(r.origin().toUpperCase());
        f.setDestination(r.destination().toUpperCase());
        f.setDepartureTime(r.departureTime());
        f.setArrivalTime(r.arrivalTime());
        f.setFare(r.fare());
        f.setAvailableSeats(r.availableSeats());
    }

    record FlightRequest(
            @NotBlank String flightNumber,
            @Pattern(regexp = "[A-Za-z]{3}") String origin,
            @Pattern(regexp = "[A-Za-z]{3}") String destination,
            @NotNull LocalDateTime departureTime,
            @NotNull LocalDateTime arrivalTime,
            @NotNull @PositiveOrZero BigDecimal fare,
            @Min(0) int availableSeats) {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class FlightNotFoundException extends RuntimeException {
        FlightNotFoundException(Long id) {
            super("Flight " + id + " was not found");
        }
    }
}
