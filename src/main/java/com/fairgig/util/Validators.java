package com.fairgig.util;

import java.util.regex.Pattern;

/** Small, reusable input checks shared by the services. */
public final class Validators {

    private static final Pattern EMAIL =
        Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private Validators() {}

    public static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /** Trims the text; null becomes an empty string. */
    public static String clean(String text) {
        return text == null ? "" : text.trim();
    }

    /** Trims the text; blank becomes null (for optional database columns). */
    public static String blankToNull(String text) {
        return isBlank(text) ? null : text.trim();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }
}
