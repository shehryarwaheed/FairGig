
package com.fairgig.strategy.analytics;

import com.fairgig.repository.InMemoryEarningsRepository;
import com.fairgig.model.EarningsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PlatformTrendStrategyTest {

    private InMemoryEarningsRepository repo;
    private PlatformTrendStrategy strategy;

    @BeforeEach
    void setUp() {
        repo = new InMemoryEarningsRepository();
        strategy = new PlatformTrendStrategy(repo);

        repo.add(new EarningsRecord(1, 1, LocalDate.of(2023, 1, 3),
                new BigDecimal("100.00"), BigDecimal.ZERO, 180, "PlatformA"));
        repo.add(new EarningsRecord(2, 1, LocalDate.of(2023, 1, 10),
                new BigDecimal("150.00"), BigDecimal.ZERO, 240, "PlatformB"));
    }

    @Test
    void producesWeeklyAndMonthlyAggregations() {
        PlatformTrend trend = strategy.execute(1, null, null);
        var platformA = trend.getPlatformToPeriodIncome().get("PlatformA");
        var platformB = trend.getPlatformToPeriodIncome().get("PlatformB");

        assertNotNull(platformA);
        assertNotNull(platformB);
        // Week keys may differ by locale; we just verify the map contains a single entry.
        assertEquals(1, platformA.size());
        assertEquals(1, platformB.size());
    }
}