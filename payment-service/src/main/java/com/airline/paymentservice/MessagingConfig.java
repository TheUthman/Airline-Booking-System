package com.airline.paymentservice;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {
    @Bean
    TopicExchange airlineEventsExchange() {
        return new TopicExchange("airline.events", true, false);
    }
}
