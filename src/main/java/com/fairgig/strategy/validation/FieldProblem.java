package com.fairgig.strategy.validation;

/**
 * One thing that is wrong with an input.
 *
 * @param field   which field it is about, e.g. "grossEarnings" (the CSV import shows it as the column)
 * @param message what is wrong and how to fix it, ready to show to the user (NFR-10)
 */
public record FieldProblem(String field, String message) {
}
