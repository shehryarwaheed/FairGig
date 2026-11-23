package com.fairgig.strategy.validation;

import com.fairgig.model.EarningInput;
import java.time.Clock;
import java.time.LocalDate;

/** FR-10: the shift date must be a real, plausible day: not in the future and not absurdly old. */
public class ShiftDateRule implements ValidationStrategy<EarningInput> {

    static final LocalDate EARLIEST = LocalDate.of(2015, 1, 1);

    private final Clock clock;

    public ShiftDateRule() {
        this(Clock.systemDefaultZone());
    }

    /** The clock can be replaced in tests so "today" is fixed. */
    public ShiftDateRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ValidationResult validate(EarningInput in) {
        LocalDate date = in.shiftDate();
        if (date == null) {
            return ValidationResult.ok();
        }
        if (date.isAfter(LocalDate.now(clock))) {
            return ValidationResult.problem("shiftDate", "The shift date cannot be in the future.");
        }
        if (date.isBefore(EARLIEST)) {
            return ValidationResult.problem("shiftDate", "The shift date looks too old. Please check the year.");
        }
        return ValidationResult.ok();
    }
}
