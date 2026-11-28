package com.fairgig.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.security.PasswordHasher;
import com.fairgig.security.PasswordPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** NFR-01: passwords are only stored as salted BCrypt hashes; policy rules for new passwords. */
class PasswordSecurityTest {

    @Test
    void hashIsSaltedBcryptAndNeverContainsThePassword() {
        String first = PasswordHasher.hash("Secret123");
        String second = PasswordHasher.hash("Secret123");
        assertTrue(first.startsWith("$2a$"));
        assertEquals(60, first.length());
        assertFalse(first.contains("Secret123"));
        assertNotEquals(first, second, "every hash gets its own random salt");
    }

    @Test
    void matchesOnlyTheRightPassword() {
        String hash = PasswordHasher.hash("Secret123");
        assertTrue(PasswordHasher.matches("Secret123", hash));
        assertFalse(PasswordHasher.matches("secret123", hash));
    }

    @Test
    void badInputNeverThrowsAndNeverMatches() {
        String hash = PasswordHasher.hash("Secret123");
        assertFalse(PasswordHasher.matches("x", "this-is-not-a-hash"));
        assertFalse(PasswordHasher.matches(null, hash));
        assertFalse(PasswordHasher.matches("x", null));
        assertFalse(PasswordHasher.matches("x", ""));
    }

    @Test
    void emptyPasswordCannotBeHashed() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc1", "abcdefgh", "12345678", " "})
    void weakPasswordsAreRejectedWithAMessage(String weak) {
        assertFalse(PasswordPolicy.problems(weak).isEmpty());
    }

    @Test
    void goodPasswordIsAccepted() {
        assertTrue(PasswordPolicy.problems("Abcdef12").isEmpty());
        assertTrue(PasswordPolicy.problems("Test@1234").isEmpty());
    }

    @Test
    void passwordsLongerThanBcryptCanReadAreRejected() {
        assertFalse(PasswordPolicy.problems("a1".repeat(40)).isEmpty());
        assertTrue(PasswordPolicy.problems("a1".repeat(36)).isEmpty(), "exactly 72 characters is fine");
    }

    @Test
    void nullAndEmptyAskForAPassword() {
        assertEquals(1, PasswordPolicy.problems(null).size());
        assertEquals(1, PasswordPolicy.problems("").size());
    }
}
