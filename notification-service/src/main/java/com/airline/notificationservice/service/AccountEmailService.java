package com.airline.notificationservice.service;

import com.airline.notificationservice.Notification;
import com.airline.notificationservice.NotificationRepository;
import com.airline.notificationservice.client.EmailClient;
import com.airline.notificationservice.dto.AccountVerificationRequest;
import com.airline.notificationservice.dto.PasswordResetRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AccountEmailService {

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);

    private final EmailClient emailClient;
    private final EmailTemplateEngine templateEngine;
    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;

    public AccountEmailService(
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
    public void sendPasswordReset(PasswordResetRequest request) {
        Notification notification = new Notification();
        notification.setEventType("account.password_reset");
        notification.setRecipient(request.recipientEmail());
        notification.setCreatedAt(LocalDateTime.now());

        try {
            notification.setPayload(objectMapper.writeValueAsString(request));

            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", request.userName());
            variables.put("resetUrl", request.resetUrl());
            variables.put("expireInMinutes", request.expireInMinutes());

            String htmlBody = templateEngine.render("password-reset", variables);
            String subject = "Reset Your Airline Account Password";

            emailClient.sendEmail(request.recipientEmail(), subject, htmlBody);
            notification.setDeliveryStatus("SENT");
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}", request.recipientEmail(), ex);
            notification.setDeliveryStatus("FAILED");
            notification.setDeliveryError(ex.getMessage());
        }

        repository.save(notification);
    }

    @Async("emailExecutor")
    public void sendAccountVerification(AccountVerificationRequest request) {
        Notification notification = new Notification();
        notification.setEventType("account.verification");
        notification.setRecipient(request.recipientEmail());
        notification.setCreatedAt(LocalDateTime.now());

        try {
            notification.setPayload(objectMapper.writeValueAsString(request));

            Map<String, Object> variables = new HashMap<>();
            variables.put("userName", request.userName());
            variables.put("verificationUrl", request.verificationUrl());

            String htmlBody = templateEngine.render("account-verification", variables);
            String subject = "Verify Your Airline Account";

            emailClient.sendEmail(request.recipientEmail(), subject, htmlBody);
            notification.setDeliveryStatus("SENT");
        } catch (Exception ex) {
            log.error("Failed to send account verification email to {}", request.recipientEmail(), ex);
            notification.setDeliveryStatus("FAILED");
            notification.setDeliveryError(ex.getMessage());
        }

        repository.save(notification);
    }
}
