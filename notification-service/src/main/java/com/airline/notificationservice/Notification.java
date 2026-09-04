package com.airline.notificationservice;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;
    private String recipient;

    @Column(length = 4000)
    private String payload;

    private String deliveryStatus;

    @Column(length = 1000)
    private String deliveryError;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String v) {
        eventType = v;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String v) {
        recipient = v;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String v) {
        payload = v;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String v) {
        deliveryStatus = v;
    }

    public String getDeliveryError() {
        return deliveryError;
    }

    public void setDeliveryError(String v) {
        deliveryError = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime v) {
        createdAt = v;
    }
}
