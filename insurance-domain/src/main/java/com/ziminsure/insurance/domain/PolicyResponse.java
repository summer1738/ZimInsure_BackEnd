package com.ziminsure.insurance.domain;

import java.time.LocalDate;

public class PolicyResponse {
    private Long id;
    private String policyNumber;
    private String type;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double premium;
    private Long carId;
    private Long clientId;
    private String clientName;

    public PolicyResponse(Long id, String policyNumber, String type, String status, LocalDate startDate, LocalDate endDate, Double premium, Long carId, Long clientId, String clientName) {
        this.id = id;
        this.policyNumber = policyNumber;
        this.type = type;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.premium = premium;
        this.carId = carId;
        this.clientId = clientId;
        this.clientName = clientName;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Double getPremium() { return premium; }
    public void setPremium(Double premium) { this.premium = premium; }
    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
} 