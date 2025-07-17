package com.ziminsure.insurance.repository;

import com.ziminsure.insurance.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByForRole(String forRole);
    List<Notification> findByAgentId(Long agentId);
    List<Notification> findByClientId(Long clientId);
    List<Notification> findByCarId(Long carId);
    List<Notification> findByIsReadFalseAndForRole(String forRole);
    List<Notification> findByIsReadFalseAndAgentId(Long agentId);
    List<Notification> findByIsReadFalseAndClientId(Long clientId);
} 