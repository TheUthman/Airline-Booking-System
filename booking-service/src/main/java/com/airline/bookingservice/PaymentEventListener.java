package com.airline.bookingservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final BookingRepository repository;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    PaymentEventListener(
            BookingRepository repository, StringRedisTemplate redis, ObjectMapper mapper) {
        this.repository = repository;
        this.redis = redis;
        this.mapper = mapper;
    }

    @RabbitListener(queues = MessagingConfig.PAYMENT_QUEUE)
    public void handle(String payload) {
        JsonNode event;
        try {
            event = mapper.readTree(payload);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            // A payload that is not valid JSON can never be processed. ACK and drop it
            // instead of throwing: previously the default error handler requeued the
            // poisoned message on every delivery and it looped forever.
            log.warn(
                    "Discarding unparseable message from queue '{}'; payload: {}",
                    MessagingConfig.PAYMENT_QUEUE,
                    preview(payload),
                    e);
            return;
        }

        // Guard against empty/null bodies (readTree("") returns null) and non-object
        // payloads such as arrays or scalars, which can never carry a bookingId.
        if (event == null || !event.isObject()) {
            log.warn(
                    "Discarding non-object message from queue '{}'; payload: {}",
                    MessagingConfig.PAYMENT_QUEUE,
                    preview(payload));
            return;
        }

        Long bookingId = event.path("bookingId").asLong();
        repository
                .findById(bookingId)
                .ifPresent(
                        b -> {
                            if (b.getStatus() != BookingStatus.PENDING_PAYMENT) return;
                            boolean paid = "payment.succeeded".equals(event.path("type").asText());
                            b.setStatus(paid ? BookingStatus.CONFIRMED : BookingStatus.CANCELLED);
                            repository.save(b);
                            redis.delete("seat-lock:" + b.getFlightId() + ":" + b.getSeatNumber());
                        });
    }

    private static String preview(String payload) {
        if (payload == null) return "<null>";
        return payload.length() <= 300 ? payload : payload.substring(0, 300) + "...";
    }
}
