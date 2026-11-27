
package com.fairgig.service.analytics;

public final class AnalyticsResult {
    private final Object payload;

    public AnalyticsResult(Object payload) {
        this.payload = payload;
    }

    public Object getPayload() { return payload; }
}