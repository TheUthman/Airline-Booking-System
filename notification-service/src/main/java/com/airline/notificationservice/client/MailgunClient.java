package com.airline.notificationservice.client;

import com.airline.notificationservice.config.MailgunProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "notifications.mailgun", name = "api-key")
public class MailgunClient implements EmailClient {

    private static final Logger log = LoggerFactory.getLogger(MailgunClient.class);

    private final MailgunProperties properties;
    private final RestClient client;

    public MailgunClient(MailgunProperties properties) {
        this.properties = properties;
        this.client = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeaders(headers -> {
                    headers.setBasicAuth("api", properties.apiKey());
                    headers.set("User-Agent", "airline-booking-system");
                })
                .build();
    }

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("MAILGUN_API_KEY is not configured");
        }
        if (properties.domain() == null || properties.domain().isBlank()) {
            throw new IllegalStateException("MAILGUN_DOMAIN is not configured");
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("from", properties.from());
        formData.add("to", to);
        formData.add("subject", subject);
        formData.add("html", htmlBody);

        log.info("Sending email via Mailgun to [{}], subject: [{}]", to, subject);

        client.post()
                .uri("/v3/{domain}/messages", properties.domain())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toBodilessEntity();

        log.info("Email successfully sent via Mailgun to [{}]", to);
    }
}
