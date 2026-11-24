package com.fairgig.model;

/**
 * Where a complaint is in its life (FR-31). The rules for moving between statuses belong to the
 * State pattern (complaint service). The database stores name() in complaints.status.
 */
public enum ComplaintStatus {
    SUBMITTED("Submitted"),
    ESCALATED("Escalated"),
    RESOLVED("Resolved");

    private final String label;

    ComplaintStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static ComplaintStatus fromDb(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
