package com.fairgig.model;

import java.time.LocalDateTime;

/** A worker's complaint about a platform (table "complaints"), optionally linked to an earning (FR-30). */
public class Complaint {

    private Integer complaintId;
    private Integer workerId;
    private Integer platformId;
    private Integer recordId;                 // optional: the earning the complaint is about
    private Integer clusterId;                // set when an advocate groups it into a common issue
    private Integer handledByAdvocateId;      // set when an advocate escalates or resolves it
    private Integer resolvedByRepId;          // set when a representative resolves it
    private ComplaintCategory category;
    private String description;
    private ComplaintStatus status = ComplaintStatus.SUBMITTED;
    private LocalDateTime createdAt;
    private LocalDateTime escalatedAt;
    private LocalDateTime resolvedAt;

    public Complaint() {}

    public Integer getComplaintId() { return complaintId; }
    public void setComplaintId(Integer complaintId) { this.complaintId = complaintId; }

    public Integer getWorkerId() { return workerId; }
    public void setWorkerId(Integer workerId) { this.workerId = workerId; }

    public Integer getPlatformId() { return platformId; }
    public void setPlatformId(Integer platformId) { this.platformId = platformId; }

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }

    public Integer getClusterId() { return clusterId; }
    public void setClusterId(Integer clusterId) { this.clusterId = clusterId; }

    public Integer getHandledByAdvocateId() { return handledByAdvocateId; }
    public void setHandledByAdvocateId(Integer handledByAdvocateId) { this.handledByAdvocateId = handledByAdvocateId; }

    public Integer getResolvedByRepId() { return resolvedByRepId; }
    public void setResolvedByRepId(Integer resolvedByRepId) { this.resolvedByRepId = resolvedByRepId; }

    public ComplaintCategory getCategory() { return category; }
    public void setCategory(ComplaintCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ComplaintStatus getStatus() { return status; }
    public void setStatus(ComplaintStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(LocalDateTime escalatedAt) { this.escalatedAt = escalatedAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    @Override
    public boolean equals(Object other) {
        return this == other
            || (other instanceof Complaint that && complaintId != null && complaintId.equals(that.complaintId));
    }

    @Override
    public int hashCode() {
        return complaintId == null ? System.identityHashCode(this) : complaintId.hashCode();
    }

    @Override
    public String toString() {
        return "Complaint{id=" + complaintId + ", category=" + category + ", status=" + status + "}";
    }
}
