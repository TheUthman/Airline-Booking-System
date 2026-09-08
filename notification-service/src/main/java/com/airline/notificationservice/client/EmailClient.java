package com.airline.notificationservice.client;

public interface EmailClient {
    void sendEmail(String to, String subject, String htmlBody);
}
