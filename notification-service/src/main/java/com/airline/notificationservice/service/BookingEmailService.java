package com.airline.notificationservice.service;

import com.airline.notificationservice.Notification;
import com.airline.notificationservice.NotificationRepository;
import com.airline.notificationservice.client.EmailClient;
import com.airline.notificationservice.dto.BookingConfirmationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class BookingEmailService {

    private static final Logger log = LoggerFactory.getLogger(BookingEmailService.class);

    private final EmailClient emailClient;
    private final EmailTemplateEngine templateEngine;
    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    public BookingEmailService(
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
    public void sendBookingConfirmation(BookingConfirmationRequest request) {
        Notification notification = new Notification();
        notification.setEventType("booking.confirmed");
        notification.setRecipient(request.recipientEmail());
        notification.setCreatedAt(LocalDateTime.now());

        try {
            notification.setPayload(objectMapper.writeValueAsString(request));

            Map<String, Object> variables = new HashMap<>();
            variables.put("bookingId", request.bookingId());
            variables.put("bookingReference", request.bookingReference());
            variables.put("flightNumber", request.flightNumber());
            variables.put("origin", request.origin());
            variables.put("destination", request.destination());
            variables.put("departureTime", request.departureTime());
            variables.put("arrivalTime", request.arrivalTime());
            variables.put("totalAmount", request.totalAmount());
            variables.put("passengerNames", request.passengerNames());

            String htmlBody = templateEngine.render("booking-confirmation", variables);
            String subject = "Your Booking is Confirmed - Ref: " + (request.bookingReference() != null ? request.bookingReference() : "#" + request.bookingId());

            emailClient.sendEmail(request.recipientEmail(), subject, htmlBody);
            notification.setDeliveryStatus("SENT");
        } catch (Exception ex) {
            log.error("Failed to send booking confirmation email to {}", request.recipientEmail(), ex);
            notification.setDeliveryStatus("FAILED");
            notification.setDeliveryError(ex.getMessage());
        }

        repository.save(notification);
    }
}
