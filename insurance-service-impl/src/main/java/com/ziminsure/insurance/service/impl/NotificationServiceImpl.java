package com.ziminsure.insurance.service.impl;

import com.ziminsure.insurance.domain.Notification;
import com.ziminsure.insurance.repository.NotificationRepository;
import com.ziminsure.insurance.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationServiceImpl implements NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public Notification addNotification(String message, String type, String forRole, Long agentId, Long clientId, Long carId) {
        Notification notification = Notification.builder()
                .message(message)
                .type(type)
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .forRole(forRole)
                .agentId(agentId)
                .clientId(clientId)
                .carId(carId)
                .build();
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> getNotificationsByRole(String forRole) {
        return notificationRepository.findByForRole(forRole);
    }

    @Override
    public List<Notification> getNotificationsByAgentId(Long agentId) {
        return notificationRepository.findByAgentId(agentId);
    }

    @Override
    public List<Notification> getNotificationsByClientId(Long clientId) {
        return notificationRepository.findByClientId(clientId);
    }

    @Override
    public Optional<Notification> markAsRead(Long notificationId) {
        Optional<Notification> notificationOpt = notificationRepository.findById(notificationId);
        notificationOpt.ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        return notificationOpt;
    }

    @Override
    public void markAllAsReadByUser(String forRole, Long agentId, Long clientId) {
        if (forRole != null) {
            notificationRepository.findByIsReadFalseAndForRole(forRole)
                    .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
        }
        if (agentId != null) {
            notificationRepository.findByIsReadFalseAndAgentId(agentId)
                    .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
        }
        if (clientId != null) {
            notificationRepository.findByIsReadFalseAndClientId(clientId)
                    .forEach(n -> { n.setRead(true); notificationRepository.save(n); });
        }
    }

    @Override
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
} 