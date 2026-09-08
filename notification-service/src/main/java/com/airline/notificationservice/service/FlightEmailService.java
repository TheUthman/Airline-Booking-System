package com.airline.notificationservice.service;

import com.airline.notificationservice.Notification;
import com.airline.notificationservice.NotificationRepository;
import com.airline.notificationservice.client.EmailClient;
import com.airline.notificationservice.dto.FlightCancellationRequest;
import com.airline.notificationservice.dto.FlightUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class FlightEmailService {

    private static final Logger log = LoggerFactory.getLogger(FlightEmailService.class);

    private final EmailClient emailClient;
    private final EmailTemplateEngine templateEngine;
    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    public FlightEmailService(
            EmailClient emailClient,
            EmailTemplateEngine templateEngine,
            NotificationRepository repository,
            ObjectMapper objectMapper) {
        this.emailClient = emailClient;
        this.templateEngine = templateEngine;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Async("emailExecutor")
    public void sendFlightUpdate(FlightUpdateRequest request) {
        Notification notification = new Notification();
        notification.setEventType("flight.updated");
        notification.setRecipient(request.recipientEmail());
        notification.setCreatedAt(LocalDateTime.now());

        try {
            notification.setPayload(objectMapper.writeValueAsString(request));

            Map<String, Object> variables = new HashMap<>();
            variables.put("flightNumber", request.flightNumber());
            variables.put("origin", request.origin());
            variables.put("destination", request.destination());
            variables.put("scheduledDeparture", request.scheduledDeparture());
            variables.put("newDeparture", request.newDeparture());
            variables.put("gate", request.gate());
            variables.put("statusMessage", request.statusMessage());

            String htmlBody = templateEngine.render("flight-update", variables);
            String subject = "Flight Update: " + request.flightNumber();

            emailClient.sendEmail(request.recipientEmail(), subject, htmlBody);
            notification.setDeliveryStatus("SENT");
        } catch (Exception ex) {
            log.error("Failed to send flight update email to {}", request.recipientEmail(), ex);
            notification.setDeliveryStatus("FAILED");
            notification.setDeliveryError(ex.getMessage());
        }

        repository.save(notification);
    }

    @Async("emailExecutor")
    public void sendFlightCancellation(FlightCancellationRequest request) {
        Notification notification = new Notification();
        notification.setEventType("flight.cancelled");
        notification.setRecipient(request.recipientEmail());
        notification.setCreatedAt(LocalDateTime.now());

        try {
            notification.setPayload(objectMapper.writeValueAsString(request));

            Map<String, Object> variables = new HashMap<>();
            variables.put("flightNumber", request.flightNumber());
            variables.put("origin", request.origin());
            variables.put("destination", request.destination());
            variables.put("departureDate", request.departureDate());
            variables.put("reason", request.reason());
            variables.put("refundPolicyUrl", request.refundPolicyUrl());

            String htmlBody = templateEngine.render("flight-cancellation", variables);
            String subject = "Urgent: Flight Cancellation Notice - " + request.flightNumber();

            emailClient.sendEmail(request.recipientEmail(), subject, htmlBody);
            notification.setDeliveryStatus("SENT");
        } catch (Exception ex) {
            log.error("Failed to send flight cancellation email to {}", request.recipientEmail(), ex);
            notification.setDeliveryStatus("FAILED");
            notification.setDeliveryError(ex.getMessage());
        }

        repository.save(notification);
    }
}
