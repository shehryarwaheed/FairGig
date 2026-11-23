package com.fairgig.strategy.validation;

/**
 * STRATEGY pattern: one interchangeable rule (or set of rules) for checking an input.
 * The earnings service only knows this interface, so rules can be added, removed or swapped
 * (for the manual form, the CSV import, or in unit tests) without touching the service.
 *
 * @param <T> the kind of input being checked
 */
public interface ValidationStrategy<T> {

    /** Checks the input. Must never throw for bad input (including null fields): report problems instead. */
    ValidationResult validate(T input);
}
