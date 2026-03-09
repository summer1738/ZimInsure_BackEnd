package com.ziminsure.insurance.service;

import com.ziminsure.insurance.domain.Notification;
import com.ziminsure.insurance.domain.User;

import java.util.List;
import java.util.Optional;

public interface NotificationService {
    Notification addNotification(String message, String type, String forRole, Long agentId, Long clientId, Long carId);
    List<Notification> getNotificationsByRole(String forRole);
    List<Notification> getNotificationsByAgentId(Long agentId);
    List<Notification> getNotificationsByClientId(Long clientId);
    Optional<Notification> markAsRead(Long notificationId);
    void markAllAsReadByUser(String forRole, Long agentId, Long clientId);
    /** Mark all as read for the given user (by role: CLIENT by clientId, AGENT by agentId, SUPER_ADMIN all). */
    void markAllAsReadForUser(User user);
    List<Notification> getAllNotifications();
    Optional<Notification> findById(Long id);
    void deleteById(Long id);
} 