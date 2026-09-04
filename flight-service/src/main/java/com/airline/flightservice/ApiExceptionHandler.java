package com.airline.flightservice;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    ResponseEntity<ApiError> runtime(RuntimeException ex, HttpServletRequest r) {
        HttpStatus status =
                ex instanceof FlightController.FlightNotFoundException
                        ? HttpStatus.NOT_FOUND
                        : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(
                        new ApiError(
                                Instant.now(),
                                status.value(),
                                status.getReasonPhrase(),
                                ex.getMessage(),
                                r.getRequestURI()));
    }

    record ApiError(Instant timestamp, int status, String error, String message, String path) {}
}
