package com.fairgig.unit.complaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** FR-32: the representative answers and resolves complaints of their own platform. */
class RepresentativeComplaintServiceTest extends ComplaintTestBase {

    @Nested
    class Representative {

        @BeforeEach
        void asUberRep() {
            logInAsRep(UBER_REP, UBER);
        }

        @Test
        @DisplayName("the list holds only the representative's own platform")
        void ownPlatformOnly() {
            seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            seed(SARA, UBER, ComplaintStatus.ESCALATED);
            assertEquals(2, rep.platformComplaints(UBER).size());
            assertThrows(AccessDeniedException.class, () -> rep.platformComplaints(FOODPANDA));
        }

        @Test
        @DisplayName("an answer is stored, the worker is told, and the status stays Submitted")
        void respond() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            ComplaintResponse response = rep.respond(UBER_REP, c.getComplaintId(), "  We are looking into it.  ");

            assertEquals("We are looking into it.", response.getMessage());
            assertEquals(UBER_REP, response.getRepresentativeId());
            assertEquals(ComplaintStatus.SUBMITTED, complaints.items.get(c.getComplaintId()).getStatus());
            assertEquals(1, sender.sent.size());
            assertEquals(ALI, sender.sent.get(0).userId());
            assertTrue(sender.sent.get(0).message().contains("new response"));
        }

        @Test
        void blankOrTooLongAnswerIsRefused() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            assertThrows(ValidationException.class, () -> rep.respond(UBER_REP, c.getComplaintId(), "  "));
            assertThrows(ValidationException.class, () -> rep.respond(UBER_REP, c.getComplaintId(), "x".repeat(1001)));
            assertTrue(complaints.responses.isEmpty());
            assertTrue(sender.sent.isEmpty());
        }

        @Test
        void resolvedComplaintCannotBeAnswered() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.RESOLVED);
            assertThrows(ValidationException.class, () -> rep.respond(UBER_REP, c.getComplaintId(), "Too late"));
        }

        @Test
        @DisplayName("resolving: status, who resolved it, and the worker is told")
        void resolve() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint resolved = rep.resolve(UBER_REP, c.getComplaintId());

            assertEquals(ComplaintStatus.RESOLVED, resolved.getStatus());
            assertEquals(UBER_REP, resolved.getResolvedByRepId());
            assertNotNullTime(resolved);
            assertEquals(1, sender.sent.size());
            assertEquals(ALI, sender.sent.get(0).userId());
            assertTrue(sender.sent.get(0).message().contains("resolved"));
        }

        @Test
        @DisplayName("an escalated complaint is resolved by an advocate, not by a representative")
        void escalatedIsNotForRepresentatives() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.ESCALATED);
            ValidationException error = assertThrows(ValidationException.class,
                () -> rep.resolve(UBER_REP, c.getComplaintId()));
            assertTrue(error.getMessage().contains("advocate"));
            assertEquals(ComplaintStatus.ESCALATED, complaints.items.get(c.getComplaintId()).getStatus());
        }

        @Test
        void resolvedStaysResolved() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.RESOLVED);
            assertThrows(ValidationException.class, () -> rep.resolve(UBER_REP, c.getComplaintId()));
            assertTrue(sender.sent.isEmpty());
        }

        @Test
        @DisplayName("a representative cannot answer or resolve another platform's complaint (FR-06)")
        void otherPlatformIsDenied() {
            Complaint other = seed(ALI, FOODPANDA, ComplaintStatus.SUBMITTED);
            assertThrows(AccessDeniedException.class, () -> rep.respond(UBER_REP, other.getComplaintId(), "Hi"));
            assertThrows(AccessDeniedException.class, () -> rep.resolve(UBER_REP, other.getComplaintId()));
            assertEquals(ComplaintStatus.SUBMITTED, complaints.items.get(other.getComplaintId()).getStatus());
        }

        @Test
        void cannotActInSomeoneElsesName() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            assertThrows(AccessDeniedException.class, () -> rep.respond(FP_REP, c.getComplaintId(), "Hi"));
            assertThrows(AccessDeniedException.class, () -> rep.resolve(FP_REP, c.getComplaintId()));
        }

        @Test
        void unknownComplaintGivesAClearError() {
            assertThrows(NotFoundException.class, () -> rep.respond(UBER_REP, 999, "Hi"));
        }

        private void assertNotNullTime(Complaint c) {
            assertTrue(c.getResolvedAt() != null);
        }
    }
}
