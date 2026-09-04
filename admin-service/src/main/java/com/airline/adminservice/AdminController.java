package com.airline.adminservice;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminAuditLogRepository auditLogs;

    AdminController(AdminAuditLogRepository auditLogs) {
        this.auditLogs = auditLogs;
    }

    @GetMapping("/dashboard")
    Map<String, Object> dashboard() {
        return Map.of(
                "service",
                "admin-service",
                "generatedAt",
                Instant.now(),
                "auditActionCount",
                auditLogs.count(),
                "message",
                "Operational data is assembled from service-owned APIs; no cross-service database"
                    + " access is used.");
    }

    @PostMapping("/actions")
    ResponseEntity<Map<String, Object>> action(
            @RequestHeader("X-User-Email") String email, @RequestBody Map<String, Object> action) {
        AdminAuditLog log = new AdminAuditLog();
        log.setActor(email);
        log.setAction(action.toString());
        log.setCreatedAt(LocalDateTime.now());
        auditLogs.save(log);
        return ResponseEntity.accepted()
                .body(Map.of("requestedBy", email, "action", action, "status", "RECORDED"));
    }
}
