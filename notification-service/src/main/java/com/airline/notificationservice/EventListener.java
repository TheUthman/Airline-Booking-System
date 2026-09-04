package com.airline.notificationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Configuration
class NotificationQueueConfig {
    @Bean
    Queue notificationQueue() {
        return QueueBuilder.durable("notification.events").build();
    }

    @Bean
    Binding notificationBinding(Queue notificationQueue) {
        return BindingBuilder.bind(notificationQueue)
                .to(new TopicExchange("airline.events", true, false))
                .with("#");
    }
}

@Component
class EventListener {
    private final NotificationRepository repository;
    private final ObjectMapper mapper;
    private final ResendEmailSender emailSender;

    EventListener(
            NotificationRepository repository, ObjectMapper mapper, ResendEmailSender emailSender) {
        this.repository = repository;
        this.mapper = mapper;
        this.emailSender = emailSender;
    }

    @RabbitListener(queues = "notification.events")
    public void receive(String payload) {
        Notification n = new Notification();
        n.setPayload(payload);
        n.setCreatedAt(LocalDateTime.now());
        try {
            JsonNode event = mapper.readTree(payload);
            String eventType = event.path("type").asText("DOMAIN_EVENT");
            String recipient = event.path("recipientEmail").asText("");
            n.setEventType(eventType);
            n.setRecipient(recipient);
            if ("payment.succeeded".equals(eventType) && !recipient.isBlank()) {
                emailSender.sendPaymentConfirmation(recipient, event.path("bookingId").asLong());
                n.setDeliveryStatus("SENT");
            } else {
                n.setDeliveryStatus("SKIPPED");
            }
        } catch (Exception exception) {
            n.setDeliveryStatus("FAILED");
            n.setDeliveryError(exception.getMessage());
        }
        repository.save(n);
    }
}
