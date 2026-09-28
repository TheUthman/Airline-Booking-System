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

    /** Creates one pending reservation per traveller so each seat has an independent lifecycle. */
    @PostMapping("/group")
    @ResponseStatus(HttpStatus.CREATED)
    List<Booking> createGroup(@RequestHeader("X-User-Email") String email,
            @Valid @RequestBody GroupBookingRequest request) {
        if (request.travelers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one traveller is required");
        }
        List<Booking> result = new ArrayList<>();
        for (TravelerSeat traveler : request.travelers()) {
            String key = "seat-lock:" + request.flightId() + ":" + traveler.seatNumber().toUpperCase();
            if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, email, Duration.ofMinutes(10)))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A requested seat is currently unavailable");
            }
            Booking booking = new Booking();
            booking.setPnr(UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
            booking.setOwnerEmail(email);
            booking.setFlightId(request.flightId());
            booking.setPassengerId(traveler.passengerId());
            booking.setSeatNumber(traveler.seatNumber().toUpperCase());
            booking.setAmount(traveler.amount());
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
            booking.setCreatedAt(LocalDateTime.now());
            result.add(repository.save(booking));
        }
        return result;
    }

    @PostMapping("/{id}/upgrade")
    Booking upgrade(@PathVariable Long id, @RequestHeader("X-User-Email") String email,
            @RequestParam @NotBlank String seatNumber, @RequestParam @Positive BigDecimal additionalAmount) {
        Booking booking = one(id, email);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending bookings can be upgraded");
        }
        String key = "seat-lock:" + booking.getFlightId() + ":" + seatNumber.toUpperCase();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, email, Duration.ofMinutes(10)))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Requested upgrade seat is unavailable");
        }
        redis.delete("seat-lock:" + booking.getFlightId() + ":" + booking.getSeatNumber());
        booking.setSeatNumber(seatNumber.toUpperCase());
        booking.setAmount(booking.getAmount().add(additionalAmount));
        return repository.save(booking);
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

    record TravelerSeat(@NotNull @Positive Long passengerId,
            @NotBlank @Pattern(regexp = "[0-9]{1,3}[A-Za-z]") String seatNumber,
            @NotNull @Positive BigDecimal amount) {}
    record GroupBookingRequest(@NotNull @Positive Long flightId, @NotEmpty List<@Valid TravelerSeat> travelers) {}
}
