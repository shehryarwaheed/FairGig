package com.fairgig.unit.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.VerificationStatus;
import com.fairgig.repository.CityStatsRepository.Totals;
import com.fairgig.strategy.analytics.CityMedianComparison;
import com.fairgig.strategy.analytics.CityMedianComparisonStrategy;
import com.fairgig.strategy.analytics.EarningsSummary;
import com.fairgig.strategy.analytics.EarningsSummaryStrategy;
import com.fairgig.strategy.analytics.PlatformTrend;
import com.fairgig.strategy.analytics.PlatformTrendStrategy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** STRATEGY pattern for analytics (FR-23 to FR-25, NFR-13). */
class AnalyticsStrategyTest {

    static EarningsRecord earning(int platform, LocalDate date, String hours, String gross, String ded, String net,
                                  VerificationStatus status) {
        EarningsRecord r = new EarningsRecord();
        r.setPlatformId(platform);
        r.setShiftDate(date);
        r.setHoursWorked(new BigDecimal(hours));
        r.setGrossEarnings(new BigDecimal(gross));
        r.setDeductions(new BigDecimal(ded));
        r.setNetReceived(new BigDecimal(net));
        r.setStatus(status);
        return r;
    }

    private final List<EarningsRecord> records = List.of(
        earning(1, LocalDate.of(2026, 10, 30), "4", "1000", "200", "800", VerificationStatus.VERIFIED),
        earning(1, LocalDate.of(2026, 11, 2), "6", "1500", "300", "1200", VerificationStatus.PENDING),
        earning(2, LocalDate.of(2026, 11, 3), "5", "900", "100", "800", VerificationStatus.PENDING));

    @Test
    void summaryAddsTotalsAndDividesNetByHours() {
        EarningsSummary s = new EarningsSummaryStrategy().analyze(records);
        assertEquals(new BigDecimal("3400.00"), s.totalGross());
        assertEquals(new BigDecimal("600.00"), s.totalDeductions());
        assertEquals(new BigDecimal("2800.00"), s.totalNet());
        assertEquals(new BigDecimal("186.67"), s.effectiveHourlyRate());
    }

    @Test
    void noEarningsGiveZeroInsteadOfAnError() {
        EarningsSummary s = new EarningsSummaryStrategy().analyze(List.of());
        assertEquals(new BigDecimal("0.00"), s.effectiveHourlyRate());
    }

    @Test
    void trendsGroupByPlatformWeekAndMonth() {
        PlatformTrend t = new PlatformTrendStrategy().analyze(records);
        assertEquals(new BigDecimal("2000.00"), t.byPlatform().get(1).totalNet());
        assertEquals(List.of("2026-10", "2026-11"), List.copyOf(t.monthly().keySet()));
        assertEquals(new BigDecimal("2000"), t.monthly().get("2026-11"));
        assertEquals(2, t.weekly().size());
    }

    @Test
    void firstOfJanuaryBelongsToTheIsoWeekOfTheOldYear() {
        PlatformTrend t = new PlatformTrendStrategy().analyze(List.of(
            earning(1, LocalDate.of(2027, 1, 1), "1", "10", "0", "10", VerificationStatus.PENDING)));
        assertEquals("2026-W53", t.weekly().firstKey());
    }

    @Test
    void comparesWithTheMedianOfWorkersInTheCity() {
        CityMedianComparison c = new CityMedianComparisonStrategy(List.of(
            new Totals(new BigDecimal("1000"), BigDecimal.TEN),
            new Totals(new BigDecimal("3000"), BigDecimal.TEN),
            new Totals(new BigDecimal("2000"), BigDecimal.TEN))).analyze(records);
        assertTrue(c.hasMedian());
        assertEquals(new BigDecimal("2000.00"), c.medianNet());
        assertEquals(new BigDecimal("200.00"), c.medianHourlyRate());
    }

    @Test
    void noMedianWhenItWouldRevealSomebody() {
        CityMedianComparison c = new CityMedianComparisonStrategy(List.of(
            new Totals(BigDecimal.TEN, BigDecimal.ONE), new Totals(BigDecimal.ONE, BigDecimal.ONE))).analyze(records);
        assertFalse(c.hasMedian());
    }
}
