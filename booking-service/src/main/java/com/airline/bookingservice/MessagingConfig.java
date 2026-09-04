package com.airline.bookingservice;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.*;

@Configuration
public class MessagingConfig {
    public static final String EXCHANGE = "airline.events";
    public static final String PAYMENT_QUEUE = "booking.payment-events";

    @Bean
    TopicExchange airlineEvents() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE).build();
    }

    @Bean
    Binding paymentBinding(Queue paymentQueue, TopicExchange airlineEvents) {
        return BindingBuilder.bind(paymentQueue).to(airlineEvents).with("payment.*");
    }
}
