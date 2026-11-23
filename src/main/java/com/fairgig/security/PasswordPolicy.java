package com.fairgig.security;

import java.util.ArrayList;
import java.util.List;

/** Rules for a new password. Messages say what is wrong and how to fix it (NFR-10). */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    /** BCrypt only reads the first 72 characters, so longer passwords would be silently cut. */
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {}

    /** Returns the list of problems (empty list = the password is acceptable). */
    public static List<String> problems(String password) {
        List<String> problems = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            problems.add("Please enter a password.");
            return problems;
        }
        if (password.length() < MIN_LENGTH) {
            problems.add("Your password must be at least " + MIN_LENGTH + " characters long.");
        }
        if (password.length() > MAX_LENGTH) {
            problems.add("Your password must be " + MAX_LENGTH + " characters or fewer.");
        }
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            problems.add("Your password must contain at least one letter and one number.");
        }
        return problems;
    }
}
