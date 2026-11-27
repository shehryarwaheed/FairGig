
package com.fairgig.service.analytics;

public final class AnalyticsRequest {
    public enum Type { SUMMARY, PLATFORM_TREND, CITY_MEDIAN }

    private final Type type;
    private final int workerId;
    private final java.time.LocalDate from;
    private final java.time.LocalDate to;

    public AnalyticsRequest(Type type, int workerId,
                            java.time.LocalDate from, java.time.LocalDate to) {
        this.type = type;
        this.workerId = workerId;
        this.from = from;
        this.to = to;
    }

    public Type getType() { return type; }
    public int getWorkerId() { return workerId; }
    public java.time.LocalDate getFrom() { return from; }
    public java.time.LocalDate getTo() { return to; }
}