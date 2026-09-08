package com.airline.notificationservice.service;

import com.airline.notificationservice.Notification;
import com.airline.notificationservice.NotificationRepository;
import com.airline.notificationservice.client.EmailClient;
import com.airline.notificationservice.dto.PaymentConfirmationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentEmailService {

    private static final Logger log = LoggerFactory.getLogger(PaymentEmailService.class);

    private final EmailClient emailClient;
    private final EmailTemplateEngine templateEngine;
    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    public PaymentEmailService(
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
    public void sendPaymentConfirmation(PaymentConfirmationRequest request) {
        Notification notification = new Notification();
        notification.setEventType("payment.succeeded");
        notification.setRecipient(request.recipientEmail());
        notification.setCreatedAt(LocalDateTime.now());

        try {
            notification.setPayload(objectMapper.writeValueAsString(request));

            Map<String, Object> variables = new HashMap<>();
            variables.put("bookingId", request.bookingId());
            variables.put("paymentId", request.paymentId());
            variables.put("providerReference", request.providerReference());
            variables.put("amount", request.amount());
            variables.put("paymentMethod", request.paymentMethod());

            String htmlBody = templateEngine.render("payment-confirmation", variables);
            String subject = "Your Payment Confirmation - Booking #" + request.bookingId();

            emailClient.sendEmail(request.recipientEmail(), subject, htmlBody);
            notification.setDeliveryStatus("SENT");
        } catch (Exception ex) {
            log.error("Failed to send payment confirmation email to {}", request.recipientEmail(), ex);
            notification.setDeliveryStatus("FAILED");
            notification.setDeliveryError(ex.getMessage());
        }

        repository.save(notification);
    }
}
