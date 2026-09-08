package com.airline.notificationservice.controller;

import com.airline.notificationservice.Notification;
import com.airline.notificationservice.NotificationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository repository;

    public NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Notification> mine(@RequestHeader("X-User-Email") String email) {
        return repository.findTop50ByRecipientOrderByCreatedAtDesc(email);
    }
}
