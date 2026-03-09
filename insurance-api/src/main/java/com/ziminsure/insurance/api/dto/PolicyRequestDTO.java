package com.ziminsure.insurance.api.dto;

import java.time.LocalDate;

/**
 * DTO for creating or updating a policy (avoids deserializing Policy entity with @JsonBackReference).
 */
public class PolicyRequestDTO {
    private String policyNumber;
    private String type;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double premium;
    private Long carId;
    private Long clientId;
    /** Name of the insurance company for this policy. */
    private String insuranceCompany;

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

    public String getInsuranceCompany() {
        return insuranceCompany;
    }

    public void setInsuranceCompany(String insuranceCompany) {
        this.insuranceCompany = insuranceCompany;
    }
}
