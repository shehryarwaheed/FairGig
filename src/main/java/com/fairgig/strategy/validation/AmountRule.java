package com.fairgig.strategy.validation;

import com.fairgig.model.EarningInput;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-10: money amounts must not be negative, have at most 2 decimals, and fit the database
 * column (DECIMAL(10,2)). Fields that are empty are skipped here (RequiredFieldsRule reports them).
 */
public class AmountRule implements ValidationStrategy<EarningInput> {

    /** Largest value that fits DECIMAL(10,2). */
    static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    @Override
    public ValidationResult validate(EarningInput in) {
        List<FieldProblem> problems = new ArrayList<>();
        check("grossEarnings", "gross earnings", in.grossEarnings(), problems);
        check("deductions", "deductions", in.deductions(), problems);
        check("netReceived", "net amount", in.netReceived(), problems);
        return ValidationResult.of(problems);
    }

    private static void check(String field, String label, BigDecimal value, List<FieldProblem> problems) {
        if (value == null) {
            return;
        }
        String capitalised = Character.toUpperCase(label.charAt(0)) + label.substring(1);
        if (value.signum() < 0) {
            problems.add(new FieldProblem(field, capitalised + " must not be negative. Enter 0 or more."));
        } else if (value.stripTrailingZeros().scale() > 2) {
            problems.add(new FieldProblem(field, capitalised + " can have at most 2 decimal places, for example 1250.50."));
        } else if (value.compareTo(MAX_AMOUNT) > 0) {
            problems.add(new FieldProblem(field, capitalised + " is too large. Please check the amount."));
        }
    }
}
