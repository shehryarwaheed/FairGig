
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

        // Two records on different platforms / weeks
        repo.add(new EarningsRecord(1, 1,
                LocalDate.of(2023, 1, 3), new BigDecimal("100.00"),
                BigDecimal.ZERO, 180, "PlatformA"));
        repo.add(new EarningsRecord(2, 1,
                LocalDate.of(2023, 1, 10), new BigDecimal("150.00"),
                BigDecimal.ZERO, 240, "PlatformB"));
    }

    @Test
    void producesAggregatedTrend() {
        PlatformTrend trend = strategy.execute(1, null, null);
        var map = trend.getPlatformToPeriodIncome();

        assertTrue(map.containsKey("PlatformA"));
        assertTrue(map.containsKey("PlatformB"));

        // each platform should have exactly one week key (the test data covers only one week each)
        assertEquals(1, map.get("PlatformA").size());
        assertEquals(1, map.get("PlatformB").size());
    }
}