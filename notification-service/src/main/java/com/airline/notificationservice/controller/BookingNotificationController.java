package com.airline.notificationservice.controller;

import com.airline.notificationservice.dto.BookingConfirmationRequest;
import com.airline.notificationservice.service.BookingEmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications/booking")
public class BookingNotificationController {

    private final BookingEmailService bookingEmailService;

    public BookingNotificationController(BookingEmailService bookingEmailService) {
        this.bookingEmailService = bookingEmailService;
    }

    @PostMapping("/confirmation")
    public ResponseEntity<Map<String, String>> sendConfirmation(@Valid @RequestBody BookingConfirmationRequest request) {
        bookingEmailService.sendBookingConfirmation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Booking confirmation email queued for dispatch"));
    }
}
