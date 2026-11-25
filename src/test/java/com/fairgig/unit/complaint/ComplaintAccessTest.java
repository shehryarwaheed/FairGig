package com.fairgig.unit.complaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.security.Session;
import com.fairgig.util.AccessDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** NFR-03: the Proxy keeps every role to its own complaint service. */
class ComplaintAccessTest extends ComplaintTestBase {

    @Nested
    class Roles {

        @Test
        @DisplayName("workers cannot use the representative or advocate actions")
        void workerIsLimited() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            assertThrows(AccessDeniedException.class, () -> rep.platformComplaints(UBER));
            assertThrows(AccessDeniedException.class, () -> rep.respond(ALI, c.getComplaintId(), "Hi"));
            assertThrows(AccessDeniedException.class, () -> rep.resolve(ALI, c.getComplaintId()));
            assertThrows(AccessDeniedException.class, () -> advocate.allComplaints());
            assertThrows(AccessDeniedException.class, () -> advocate.escalate(ALI, c.getComplaintId()));
            assertThrows(AccessDeniedException.class, () -> advocate.tag(ALI, c.getComplaintId(), "x"));
            assertThrows(AccessDeniedException.class, () -> advocate.issues());
        }

        @Test
        @DisplayName("representatives cannot file complaints or use the advocate actions")
        void representativeIsLimited() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            logInAsRep(UBER_REP, UBER);
            assertThrows(AccessDeniedException.class,
                () -> worker.fileComplaint(UBER_REP, UBER, ComplaintCategory.OTHER, "x", null));
            assertThrows(AccessDeniedException.class, () -> advocate.escalate(UBER_REP, c.getComplaintId()));
            assertThrows(AccessDeniedException.class, () -> advocate.resolveAsAdvocate(UBER_REP, c.getComplaintId()));
            assertThrows(AccessDeniedException.class, () -> advocate.allComplaints());
            assertEquals(ComplaintStatus.SUBMITTED, complaints.items.get(c.getComplaintId()).getStatus());
        }

        @Test
        @DisplayName("advocates cannot answer or file, and cannot act in someone else's name")
        void advocateIsLimited() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            logInAsAdvocate();
            assertThrows(AccessDeniedException.class, () -> rep.respond(ADVOCATE, c.getComplaintId(), "Hi"));
            assertThrows(AccessDeniedException.class,
                () -> worker.fileComplaint(ADVOCATE, UBER, ComplaintCategory.OTHER, "x", null));
            assertThrows(AccessDeniedException.class, () -> advocate.escalate(999, c.getComplaintId()));
            assertFalse(complaints.items.get(c.getComplaintId()).getStatus() == ComplaintStatus.ESCALATED);
        }

        @Test
        void loggedOutUsersAreDenied() {
            Session.clear();
            assertThrows(AccessDeniedException.class, () -> worker.myComplaints(ALI));
            assertThrows(AccessDeniedException.class, () -> rep.platformComplaints(UBER));
            assertThrows(AccessDeniedException.class, () -> advocate.allComplaints());
        }
    }
}
