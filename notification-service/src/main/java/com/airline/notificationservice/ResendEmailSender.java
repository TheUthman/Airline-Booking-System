package com.airline.notificationservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
class ResendEmailSender {
    private final String apiKey;
    private final String from;
    private final RestClient client;

    ResendEmailSender(
            @Value("${notifications.email.resend-api-key:}") String apiKey,
            @Value("${notifications.email.from:Airline Booking <onboarding@resend.dev>}")
                    String from,
            RestClient.Builder builder) {
        this.apiKey = apiKey;
        this.from = from;
        this.client =
                builder.baseUrl("https://api.resend.com")
                        .defaultHeader("User-Agent", "airline-booking-system-school-project")
                        .build();
    }

    void sendPaymentConfirmation(String recipient, long bookingId) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("RESEND_API_KEY is not configured");
        }
        client.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(
                        Map.of(
                                "from",
                                from,
                                "to",
                                new String[] {recipient},
                                "subject",
                                "Your airline booking is confirmed",
                                "html",
                                "<h1>Booking confirmed</h1><p>Your payment was successful. Your"
                                    + " booking reference is "
                                        + bookingId
                                        + ".</p>"))
                .retrieve()
                .toBodilessEntity();
    }
}
