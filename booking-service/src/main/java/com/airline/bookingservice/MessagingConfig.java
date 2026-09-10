package com.airline.bookingservice;

import org.springframework.amqp.core.*; // DirectExchange, Queue, TopicExchange, Binding, BindingBuilder, QueueBuilder
import org.springframework.context.annotation.*; // Bean, Configuration

@Configuration
public class MessagingConfig {
    public static final String EXCHANGE = "airline.events";
    public static final String PAYMENT_QUEUE = "booking.payment-events";

    /** Dead-letter exchange that receives payment events the listener cannot process. */
    public static final String PAYMENT_DLX = "booking.payment-events.dlx";

    /** Dead-letter queue that parks unprocessable payment events for inspection. */
    public static final String PAYMENT_DLQ = "booking.payment-events.dlq";

    @Bean
    TopicExchange airlineEvents() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    DirectExchange paymentDlx() {
        return new DirectExchange(PAYMENT_DLX, true, false);
    }

    @Bean
    Queue paymentQueue() {
        return QueueBuilder.durable(PAYMENT_QUEUE)
                .withArgument("x-dead-letter-exchange", PAYMENT_DLX)
                .withArgument("x-dead-letter-routing-key", PAYMENT_DLQ)
                .build();
    }

    @Bean
    Queue paymentDlq() {
        return QueueBuilder.durable(PAYMENT_DLQ).build();
    }

    @Bean
    Binding paymentBinding(Queue paymentQueue, TopicExchange airlineEvents) {
        return BindingBuilder.bind(paymentQueue).to(airlineEvents).with("payment.*");
    }

    @Bean
    Binding paymentDlqBinding(Queue paymentDlq, DirectExchange paymentDlx) {
        return BindingBuilder.bind(paymentDlq).to(paymentDlx).with(PAYMENT_DLQ);
    }
}
