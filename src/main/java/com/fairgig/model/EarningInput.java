package com.fairgig.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * What a worker types in to log an earning (before it is validated and saved).
 * Any field may be null here: the validation rules report what is missing.
 * Used by both the manual form (FR-09) and the CSV import (FR-12).
 *
 * @param platformId     the platform the money came from
 * @param shiftDate      the day of the shift
 * @param hoursWorked    hours worked, 0 to 24
 * @param grossEarnings  earnings before deductions
 * @param deductions     what the platform took (commission, fees)
 * @param netReceived    what the worker actually received
 */
public record EarningInput(
        Integer platformId,
        LocalDate shiftDate,
        BigDecimal hoursWorked,
        BigDecimal grossEarnings,
        BigDecimal deductions,
        BigDecimal netReceived) {
}
