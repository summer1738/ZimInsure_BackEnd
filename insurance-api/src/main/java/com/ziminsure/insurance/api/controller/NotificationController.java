package com.ziminsure.insurance.api.controller;

import com.ziminsure.insurance.domain.Notification;
import com.ziminsure.insurance.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/role/{role}")
    public List<Notification> getByRole(@PathVariable("role") String role) {
        return notificationService.getNotificationsByRole(role);
    }

    @GetMapping("/agent/{agentId}")
    public List<Notification> getByAgent(@PathVariable("agentId") Long agentId) {
        return notificationService.getNotificationsByAgentId(agentId);
    }

    @GetMapping("/client/{clientId}")
    public List<Notification> getByClient(@PathVariable("clientId") Long clientId) {
        return notificationService.getNotificationsByClientId(clientId);
    }

    @GetMapping
    public List<Notification> getAllNotifications() {
        return notificationService.getAllNotifications();
    }

    @PostMapping
    public Notification addNotification(@RequestBody Notification notification) {
        return notificationService.addNotification(
                notification.getMessage(),
                notification.getType(),
                notification.getForRole(),
                notification.getAgentId(),
                notification.getClientId(),
                notification.getCarId()
        );
    }

    @PutMapping("/read/{id}")
    public Optional<Notification> markAsRead(@PathVariable("id") Long id) {
        return notificationService.markAsRead(id);
    }

    @PutMapping("/readAll")
    public void markAllAsRead(@RequestParam(name = "forRole", required = false) String forRole,
                              @RequestParam(name = "agentId", required = false) Long agentId,
                              @RequestParam(name = "clientId", required = false) Long clientId) {
        notificationService.markAllAsReadByUser(forRole, agentId, clientId);
    }
} 