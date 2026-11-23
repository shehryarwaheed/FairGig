package com.fairgig.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password hashing with BCrypt (NFR-01). Passwords are only ever stored as salted hashes;
 * each hash contains its own random salt, so the same password gives a different hash every time.
 */
public final class PasswordHasher {

    /** Work factor: higher = slower for attackers (and for us). 10 is a good balance. */
    private static final int COST = 10;

    private PasswordHasher() {}

    /** Returns a salted BCrypt hash (60 characters) for the given password. */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password must not be empty.");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST));
    }

    /** True when the password matches the stored hash. Never throws: a bad hash simply does not match. */
    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException malformedHash) {
            return false;
        }
    }
}
