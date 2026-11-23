package com.fairgig.strategy.validation;

import com.fairgig.model.EarningInput;
import java.util.ArrayList;
import java.util.List;

/** FR-10: every field of an earning must be filled in. */
public class RequiredFieldsRule implements ValidationStrategy<EarningInput> {

    @Override
    public ValidationResult validate(EarningInput in) {
        List<FieldProblem> problems = new ArrayList<>();
        if (in.platformId() == null) {
            problems.add(new FieldProblem("platform", "Please choose the platform."));
        }
        if (in.shiftDate() == null) {
            problems.add(new FieldProblem("shiftDate", "Please enter the shift date."));
        }
        if (in.hoursWorked() == null) {
            problems.add(new FieldProblem("hoursWorked", "Please enter the hours worked, for example 6.5."));
        }
        if (in.grossEarnings() == null) {
            problems.add(new FieldProblem("grossEarnings", "Please enter the gross earnings (before deductions)."));
        }
        if (in.deductions() == null) {
            problems.add(new FieldProblem("deductions", "Please enter the deductions. Enter 0 if there were none."));
        }
        if (in.netReceived() == null) {
            problems.add(new FieldProblem("netReceived", "Please enter the net amount you received."));
        }
        return ValidationResult.of(problems);
    }
}
