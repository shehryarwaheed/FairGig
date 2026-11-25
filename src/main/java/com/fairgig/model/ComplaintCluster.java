package com.fairgig.model;

import java.time.LocalDateTime;

/** A common issue: similar complaints grouped by an advocate (table "complaint_clusters", FR-33). */
public class ComplaintCluster {

    private Integer clusterId;
    private Integer advocateId;
    private String title;
    private String description;
    private LocalDateTime createdAt;

    public ComplaintCluster() {}

    public Integer getClusterId() { return clusterId; }
    public void setClusterId(Integer clusterId) { this.clusterId = clusterId; }

    public Integer getAdvocateId() { return advocateId; }
    public void setAdvocateId(Integer advocateId) { this.advocateId = advocateId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public boolean equals(Object other) {
        return this == other
            || (other instanceof ComplaintCluster that && clusterId != null && clusterId.equals(that.clusterId));
    }

    @Override
    public int hashCode() {
        return clusterId == null ? System.identityHashCode(this) : clusterId.hashCode();
    }

    @Override
    public String toString() {
        return "ComplaintCluster{id=" + clusterId + ", title=" + title + "}";
    }
}
