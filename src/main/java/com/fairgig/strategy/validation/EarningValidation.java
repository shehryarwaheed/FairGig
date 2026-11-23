package com.fairgig.strategy.validation;

import com.fairgig.model.EarningInput;
import java.time.Clock;

/** Builds the standard set of earning rules (required fields, amounts, hours, date). */
public final class EarningValidation {

    private EarningValidation() {}

    public static ValidationStrategy<EarningInput> standard() {
        return standard(Clock.systemDefaultZone());
    }

    public static ValidationStrategy<EarningInput> standard(Clock clock) {
        return new CompositeValidation<>(
            new RequiredFieldsRule(),
            new AmountRule(),
            new HoursRule(),
            new ShiftDateRule(clock));
    }
}
