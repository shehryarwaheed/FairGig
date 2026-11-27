
package com.fairgig.strategy.analytics;

import com.fairgig.repository.InMemoryEarningsRepository;
import com.fairgig.model.EarningsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class EarningsSummaryStrategyTest {

    private InMemoryEarningsRepository repo;
    private EarningsSummaryStrategy strategy;

    @BeforeEach
    void setUp() {
        repo = new InMemoryEarningsRepository();
        strategy = new EarningsSummaryStrategy(repo);

        // Seed data
        repo.add(new EarningsRecord(1, 1, LocalDate.of(2023,1,10),
                new BigDecimal("120.00"), new BigDecimal("20.00"), 240, "PlatformA"));
    }

    @Test
    void calculatesSummaryCorrectly() {
        EarningsSummary result = strategy.execute(1, null, null);
        assertEquals(new BigDecimal("120.00"), result.getTotalIncome());
        assertEquals(new BigDecimal("20.00"), result.getTotalDeductions());
        // 240 minutes = 4 hours => 120 / 4 = 30.00
        assertEquals(30.0, result.getEffectiveHourlyRate(), 0.01);
    }
}