package com.airline.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentRepository repository;

    private final RabbitTemplate rabbit;

    private final ObjectMapper mapper;

    private final String webhookSecret;

    PaymentController(
            PaymentRepository repository,
            RabbitTemplate rabbit,
            ObjectMapper mapper,
            @Value("${payment.webhook-secret}") String webhookSecret) {
        this.repository = repository;
        this.rabbit = rabbit;
        this.mapper = mapper;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/initiate")
    @ResponseStatus(HttpStatus.CREATED)
    Payment initiate(
            @RequestHeader("X-User-Email") String email, @Valid @RequestBody InitiateRequest r) {
        Payment p = new Payment();
        p.setBookingId(r.bookingId());
        p.setAmount(r.amount());
        p.setOwnerEmail(email);
        p.setProviderReference("pay_" + UUID.randomUUID().toString().replace("-", ""));
        p.setStatus(PaymentStatus.PENDING);
        p.setCreatedAt(LocalDateTime.now());
        return repository.save(p);
    }

    @PostMapping("/webhook")
    ResponseEntity<Void> webhook(
            @RequestHeader("X-Payment-Webhook-Secret") String secret,
            @Valid @RequestBody WebhookRequest r)
            throws Exception {
        if (!webhookSecret.equals(secret))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Payment p =
                repository
                        .findByProviderReference(r.providerReference())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Payment not found"));
        if (p.getStatus() != PaymentStatus.PENDING) return ResponseEntity.ok().build();
        p.setStatus(r.succeeded() ? PaymentStatus.SUCCEEDED : PaymentStatus.FAILED);
        repository.save(p);
        String type = r.succeeded() ? "payment.succeeded" : "payment.failed";
        rabbit.convertAndSend(
                "airline.events",
                type,
                mapper.writeValueAsString(
                        Map.of(
                                "type",
                                type,
                                "bookingId",
                                p.getBookingId(),
                                "paymentId",
                                p.getId(),
                                "recipientEmail",
                                p.getOwnerEmail())));
        return ResponseEntity.ok().build();
    }

    record InitiateRequest(
            @NotNull @Positive Long bookingId, @NotNull @Positive BigDecimal amount) {}

    record WebhookRequest(@NotBlank String providerReference, boolean succeeded) {}
}
