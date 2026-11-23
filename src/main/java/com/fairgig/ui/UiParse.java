package com.fairgig.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/**
 * Turns what the user typed into dates and numbers. A blank box gives null (the backend rules
 * then say what is missing); text that is not a date/number throws IllegalArgumentException
 * with a message that is ready to show next to the field.
 */
public final class UiParse {

    private static final DateTimeFormatter DAY_FIRST =
        DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern THOUSANDS = Pattern.compile("^\\d{1,3}(,\\d{3})+(\\.\\d+)?$");

    private UiParse() {}

    public static LocalDate date(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException ignored) {
            // try day/month/year
        }
        try {
            return LocalDate.parse(t, DAY_FIRST);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Use a date like 2026-11-23 (or 23/11/2026).");
        }
    }

    public static BigDecimal decimal(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (THOUSANDS.matcher(t).matches()) {
            t = t.replace(",", "");
        }
        try {
            if (t.indexOf('e') >= 0 || t.indexOf('E') >= 0) {
                throw new NumberFormatException();
            }
            return new BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a number like 1250.50 (digits only).");
        }
    }
}
