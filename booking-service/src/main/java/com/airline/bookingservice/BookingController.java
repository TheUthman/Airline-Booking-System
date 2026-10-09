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
    List<Booking> mine(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return repository.findAll();
        }
        return repository.findByOwnerEmailOrderByCreatedAtDesc(email);
    }

    @GetMapping("/pnr/{pnr}")
    Booking byPnr(@PathVariable String pnr) {
        return repository
                .findByPnrIgnoreCase(pnr.trim())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    @GetMapping("/staff/lookup")
    StaffBookingView staffLookup(
            @RequestParam @NotBlank String pnr,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireStaffOrAdmin(role);

        Booking booking = repository
                .findByPnrIgnoreCase(pnr.trim())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        return toStaffBookingView(booking);
    }

    @GetMapping("/staff/flights/{flightId}/manifest")
    List<StaffManifestBookingView> staffFlightManifest(
            @PathVariable @Positive Long flightId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireStaffOrAdmin(role);
        return repository.findByFlightIdOrderByCreatedAtAsc(flightId).stream()
                .map(booking -> new StaffManifestBookingView(
                        booking.getId(),
                        booking.getPnr(),
                        booking.getPassengerId(),
                        booking.getSeatNumber(),
                        booking.getCabinClass(),
                        booking.getStatus(),
                        booking.getCheckedInAt()))
                .toList();
    }

    @PostMapping("/{id}/check-in")
    StaffBookingView staffCheckIn(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireStaffOrAdmin(role);

        Booking booking = repository
                .findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Only confirmed bookings can be checked in");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(LocalDateTime.now());
        return toStaffBookingView(repository.save(booking));
    }

    // PNR lookup — optionally cross-checks lastName against the booking owner email as a guard
    @GetMapping("/search")
    Booking search(@RequestParam String pnr, @RequestParam(required = false) String lastName) {
        Booking booking = byPnr(pnr);
        if (lastName != null && !lastName.isBlank()
                && !booking.getOwnerEmail().toLowerCase().contains(lastName.toLowerCase())) {
            // Don't reveal that the PNR exists for a different passenger
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        return booking;
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
        String cabinClass = normalizeCabinClass(request.cabinClass());
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
        b.setCabinClass(cabinClass);
        b.setAmount(request.amount());
        b.setStatus(BookingStatus.PENDING_PAYMENT);
        // createdAt set automatically by @PrePersist
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
        List<String> acquiredLocks = new ArrayList<>();
        List<Booking> result = new ArrayList<>();
        try {
            for (TravelerSeat traveler : request.travelers()) {
                String cabinClass = normalizeCabinClass(traveler.cabinClass());
                String key = "seat-lock:" + request.flightId() + ":" + traveler.seatNumber().toUpperCase();
                if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, email, Duration.ofMinutes(10)))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "A requested seat is currently unavailable");
                }
                acquiredLocks.add(key);
                Booking booking = new Booking();
                booking.setPnr(UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase());
                booking.setOwnerEmail(email);
                booking.setFlightId(request.flightId());
                booking.setPassengerId(traveler.passengerId());
                booking.setSeatNumber(traveler.seatNumber().toUpperCase());
                booking.setCabinClass(cabinClass);
                booking.setAmount(traveler.amount());
                booking.setStatus(BookingStatus.PENDING_PAYMENT);
                result.add(repository.save(booking));
            }
        } catch (ResponseStatusException ex) {
            // Release all locks acquired so far before propagating the conflict
            acquiredLocks.forEach(redis::delete);
            throw ex;
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
        if (b.getStatus() == BookingStatus.CONFIRMED
                || b.getStatus() == BookingStatus.CHECKED_IN)
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Confirmed or checked-in bookings must be cancelled through support");
        b.setStatus(BookingStatus.CANCELLED);
        redis.delete("seat-lock:" + b.getFlightId() + ":" + b.getSeatNumber());
        return repository.save(b);
    }

    record BookingRequest(
            @NotNull @Positive Long flightId,
            @NotNull @Positive Long passengerId,
            @NotBlank @Pattern(regexp = "[0-9]{1,3}[A-Za-z]") String seatNumber,
            @NotNull @Positive BigDecimal amount,
            @Pattern(regexp = "(?i)ECONOMY|BUSINESS") String cabinClass) {
        BookingRequest(Long flightId, Long passengerId, String seatNumber, BigDecimal amount) {
            this(flightId, passengerId, seatNumber, amount, "ECONOMY");
        }
    }

    record TravelerSeat(@NotNull @Positive Long passengerId,
            @NotBlank @Pattern(regexp = "[0-9]{1,3}[A-Za-z]") String seatNumber,
            @NotNull @Positive BigDecimal amount,
            @Pattern(regexp = "(?i)ECONOMY|BUSINESS") String cabinClass) {
        TravelerSeat(Long passengerId, String seatNumber, BigDecimal amount) {
            this(passengerId, seatNumber, amount, "ECONOMY");
        }
    }
    record GroupBookingRequest(@NotNull @Positive Long flightId, @NotEmpty List<@Valid TravelerSeat> travelers) {}

    public record StaffBookingView(
            Long id,
            String pnr,
            Long flightId,
            String seatNumber,
            String cabinClass,
            BigDecimal amount,
            BookingStatus status,
            LocalDateTime createdAt,
            LocalDateTime checkedInAt) {}

    public record StaffManifestBookingView(
            Long id,
            String pnr,
            Long passengerId,
            String seatNumber,
            String cabinClass,
            BookingStatus status,
            LocalDateTime checkedInAt) {}

    private static void requireStaffOrAdmin(String role) {
        if (!"STAFF".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff access is required");
        }
    }

    private static StaffBookingView toStaffBookingView(Booking booking) {
        return new StaffBookingView(
                booking.getId(),
                booking.getPnr(),
                booking.getFlightId(),
                booking.getSeatNumber(),
                booking.getCabinClass(),
                booking.getAmount(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getCheckedInAt());
    }

    private String normalizeCabinClass(String cabinClass) {
        String normalized = cabinClass == null || cabinClass.isBlank()
                ? "ECONOMY"
                : cabinClass.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ECONOMY") && !normalized.equals("BUSINESS")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cabin class must be ECONOMY or BUSINESS");
        }
        return normalized;
    }
}
