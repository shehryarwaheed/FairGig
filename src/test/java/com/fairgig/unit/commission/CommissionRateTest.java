package com.fairgig.unit.commission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.CommissionRate;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** FR-26: a rate that jumps by 5 percentage points or more is a "sudden change". */
class CommissionRateTest {

    private static CommissionRate rate(String percent, String previous) {
        CommissionRate r = new CommissionRate();
        r.setRatePercent(new BigDecimal(percent));
        r.setPreviousPercent(previous == null ? null : new BigDecimal(previous));
        return r;
    }

    @Test
    @DisplayName("the first rate has nothing to compare with, so it is never sudden")
    void firstRateIsNeverSudden() {
        CommissionRate first = rate("25", null);
        assertNull(first.changeFromPrevious());
        assertFalse(first.isSuddenChange());
    }

    @ParameterizedTest(name = "{1}% -> {0}% sudden = {2}")
    @CsvSource({
        "20.00, 20.00, false",     // no change
        "22.50, 20.00, false",     // +2.5 points
        "24.99, 20.00, false",     // just under the limit
        "25.00, 20.00, true",      // exactly 5 points counts
        "30.00, 20.00, true",      // big jump up
        "15.00, 20.00, true",      // exactly 5 points down also counts
        "10.00, 20.00, true",      // big drop
        "17.50, 20.00, false"      // -2.5 points
    })
    void thresholdIsFivePercentagePoints(String now, String before, boolean sudden) {
        assertEquals(sudden, rate(now, before).isSuddenChange());
    }

    @Test
    void changeIsSigned() {
        assertEquals(0, new BigDecimal("5.00").compareTo(rate("25.00", "20.00").changeFromPrevious()));
        assertEquals(0, new BigDecimal("-5.00").compareTo(rate("15.00", "20.00").changeFromPrevious()));
    }

    @Test
    void thresholdConstantIsFive() {
        assertTrue(CommissionRate.SUDDEN_CHANGE_POINTS.compareTo(new BigDecimal("5")) == 0);
    }
}
