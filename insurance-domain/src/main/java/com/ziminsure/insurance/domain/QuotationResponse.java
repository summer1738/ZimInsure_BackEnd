package com.ziminsure.insurance.domain;

import java.time.LocalDate;

/**
 * DTO for quotation list and create/update responses (avoids exposing entity with lazy relations).
 */
public class QuotationResponse {
    private Long id;
    private String quotationNumber;
    private String policyType;
    private String status;
    private Double amount;
    private LocalDate createdDate;
    private Long clientId;
    private String clientName;
    private Long carId;
    private String carRegNumber;
    private Long agentId;

    public QuotationResponse(Long id, String quotationNumber, String policyType, String status, Double amount,
                             LocalDate createdDate, Long clientId, String clientName, Long carId, String carRegNumber, Long agentId) {
        this.id = id;
        this.quotationNumber = quotationNumber;
        this.policyType = policyType;
        this.status = status;
        this.amount = amount;
        this.createdDate = createdDate;
        this.clientId = clientId;
        this.clientName = clientName;
        this.carId = carId;
        this.carRegNumber = carRegNumber;
        this.agentId = agentId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuotationNumber() { return quotationNumber; }
    public void setQuotationNumber(String quotationNumber) { this.quotationNumber = quotationNumber; }
    public String getPolicyType() { return policyType; }
    public void setPolicyType(String policyType) { this.policyType = policyType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }
    public String getCarRegNumber() { return carRegNumber; }
    public void setCarRegNumber(String carRegNumber) { this.carRegNumber = carRegNumber; }
    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
}
