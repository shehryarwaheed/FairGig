package com.fairgig.unit.complaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.ValidationException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** FR-30 and FR-31: the worker files and tracks complaints. */
class WorkerComplaintServiceTest extends ComplaintTestBase {

    @Nested
    class Filing {

        @Test
        @DisplayName("a worker files a complaint: SUBMITTED, trimmed, and the platform's representatives are told")
        void fileValidComplaint() {
            Complaint c = worker.fileComplaint(ALI, UBER, ComplaintCategory.MISSING_PAYMENT,
                "  My payout is missing 800.  ", null);

            assertEquals(ComplaintStatus.SUBMITTED, c.getStatus());
            assertEquals("My payout is missing 800.", c.getDescription());
            assertEquals(ALI, c.getWorkerId());
            assertNull(c.getRecordId());
            assertEquals(1, complaints.items.size());

            assertEquals(1, sender.sentToRepresentatives.size());
            assertEquals(UBER, sender.sentToRepresentatives.get(0).platformId());
            assertTrue(sender.sentToRepresentatives.get(0).message().contains("Ali Raza"));
            assertTrue(sender.sentToRepresentatives.get(0).message().contains("Uber"));
        }

        @Test
        @DisplayName("a complaint can be linked to one of the worker's own earnings of that platform")
        void linkToOwnEarning() {
            Complaint c = worker.fileComplaint(ALI, UBER, ComplaintCategory.UNFAIR_DEDUCTION, "Too much taken", 1);
            assertEquals(1, c.getRecordId());
        }

        @Test
        void earningOfAnotherWorkerIsRefused() {
            assertThrows(ValidationException.class,
                () -> worker.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "Not mine", 3));
            assertTrue(complaints.items.isEmpty());
        }

        @Test
        void earningOfAnotherPlatformIsRefused() {
            ValidationException error = assertThrows(ValidationException.class,
                () -> worker.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "Wrong platform", 2));
            assertTrue(error.getMessage().contains("different platform"));
        }

        @Test
        void unknownEarningIsRefused() {
            assertThrows(ValidationException.class,
                () -> worker.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "Missing", 999));
        }

        @Test
        @DisplayName("a worker can only complain about a platform they are registered on")
        void mustBeRegistered() {
            ValidationException error = assertThrows(ValidationException.class,
                () -> worker.fileComplaint(ALI, FOODPANDA, ComplaintCategory.OTHER, "Not my platform", null));
            assertTrue(error.getMessage().contains("not registered on Foodpanda"));
            assertTrue(sender.sentToRepresentatives.isEmpty());
        }

        @Test
        void unknownPlatformIsRefused() {
            assertThrows(ValidationException.class,
                () -> worker.fileComplaint(ALI, 999, ComplaintCategory.OTHER, "Nowhere", null));
        }

        @Test
        @DisplayName("every problem is listed at once")
        void allProblemsAtOnce() {
            ValidationException error = assertThrows(ValidationException.class,
                () -> worker.fileComplaint(ALI, UBER, null, "   ", null));
            assertEquals(2, error.getProblems().size());
        }

        @Test
        void tooLongDescriptionIsRefused() {
            assertThrows(ValidationException.class,
                () -> worker.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "x".repeat(1001), null));
            worker.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "x".repeat(1000), null);   // exactly the limit
        }
    }

    @Nested
    class Tracking {

        @Test
        @DisplayName("a worker sees only their own complaints, with status and answers")
        void ownComplaintsWithAnswers() {
            Complaint mine = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            seed(SARA, UBER, ComplaintStatus.SUBMITTED);
            logInAsRep(UBER_REP, UBER);
            rep.respond(UBER_REP, mine.getComplaintId(), "We are checking.");
            logInAsWorker(ALI);

            List<ComplaintView> views = worker.myComplaints(ALI);
            assertEquals(1, views.size());
            assertEquals(ComplaintStatus.SUBMITTED, views.get(0).complaint().getStatus());
            assertEquals("Uber", views.get(0).platformName());
            assertEquals(1, views.get(0).responses().size());
            assertTrue(views.get(0).tags().isEmpty(), "advocate labels are not shown to workers");
            assertNull(views.get(0).issueTitle());
        }

        @Test
        void newestFirst() {
            Complaint first = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint second = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            List<ComplaintView> views = worker.myComplaints(ALI);
            assertEquals(List.of(second.getComplaintId(), first.getComplaintId()),
                views.stream().map(v -> v.complaint().getComplaintId()).toList());
        }

        @Test
        @DisplayName("a worker cannot read another worker's complaints")
        void otherWorkerIsDenied() {
            assertThrows(AccessDeniedException.class, () -> worker.myComplaints(SARA));
        }
    }
}
