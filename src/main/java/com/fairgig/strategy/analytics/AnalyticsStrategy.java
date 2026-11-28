package com.fairgig.strategy.analytics;

import com.fairgig.model.EarningsRecord;
import java.util.List;

/** STRATEGY pattern: one interchangeable way of analysing a worker's earnings. */
public interface AnalyticsStrategy<T> {

    T analyze(List<EarningsRecord> records);
}
