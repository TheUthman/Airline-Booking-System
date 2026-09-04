package com.airline.notificationservice;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repository;

    NotificationController(NotificationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    List<Notification> mine(@RequestHeader("X-User-Email") String email) {
        return repository.findTop50ByRecipientOrderByCreatedAtDesc(email);
    }
}
