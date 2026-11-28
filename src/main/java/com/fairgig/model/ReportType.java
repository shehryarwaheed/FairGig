package com.fairgig.model;

/** Kinds of report. The names match the database CHECK constraint. */
public enum ReportType {
    MONTHLY("Monthly report"),
    YEARLY("Yearly report"),
    INCOME_CERTIFICATE("Income certificate");

    private final String label;

    ReportType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
