package com.ziminsure.insurance.api.dto;

import java.time.LocalDate;

/**
 * DTO for creating or updating a quotation (avoids deserializing Quotation entity with lazy relations).
 */
public class QuotationRequestDTO {
    private String quotationNumber;
    private String policyType;
    private String status;
    private Double amount;
    private LocalDate createdDate;
    private Long clientId;
    private Long carId;

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
    public Long getCarId() { return carId; }
    public void setCarId(Long carId) { this.carId = carId; }
}
