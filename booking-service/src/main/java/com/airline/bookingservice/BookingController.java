package com.airline.bookingservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingRepository repository;
    private final StringRedisTemplate redis;

    BookingController(BookingRepository repository, StringRedisTemplate redis) {
        this.repository = repository;
        this.redis = redis;
    }

    @GetMapping
    List<Booking> mine(@RequestHeader("X-User-Email") String email) {
        return repository.findByOwnerEmailOrderByCreatedAtDesc(email);
    }

    @GetMapping("/{id}")
    Booking one(@PathVariable Long id, @RequestHeader("X-User-Email") String email) {
        return repository
                .findByIdAndOwnerEmail(id, email)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Booking not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Booking create(
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody BookingRequest request) {
        String key = "seat-lock:" + request.flightId() + ":" + request.seatNumber().toUpperCase();
        boolean acquired;
        try {
            acquired =
                    Boolean.TRUE.equals(
                            redis.opsForValue().setIfAbsent(key, email, Duration.ofMinutes(10)));
        } catch (DataAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Seat locking is temporarily unavailable");
        }
        if (!acquired)
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This seat is currently being selected by another traveler");
        Booking b = new Booking();
        b.setPnr(UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
        b.setOwnerEmail(email);
        b.setFlightId(request.flightId());
        b.setPassengerId(request.passengerId());
        b.setSeatNumber(request.seatNumber().toUpperCase());
        b.setAmount(request.amount());
        b.setStatus(BookingStatus.PENDING_PAYMENT);
        b.setCreatedAt(LocalDateTime.now());
        return repository.save(b);
    }

    @PostMapping("/{id}/cancel")
    Booking cancel(@PathVariable Long id, @RequestHeader("X-User-Email") String email) {
        Booking b = one(id, email);
        if (b.getStatus() == BookingStatus.CONFIRMED)
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Confirmed bookings must be cancelled through support");
        b.setStatus(BookingStatus.CANCELLED);
        redis.delete("seat-lock:" + b.getFlightId() + ":" + b.getSeatNumber());
        return repository.save(b);
    }

    record BookingRequest(
            @NotNull @Positive Long flightId,
            @NotNull @Positive Long passengerId,
            @NotBlank @Pattern(regexp = "[0-9]{1,3}[A-Za-z]") String seatNumber,
            @NotNull @Positive BigDecimal amount) {}
}
