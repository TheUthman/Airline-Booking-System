package com.airline.passengerservice;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException ex, HttpServletRequest r) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(
                        new ApiError(
                                Instant.now(),
                                ex.getStatusCode().value(),
                                ex.getStatusCode().toString(),
                                ex.getReason(),
                                r.getRequestURI()));
    }

    record ApiError(Instant timestamp, int status, String error, String message, String path) {}
}
