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
    /** Which insurance company this quotation is for. */
    private String insuranceCompany;
    /** Optional ID of the policy this quotation is based on. */
    private Long policyId;
    /** Latest amount proposed by the client when negotiating (may be null). */
    private Double clientProposedAmount;
    /** Latest comment from the client regarding this quotation. */
    private String clientComment;

    public QuotationResponse(Long id, String quotationNumber, String policyType, String status, Double amount,
                             LocalDate createdDate, Long clientId, String clientName, Long carId, String carRegNumber,
                             Long agentId, String insuranceCompany, Long policyId, Double clientProposedAmount, String clientComment) {
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
        this.insuranceCompany = insuranceCompany;
        this.policyId = policyId;
        this.clientProposedAmount = clientProposedAmount;
        this.clientComment = clientComment;
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

    public String getInsuranceCompany() {
        return insuranceCompany;
    }

    public void setInsuranceCompany(String insuranceCompany) {
        this.insuranceCompany = insuranceCompany;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public Double getClientProposedAmount() {
        return clientProposedAmount;
    }

    public void setClientProposedAmount(Double clientProposedAmount) {
        this.clientProposedAmount = clientProposedAmount;
    }

    public String getClientComment() {
        return clientComment;
    }

    public void setClientComment(String clientComment) {
        this.clientComment = clientComment;
    }
}
