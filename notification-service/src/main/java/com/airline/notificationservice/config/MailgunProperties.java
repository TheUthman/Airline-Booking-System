package com.airline.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifications.mailgun")
public record MailgunProperties(
        String apiKey,
        String domain,
        String baseUrl,
        String from
) {
    public MailgunProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.mailgun.net";
        }
        if (from == null || from.isBlank()) {
            from = "Airline Booking <postmaster@" + (domain != null && !domain.isBlank() ? domain : "localhost") + ">";
        }
    }
}
