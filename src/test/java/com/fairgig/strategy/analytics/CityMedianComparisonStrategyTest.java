
package com.fairgig.strategy.analytics;

import com.fairgig.repository.InMemoryEarningsRepository;
import com.fairgig.model.EarningsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class CityMedianComparisonStrategyTest {

    private InMemoryEarningsRepository repo;
    private CityMedianComparisonStrategy strategy;

    @BeforeEach
    void setUp() {
        repo = new InMemoryEarningsRepository();
        strategy = new CityMedianComparisonStrategy(repo);

        // Worker 1 (target)
        repo.add(new EarningsRecord(1, 1,
                LocalDate.of(2023, 1, 1), new BigDecimal("200.00"),
                BigDecimal.ZERO, 300, "P1"));
        // Workers 2 & 3 – used to compute the city median
        repo.add(new EarningsRecord(2, 2,
                LocalDate.of(2023, 1, 1), new BigDecimal("150.00"),
                BigDecimal.ZERO, 300, "P1"));
        repo.add(new EarningsRecord(3, 3,
                LocalDate.of(2023, 1, 1), new BigDecimal("250.00"),
                BigDecimal.ZERO, 300, "P1"));
    }

    @Test
    void medianIsReturnedCorrectly() {
        CityMedianComparison result = strategy.execute(1, null, null);
        assertEquals(new BigDecimal("200.00"), result.getWorkerIncome());
        // Median of (150,200,250) = 200
        assertEquals(new BigDecimal("200.00"), result.getCityMedianIncome());
        assertFalse(result.isAboveMedian());
    }
}