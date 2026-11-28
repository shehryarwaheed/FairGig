package com.fairgig.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.controller.Outcome;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** NFR-11: a failed operation never crashes a screen; the user always gets a clear message. */
class OutcomeTest {

    @Test
    void successCarriesTheValue() {
        Outcome<String> ok = Outcome.attempt(() -> "done");
        assertTrue(ok.isOk());
        assertEquals("done", ok.value());
        assertTrue(ok.problems().isEmpty());
    }

    @Test
    void validationProblemsAreKeptOneByOne() {
        Outcome<String> bad = Outcome.attempt(() -> {
            throw new ValidationException(List.of("First problem.", "Second problem."));
        });
        assertFalse(bad.isOk());
        assertNull(bad.value());
        assertEquals(List.of("First problem.", "Second problem."), bad.problems());
        assertEquals("First problem.\nSecond problem.", bad.message());
    }

    @Test
    void accessAndNotFoundMessagesAreShownAsTheyAre() {
        assertEquals("Please log in to continue.",
            Outcome.attempt(() -> { throw new AccessDeniedException("Please log in to continue."); }).message());
        assertEquals("Gone.", Outcome.attempt(() -> { throw new NotFoundException("Gone."); }).message());
    }

    @Test
    void databaseFailuresBecomeAFriendlyMessageWithoutTechnicalDetails() {
        Outcome<String> db = Outcome.attempt(() -> { throw new DataAccessException("SQL state 08006 secret details"); });
        assertFalse(db.isOk());
        assertTrue(db.message().contains("PostgreSQL is running"));
        assertFalse(db.message().contains("secret"));
    }

    @Test
    void anyUnexpectedErrorIsCaught() {
        Outcome<String> weird = Outcome.attempt(() -> { throw new IllegalStateException("surprise"); });
        assertFalse(weird.isOk());
        assertTrue(weird.message().contains("unexpected"));
    }

    @Test
    void runWorksForActionsWithoutAResult() {
        assertTrue(Outcome.run(() -> { }).isOk());
        assertFalse(Outcome.run(() -> { throw new ValidationException("No."); }).isOk());
    }
}
