package com.ziminsure.insurance.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    private String type; // info, success, warning, error
    private LocalDateTime timestamp;
    private boolean isRead;
    private String forRole; // SUPER_ADMIN, AGENT, CLIENT
    private Long agentId;
    private Long clientId;
    private Long carId;
} 