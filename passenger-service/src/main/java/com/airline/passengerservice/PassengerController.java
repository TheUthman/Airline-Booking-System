package com.airline.passengerservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/passengers")
public class PassengerController {
    private final PassengerRepository repository;

    PassengerController(PassengerRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/me")
    List<Passenger> mine(@RequestHeader("X-User-Email") String email) {
        return repository.findByOwnerEmail(email);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Passenger create(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody PassengerRequest request) {
        Passenger p = new Passenger();
        copy(p, request);
        p.setOwnerEmail(email);
        return repository.save(p);
    }

    @PutMapping("/{id}")
    Passenger update(
            @PathVariable Long id,
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody PassengerRequest request) {
        Passenger p =
                repository
                        .findByIdAndOwnerEmail(id, email)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Passenger not found"));
        copy(p, request);
        return repository.save(p);
    }

    private void copy(Passenger p, PassengerRequest r) {
        p.setFirstName(r.firstName());
        p.setLastName(r.lastName());
        p.setDateOfBirth(r.dateOfBirth());
        p.setPhone(r.phone());
        p.setDocumentNumber(r.documentNumber());
    }

    record PassengerRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotNull @Past LocalDate dateOfBirth,
            String phone,
            String documentNumber) {}
}
