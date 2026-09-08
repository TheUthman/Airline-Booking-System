package com.airline.notificationservice.controller;

import com.airline.notificationservice.dto.FlightCancellationRequest;
import com.airline.notificationservice.dto.FlightUpdateRequest;
import com.airline.notificationservice.service.FlightEmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications/flight")
public class FlightNotificationController {

    private final FlightEmailService flightEmailService;

    public FlightNotificationController(FlightEmailService flightEmailService) {
        this.flightEmailService = flightEmailService;
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, String>> sendUpdate(@Valid @RequestBody FlightUpdateRequest request) {
        flightEmailService.sendFlightUpdate(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Flight update email queued for dispatch"));
    }

    @PostMapping("/cancellation")
    public ResponseEntity<Map<String, String>> sendCancellation(@Valid @RequestBody FlightCancellationRequest request) {
        flightEmailService.sendFlightCancellation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Flight cancellation email queued for dispatch"));
    }
}
