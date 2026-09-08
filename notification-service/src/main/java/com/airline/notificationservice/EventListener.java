package com.airline.notificationservice;

import com.airline.notificationservice.dto.BookingConfirmationRequest;
import com.airline.notificationservice.dto.FlightUpdateRequest;
import com.airline.notificationservice.dto.PaymentConfirmationRequest;
import com.airline.notificationservice.service.BookingEmailService;
import com.airline.notificationservice.service.FlightEmailService;
import com.airline.notificationservice.service.PaymentEmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
public class EventListener {

    private static final Logger log = LoggerFactory.getLogger(EventListener.class);

    private final NotificationRepository repository;
    private final ObjectMapper mapper;
    private final PaymentEmailService paymentEmailService;
    private final BookingEmailService bookingEmailService;
    private final FlightEmailService flightEmailService;

    public EventListener(
            NotificationRepository repository,
            ObjectMapper mapper,
            PaymentEmailService paymentEmailService,
            BookingEmailService bookingEmailService,
            FlightEmailService flightEmailService) {
        this.repository = repository;
        this.mapper = mapper;
        this.paymentEmailService = paymentEmailService;
        this.bookingEmailService = bookingEmailService;
        this.flightEmailService = flightEmailService;
    }

    @RabbitListener(queues = "notification.events")
    public void receive(String payload) {
        log.info("Received event on notification.events: {}", payload);
        try {
            JsonNode event = mapper.readTree(payload);
            String eventType = event.path("type").asText("DOMAIN_EVENT");
            String recipient = event.path("recipientEmail").asText("");

            if ("payment.succeeded".equals(eventType) && !recipient.isBlank()) {
                PaymentConfirmationRequest req = new PaymentConfirmationRequest(
                        recipient,
                        event.path("bookingId").asLong(),
                        event.has("paymentId") ? event.path("paymentId").asLong() : null,
                        event.path("providerReference").asText(null),
                        event.has("amount") ? new BigDecimal(event.path("amount").asText("0")) : BigDecimal.ZERO,
                        event.path("paymentMethod").asText("Credit Card")
                );
                paymentEmailService.sendPaymentConfirmation(req);
            } else if ("booking.confirmed".equals(eventType) && !recipient.isBlank()) {
                BookingConfirmationRequest req = new BookingConfirmationRequest(
                        recipient,
                        event.path("bookingId").asLong(),
                        event.path("bookingReference").asText(null),
                        event.path("flightNumber").asText(null),
                        event.path("origin").asText(null),
                        event.path("destination").asText(null),
                        event.path("departureTime").asText(null),
                        event.path("arrivalTime").asText(null),
                        event.has("totalAmount") ? new BigDecimal(event.path("totalAmount").asText("0")) : null,
                        null
                );
                bookingEmailService.sendBookingConfirmation(req);
            } else if ("flight.updated".equals(eventType) && !recipient.isBlank()) {
                FlightUpdateRequest req = new FlightUpdateRequest(
                        recipient,
                        event.path("flightNumber").asText(""),
                        event.path("origin").asText(null),
                        event.path("destination").asText(null),
                        event.path("scheduledDeparture").asText(null),
                        event.path("newDeparture").asText(null),
                        event.path("gate").asText(null),
                        event.path("statusMessage").asText(null)
                );
                flightEmailService.sendFlightUpdate(req);
            } else {
                Notification n = new Notification();
                n.setPayload(payload);
                n.setCreatedAt(LocalDateTime.now());
                n.setEventType(eventType);
                n.setRecipient(recipient);
                n.setDeliveryStatus("SKIPPED");
                repository.save(n);
            }
        } catch (Exception exception) {
            log.error("Failed to parse event payload: {}", payload, exception);
            Notification n = new Notification();
            n.setPayload(payload);
            n.setCreatedAt(LocalDateTime.now());
            n.setEventType("UNKNOWN");
            n.setDeliveryStatus("FAILED");
            n.setDeliveryError(exception.getMessage());
            repository.save(n);
        }
    }
}
