package com.airline.bookingservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {
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
    public void handle(String payload) throws Exception {
        JsonNode event = mapper.readTree(payload);
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
}
