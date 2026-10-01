package com.airline.authservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;
    private final String frontendUrl;

    public NotificationClient(
            @Value("${app.notification-service-url:http://localhost:8087}") String notificationServiceUrl,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.restClient = RestClient.builder().baseUrl(notificationServiceUrl).build();
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    public void sendEmailVerification(String email, String name, String token) {
        try {
            String verificationUrl = frontendUrl + "/verify-email?token=" + token + "&email=" + email;
            Map<String, Object> body = Map.of(
                    "recipientEmail", email,
                    "userName", (name != null && !name.isBlank()) ? name : email,
                    "verificationUrl", verificationUrl
            );
            log.info("Sending account verification request to notification-service for email: {}", email);
            restClient.post()
                    .uri("/api/notifications/account/verification")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully queued verification email for: {}", email);
        } catch (Exception ex) {
            log.error("Failed to trigger verification email in notification-service for {}: {}", email, ex.getMessage());
        }
    }

    public void sendPasswordReset(String email, String name, String token) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + token + "&email=" + email;
            Map<String, Object> body = Map.of(
                    "recipientEmail", email,
                    "userName", (name != null && !name.isBlank()) ? name : email,
                    "resetUrl", resetUrl,
                    "expireInMinutes", 30
            );
            log.info("Sending password reset request to notification-service for email: {}", email);
            restClient.post()
                    .uri("/api/notifications/account/password-reset")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully queued password reset email for: {}", email);
        } catch (Exception ex) {
            log.error("Failed to trigger password reset email in notification-service for {}: {}", email, ex.getMessage());
        }
    }
}
