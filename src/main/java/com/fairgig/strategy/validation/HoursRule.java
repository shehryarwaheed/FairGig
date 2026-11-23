package com.fairgig.strategy.validation;

import com.fairgig.model.EarningInput;
import java.math.BigDecimal;

/** FR-10: hours worked must be between 0 and 24 with at most 2 decimals (one shift = one day). */
public class HoursRule implements ValidationStrategy<EarningInput> {

    static final BigDecimal MAX_HOURS = new BigDecimal("24");

    @Override
    public ValidationResult validate(EarningInput in) {
        BigDecimal hours = in.hoursWorked();
        if (hours == null) {
            return ValidationResult.ok();
        }
        if (hours.signum() < 0 || hours.compareTo(MAX_HOURS) > 0) {
            return ValidationResult.problem("hoursWorked",
                "Hours worked must be between 0 and 24, for example 6.5.");
        }
        if (hours.stripTrailingZeros().scale() > 2) {
            return ValidationResult.problem("hoursWorked",
                "Hours worked can have at most 2 decimal places, for example 6.25.");
        }
        return ValidationResult.ok();
    }
}
