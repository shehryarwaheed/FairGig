
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

        // Seed a single earnings record for worker 1
        repo.add(new EarningsRecord(
                1,                 // id
                1,                 // workerId
                LocalDate.of(2023, 1, 10),
                new BigDecimal("120.00"),   // income
                new BigDecimal("20.00"),    // deductions
                240,               // minutes worked (4h)
                "PlatformA"));
    }

    @Test
    void summaryIsCalculatedCorrectly() {
        EarningsSummary result = strategy.execute(1, null, null);
        assertEquals(new BigDecimal("120.00"), result.getTotalIncome());
        assertEquals(new BigDecimal("20.00"),  result.getTotalDeductions());
        // 120 / 4h = 30.0
        assertEquals(30.0, result.getEffectiveHourlyRate(), 0.01);
    }
}