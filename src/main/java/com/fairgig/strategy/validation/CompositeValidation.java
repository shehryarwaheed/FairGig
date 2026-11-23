package com.fairgig.strategy.validation;

import java.util.List;

/** Runs several strategies and collects ALL their problems, so the user sees everything at once. */
public class CompositeValidation<T> implements ValidationStrategy<T> {

    private final List<ValidationStrategy<T>> strategies;

    @SafeVarargs
    public CompositeValidation(ValidationStrategy<T>... strategies) {
        this.strategies = List.of(strategies);
    }

    @Override
    public ValidationResult validate(T input) {
        ValidationResult result = ValidationResult.ok();
        for (ValidationStrategy<T> strategy : strategies) {
            result = result.and(strategy.validate(input));
        }
        return result;
    }
}
