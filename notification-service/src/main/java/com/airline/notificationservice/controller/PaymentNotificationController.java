package com.airline.notificationservice.controller;

import com.airline.notificationservice.dto.PaymentConfirmationRequest;
import com.airline.notificationservice.service.PaymentEmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications/payment")
public class PaymentNotificationController {

    private final PaymentEmailService paymentEmailService;

    public PaymentNotificationController(PaymentEmailService paymentEmailService) {
        this.paymentEmailService = paymentEmailService;
    }

    @PostMapping("/confirmation")
    public ResponseEntity<Map<String, String>> sendConfirmation(@Valid @RequestBody PaymentConfirmationRequest request) {
        paymentEmailService.sendPaymentConfirmation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", "Payment confirmation email queued for dispatch"));
    }
}
