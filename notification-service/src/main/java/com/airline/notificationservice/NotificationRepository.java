package com.airline.notificationservice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByRecipientOrderByCreatedAtDesc(String recipient);
}
