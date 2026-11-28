package com.fairgig.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.EarningInput;
import com.fairgig.strategy.validation.AmountRule;
import com.fairgig.strategy.validation.CompositeValidation;
import com.fairgig.strategy.validation.EarningValidation;
import com.fairgig.strategy.validation.FieldProblem;
import com.fairgig.strategy.validation.HoursRule;
import com.fairgig.strategy.validation.ValidationResult;
import com.fairgig.strategy.validation.ValidationStrategy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** STRATEGY pattern (FR-10): small interchangeable rules combined into one validator. */
class EarningValidationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T10:00:00Z"), ZoneOffset.UTC);
    private final ValidationStrategy<EarningInput> rules = EarningValidation.standard(CLOCK);

    private static BigDecimal d(String s) {
        return s == null ? null : new BigDecimal(s);
    }

    private static EarningInput input(Integer platform, LocalDate date, String hours, String gross, String ded, String net) {
        return new EarningInput(platform, date, d(hours), d(gross), d(ded), d(net));
    }

    private static EarningInput valid() {
        return input(1, TODAY, "6.5", "5200.00", "1300", "3900");
    }

    private static List<String> fields(ValidationResult r) {
        return r.problems().stream().map(FieldProblem::field).toList();
    }

    @Test
    void aCorrectEarningHasNoProblems() {
        assertTrue(rules.validate(valid()).isValid());
    }

    @Test
    void emptyFormGivesOneProblemPerFieldAndNeverCrashes() {
        ValidationResult result = rules.validate(input(null, null, null, null, null, null));
        assertEquals(6, result.problems().size());
        assertTrue(fields(result).containsAll(
            List.of("platform", "shiftDate", "hoursWorked", "grossEarnings", "deductions", "netReceived")));
    }

    @Test
    void negativeAmountsAreRefused() {
        ValidationResult result = rules.validate(input(1, TODAY, "5", "-1", "-2", "-3"));
        assertEquals(3, result.problems().size());
        assertTrue(result.messages().get(0).contains("must not be negative"));
    }

    @Test
    void zeroIsAllowedEverywhere() {
        assertTrue(rules.validate(input(1, TODAY, "0", "0", "0", "0")).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.505", "0.001", "1.234"})
    void moreThanTwoDecimalsIsRefused(String amount) {
        assertFalse(rules.validate(input(1, TODAY, "5", amount, "0", "0")).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"10.50", "10.5000", "10", "5E+2"})
    void twoDecimalsOrTrailingZerosAreFine(String amount) {
        assertTrue(rules.validate(input(1, TODAY, "5", amount, "0", "0")).isValid());
    }

    @Test
    void amountMustFitTheDatabaseColumn() {
        assertTrue(rules.validate(input(1, TODAY, "5", "99999999.99", "0", "0")).isValid());
        assertFalse(rules.validate(input(1, TODAY, "5", "100000000", "0", "0")).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "24.01", "6.555", "100"})
    void badHoursAreRefused(String hours) {
        assertFalse(rules.validate(input(1, TODAY, hours, "1", "0", "1")).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "6.25", "24", "8"})
    void goodHoursAreAccepted(String hours) {
        assertTrue(rules.validate(input(1, TODAY, hours, "1", "0", "1")).isValid());
    }

    @Test
    void shiftDateBoundaries() {
        assertFalse(rules.validate(input(1, TODAY.plusDays(1), "5", "1", "0", "1")).isValid(), "tomorrow");
        assertTrue(rules.validate(input(1, TODAY, "5", "1", "0", "1")).isValid(), "today");
        assertTrue(rules.validate(input(1, LocalDate.of(2015, 1, 1), "5", "1", "0", "1")).isValid(), "earliest allowed");
        assertFalse(rules.validate(input(1, LocalDate.of(2014, 12, 31), "5", "1", "0", "1")).isValid(), "too old");
    }

    @Test
    void compositeReportsEveryProblemAtOnce() {
        ValidationResult many = rules.validate(input(null, TODAY.plusDays(2), "30", "-5", "1.234", null));
        assertEquals(6, many.problems().size());
    }

    @Test
    void aSingleRuleCanBeUsedOnItsOwn() {
        assertEquals(1, new HoursRule().validate(input(null, null, "30", null, null, null)).problems().size());
        assertTrue(new AmountRule().validate(input(null, null, null, "10", "1", "9")).isValid());
    }

    @Test
    void strategiesAreInterchangeable() {
        ValidationStrategy<EarningInput> alwaysNo = x -> ValidationResult.problem("custom", "No.");
        ValidationStrategy<EarningInput> both = new CompositeValidation<>(alwaysNo, new HoursRule());
        ValidationResult result = both.validate(input(1, TODAY, "30", "1", "0", "1"));
        assertEquals(List.of("custom", "hoursWorked"), fields(result));
    }

    @Test
    void validationResultsCanBeCombinedAndAreImmutable() {
        ValidationResult a = ValidationResult.problem("x", "one");
        ValidationResult b = ValidationResult.problem("y", "two");
        assertEquals(List.of("one", "two"), a.and(b).messages());
        assertTrue(ValidationResult.ok().and(ValidationResult.ok()).isValid());
        assertEquals(List.of("one"), a.and(ValidationResult.ok()).messages());
    }
}
