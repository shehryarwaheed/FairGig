package com.fairgig.model;

import java.time.LocalDateTime;

/** A representative's answer to a complaint (table "complaint_responses", FR-32). */
public class ComplaintResponse {

    private Integer responseId;
    private Integer complaintId;
    private Integer representativeId;
    private String message;
    private LocalDateTime respondedAt;

    public ComplaintResponse() {}

    public Integer getResponseId() { return responseId; }
    public void setResponseId(Integer responseId) { this.responseId = responseId; }

    public Integer getComplaintId() { return complaintId; }
    public void setComplaintId(Integer complaintId) { this.complaintId = complaintId; }

    public Integer getRepresentativeId() { return representativeId; }
    public void setRepresentativeId(Integer representativeId) { this.representativeId = representativeId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getRespondedAt() { return respondedAt; }
    public void setRespondedAt(LocalDateTime respondedAt) { this.respondedAt = respondedAt; }

    @Override
    public boolean equals(Object other) {
        return this == other
            || (other instanceof ComplaintResponse that && responseId != null && responseId.equals(that.responseId));
    }

    @Override
    public int hashCode() {
        return responseId == null ? System.identityHashCode(this) : responseId.hashCode();
    }
}
