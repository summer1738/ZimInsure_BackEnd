package com.ziminsure.insurance.api.dto;

import java.time.LocalDateTime;

/**
 * Notification response with optional displayMessage for non-recipients
 * (e.g. "Client John's car..." when an admin/agent views a client notification).
 */
public class NotificationDto {
    private Long id;
    private String message;
    /** When set, show this instead of message (e.g. for SUPER_ADMIN/AGENT viewing client notifications). */
    private String displayMessage;
    private String type;
    private LocalDateTime timestamp;
    private boolean read;
    private String forRole;
    private Long agentId;
    private Long clientId;
    private Long carId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getDisplayMessage() { return displayMessage; }
    public void setDisplayMessage(String displayMessage) { this.displayMessage = displayMessage; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public String getForRole() { return forRole; }
    public void setForRole(String forRole) { this.forRole = forRole; }
    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }
}
