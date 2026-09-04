package com.airline.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/fallback/flight-service")
    public ResponseEntity<Map<String, Object>> flightServiceFallback() {

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        Map.of(
                                "error", "Flight Service Unavailable",
                                "message",
                                        "Flight services are temporarily unavailable. Please try"
                                            + " again later."));
    }
}
