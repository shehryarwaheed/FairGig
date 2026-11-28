package com.fairgig.strategy.analytics;

import com.fairgig.model.EarningsRecord;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.temporal.IsoFields;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** FR-24: compares platforms and shows weekly and monthly trends. */
public class PlatformTrendStrategy implements AnalyticsStrategy<PlatformTrend> {

    private final EarningsSummaryStrategy summary = new EarningsSummaryStrategy();

    @Override
    public PlatformTrend analyze(List<EarningsRecord> records) {
        Map<Integer, EarningsSummary> byPlatform = new LinkedHashMap<>();
        records.stream().collect(Collectors.groupingBy(EarningsRecord::getPlatformId))
            .forEach((platform, rows) -> byPlatform.put(platform, summary.analyze(rows)));
        SortedMap<String, BigDecimal> weekly = new TreeMap<>();
        SortedMap<String, BigDecimal> monthly = new TreeMap<>();
        for (EarningsRecord r : records) {
            // week-based year, so that 1 January is counted in the right ISO week
            String week = String.format("%d-W%02d", r.getShiftDate().get(IsoFields.WEEK_BASED_YEAR),
                r.getShiftDate().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            weekly.merge(week, r.getNetReceived(), BigDecimal::add);
            monthly.merge(YearMonth.from(r.getShiftDate()).toString(), r.getNetReceived(), BigDecimal::add);
        }
        return new PlatformTrend(byPlatform, weekly, monthly);
    }
}
