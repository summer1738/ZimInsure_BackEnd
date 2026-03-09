package com.ziminsure.insurance.api.dto;

/**
 * DTO for client actions on a quotation: accept / decline / negotiate.
 */
public class ClientQuotationActionDTO {
    /** Action the client is taking: ACCEPT, DECLINE, NEGOTIATE. */
    private String action;
    /** Optional proposed amount when negotiating. */
    private Double proposedAmount;
    /** Optional free-text comment from the client. */
    private String comment;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Double getProposedAmount() {
        return proposedAmount;
    }

    public void setProposedAmount(Double proposedAmount) {
        this.proposedAmount = proposedAmount;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

