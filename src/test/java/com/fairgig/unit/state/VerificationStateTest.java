package com.fairgig.unit.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationStatus;
import com.fairgig.state.verification.VerificationState;
import com.fairgig.state.verification.VerificationStates;
import com.fairgig.util.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** FR-20 to FR-22 and NFR-13: the verification workflow and its allowed transitions. */
class VerificationStateTest {

    private static VerificationState state(VerificationStatus status) {
        return VerificationStates.of(status);
    }

    @ParameterizedTest
    @EnumSource(VerificationStatus.class)
    @DisplayName("every status has a state that reports the same status")
    void lookupMatchesStatus(VerificationStatus status) {
        assertEquals(status, state(status).status());
    }

    @Nested
    class FromPending {

        @Test
        void verifiedNeedsNoExplanation() {
            assertEquals(VerificationStatus.VERIFIED,
                state(VerificationStatus.PENDING).review(ReviewDecision.VERIFIED, null).status());
        }

        @Test
        void unverifiableNeedsNoExplanation() {
            assertEquals(VerificationStatus.UNVERIFIABLE,
                state(VerificationStatus.PENDING).review(ReviewDecision.UNVERIFIABLE, "").status());
        }

        @Test
        void flaggingWithExplanationWorks() {
            assertEquals(VerificationStatus.DISPUTED_FLAGGED,
                state(VerificationStatus.PENDING)
                    .review(ReviewDecision.DISPUTED_FLAGGED, "Amount differs from the screenshot").status());
        }

        @Test
        void flaggingWithoutExplanationIsRejected() {
            VerificationState pending = state(VerificationStatus.PENDING);
            assertThrows(ValidationException.class, () -> pending.review(ReviewDecision.DISPUTED_FLAGGED, null));
            assertThrows(ValidationException.class, () -> pending.review(ReviewDecision.DISPUTED_FLAGGED, ""));
            assertThrows(ValidationException.class, () -> pending.review(ReviewDecision.DISPUTED_FLAGGED, "   "));
        }

        @Test
        void tooLongExplanationIsRejected() {
            String longText = "x".repeat(501);
            assertThrows(ValidationException.class,
                () -> state(VerificationStatus.PENDING).review(ReviewDecision.DISPUTED_FLAGGED, longText));
        }

        @Test
        void explanationOfExactlyTheLimitIsAccepted() {
            String limit = "x".repeat(500);
            assertEquals(VerificationStatus.DISPUTED_FLAGGED,
                state(VerificationStatus.PENDING).review(ReviewDecision.DISPUTED_FLAGGED, limit).status());
        }

        @Test
        void missingDecisionIsRejected() {
            assertThrows(ValidationException.class, () -> state(VerificationStatus.PENDING).review(null, "x"));
        }

        @Test
        void resubmitIsRejectedBecauseItIsAlreadyPending() {
            assertThrows(ValidationException.class, () -> state(VerificationStatus.PENDING).resubmit());
        }
    }

    @Nested
    class FromVerified {

        @ParameterizedTest
        @EnumSource(ReviewDecision.class)
        @DisplayName("Verified accepts no new decision (FR-22)")
        void noDecisionIsAccepted(ReviewDecision decision) {
            assertThrows(ValidationException.class,
                () -> state(VerificationStatus.VERIFIED).review(decision, "some reason"));
        }

        @Test
        @DisplayName("Verified can never return to Pending (FR-22)")
        void neverReturnsToPending() {
            ValidationException error = assertThrows(ValidationException.class,
                () -> state(VerificationStatus.VERIFIED).resubmit());
            assertTrue(error.getMessage().contains("verified"));
        }
    }

    @Nested
    class FromFlaggedOrUnverifiable {

        @ParameterizedTest
        @EnumSource(value = VerificationStatus.class, names = {"DISPUTED_FLAGGED", "UNVERIFIABLE"})
        @DisplayName("new evidence sends the earning back to Pending (FR-17)")
        void resubmitReturnsToPending(VerificationStatus status) {
            assertEquals(VerificationStatus.PENDING, state(status).resubmit().status());
        }

        @ParameterizedTest
        @EnumSource(value = VerificationStatus.class, names = {"DISPUTED_FLAGGED", "UNVERIFIABLE"})
        @DisplayName("no direct re-decision without new evidence")
        void directDecisionIsRejected(VerificationStatus status) {
            for (ReviewDecision decision : ReviewDecision.values()) {
                assertThrows(ValidationException.class, () -> state(status).review(decision, "reason"));
            }
        }
    }

    @Test
    @DisplayName("a full round trip: Pending -> Flagged -> Pending -> Verified, then it is final")
    void fullRoundTrip() {
        VerificationState current = state(VerificationStatus.PENDING);
        current = current.review(ReviewDecision.DISPUTED_FLAGGED, "Hours do not match");
        assertEquals(VerificationStatus.DISPUTED_FLAGGED, current.status());
        current = current.resubmit();
        assertEquals(VerificationStatus.PENDING, current.status());
        current = current.review(ReviewDecision.VERIFIED, null);
        assertEquals(VerificationStatus.VERIFIED, current.status());
        VerificationState verified = current;
        assertThrows(ValidationException.class, verified::resubmit);
    }

    @Test
    void transitionsReuseTheSharedStateInstances() {
        VerificationState pending = state(VerificationStatus.PENDING);
        assertSame(pending, state(VerificationStatus.DISPUTED_FLAGGED).resubmit());
        assertSame(pending, state(VerificationStatus.UNVERIFIABLE).resubmit());
        assertSame(state(VerificationStatus.VERIFIED), pending.review(ReviewDecision.VERIFIED, null));
    }

    @Test
    void decisionsMapToTheMatchingStatus() {
        for (ReviewDecision decision : ReviewDecision.values()) {
            assertEquals(decision.name(), decision.resultingStatus().name());
        }
    }
}
