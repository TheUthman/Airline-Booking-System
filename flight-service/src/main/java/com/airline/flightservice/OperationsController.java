package com.airline.flightservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** Admin-owned airport and aircraft catalogue endpoints. */
@RestController
@RequestMapping("/api/flights/admin")
class OperationsController {
    private final AirportRepository airports;
    private final AircraftRepository aircraft;
    OperationsController(AirportRepository airports, AircraftRepository aircraft) { this.airports = airports; this.aircraft = aircraft; }
    @GetMapping("/airports") List<Airport> airports() { return airports.findAll(); }
    @PostMapping("/airports") @ResponseStatus(HttpStatus.CREATED) Airport addAirport(@Valid @RequestBody AirportRequest r) { Airport a = new Airport(); copy(a, r); return airports.save(a); }
    @PutMapping("/airports/{id}") Airport updateAirport(@PathVariable Long id, @Valid @RequestBody AirportRequest r) { Airport a = airports.findById(id).orElseThrow(() -> new IllegalArgumentException("Airport not found")); copy(a,r); return airports.save(a); }
    @DeleteMapping("/airports/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteAirport(@PathVariable Long id) { airports.deleteById(id); }
    @GetMapping("/aircraft") List<Aircraft> aircraft() { return aircraft.findAll(); }
    @PostMapping("/aircraft") @ResponseStatus(HttpStatus.CREATED) Aircraft addAircraft(@Valid @RequestBody AircraftRequest r) { Aircraft a = new Aircraft(); copy(a,r); return aircraft.save(a); }
    @PutMapping("/aircraft/{id}") Aircraft updateAircraft(@PathVariable Long id, @Valid @RequestBody AircraftRequest r) { Aircraft a = aircraft.findById(id).orElseThrow(() -> new IllegalArgumentException("Aircraft not found")); copy(a,r); return aircraft.save(a); }
    @DeleteMapping("/aircraft/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) void deleteAircraft(@PathVariable Long id) { aircraft.deleteById(id); }
    private void copy(Airport a, AirportRequest r) { a.setCode(r.code().toUpperCase()); a.setName(r.name()); a.setCity(r.city()); a.setCountry(r.country()); }
    private void copy(Aircraft a, AircraftRequest r) { a.setCode(r.code()); a.setModel(r.model()); a.setSeatCapacity(r.seatCapacity()); }
    record AirportRequest(@Pattern(regexp="[A-Za-z]{3}") String code, @NotBlank String name, @NotBlank String city, @NotBlank String country) {}
    record AircraftRequest(@NotBlank String code, @NotBlank String model, @Min(1) int seatCapacity) {}
}
