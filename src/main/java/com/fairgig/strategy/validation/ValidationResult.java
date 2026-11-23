package com.fairgig.strategy.validation;

import java.util.ArrayList;
import java.util.List;

/** The outcome of a validation: no problems = valid. Immutable. */
public final class ValidationResult {

    private static final ValidationResult OK = new ValidationResult(List.of());

    private final List<FieldProblem> problems;

    private ValidationResult(List<FieldProblem> problems) {
        this.problems = problems;
    }

    public static ValidationResult ok() {
        return OK;
    }

    public static ValidationResult of(List<FieldProblem> problems) {
        return problems.isEmpty() ? OK : new ValidationResult(List.copyOf(problems));
    }

    public static ValidationResult problem(String field, String message) {
        return new ValidationResult(List.of(new FieldProblem(field, message)));
    }

    public boolean isValid() {
        return problems.isEmpty();
    }

    public List<FieldProblem> problems() {
        return problems;
    }

    /** Just the texts, in order. */
    public List<String> messages() {
        return problems.stream().map(FieldProblem::message).toList();
    }

    /** This result followed by the other one's problems. */
    public ValidationResult and(ValidationResult other) {
        if (other.isValid()) {
            return this;
        }
        if (isValid()) {
            return other;
        }
        List<FieldProblem> all = new ArrayList<>(problems);
        all.addAll(other.problems);
        return new ValidationResult(List.copyOf(all));
    }

    @Override
    public String toString() {
        return isValid() ? "ValidationResult{valid}" : "ValidationResult" + messages();
    }
}
