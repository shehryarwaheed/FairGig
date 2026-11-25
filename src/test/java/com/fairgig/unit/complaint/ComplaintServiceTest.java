package com.fairgig.unit.complaint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.Advocate;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.ComplaintTag;
import com.fairgig.model.VerificationStatus;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.security.Session;
import com.fairgig.service.complaint.ComplaintService;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.DefaultComplaintService;
import com.fairgig.service.complaint.IssueSummary;
import com.fairgig.unit.support.Fakes;
import com.fairgig.unit.support.Fakes.FakeClusters;
import com.fairgig.unit.support.Fakes.FakeComplaints;
import com.fairgig.unit.support.Fakes.FakeEarnings;
import com.fairgig.unit.support.Fakes.FakePlatforms;
import com.fairgig.unit.support.Fakes.FakeTags;
import com.fairgig.unit.support.Fakes.FakeUsers;
import com.fairgig.unit.support.Fakes.FakeWorkerPlatforms;
import com.fairgig.unit.support.Fakes.RecordingSender;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.sql.Connection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** FR-30 to FR-33 and the notifications of FR-41, with the Proxy and State rules (NFR-03, NFR-13). */
class ComplaintServiceTest {

    private static final int UBER = 1;
    private static final int FOODPANDA = 2;
    private static final int ALI = 20;
    private static final int SARA = 21;
    private static final int UBER_REP = 10;
    private static final int FP_REP = 11;
    private static final int ADVOCATE = 30;

    private FakeComplaints complaints;
    private FakeClusters clusters;
    private FakeTags tags;
    private FakeEarnings earnings;
    private RecordingSender sender;
    private ComplaintService service;

    @BeforeEach
    void setUp() {
        complaints = new FakeComplaints();
        clusters = new FakeClusters();
        tags = new FakeTags();
        earnings = new FakeEarnings().with(
            Fakes.earning(1, ALI, UBER, VerificationStatus.PENDING),
            Fakes.earning(2, ALI, FOODPANDA, VerificationStatus.PENDING),
            Fakes.earning(3, SARA, UBER, VerificationStatus.PENDING));
        sender = new RecordingSender();
        service = wrap(complaints);
        logInAsWorker(ALI);
    }

    private ComplaintService wrap(FakeComplaints repository) {
        FakeUsers users = new FakeUsers().with(
            Fakes.worker(ALI, "Ali Raza"), Fakes.worker(SARA, "Sara Khan"),
            Fakes.representative(UBER_REP, "Hina Malik", UBER),
            Fakes.representative(FP_REP, "Omar Siddiqui", FOODPANDA), advocate());
        return AccessProxy.wrap(ComplaintService.class, new DefaultComplaintService(
            repository, clusters, tags, earnings,
            new FakePlatforms().with(UBER, "Uber").with(FOODPANDA, "Foodpanda"),
            new FakeWorkerPlatforms().with(ALI, UBER).with(SARA, UBER),
            users, sender, Fakes.noDatabase()));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    private static Advocate advocate() {
        Advocate a = new Advocate();
        a.setUserId(ADVOCATE);
        a.setName("Maryam Tariq");
        return a;
    }

    private void logInAsWorker(int id) {
        Session.start(Fakes.worker(id, id == ALI ? "Ali Raza" : "Sara Khan"));
    }

    private void logInAsRep(int id, int platform) {
        Session.start(Fakes.representative(id, "Rep", platform));
    }

    private void logInAsAdvocate() {
        Session.start(advocate());
    }

    /** Puts a complaint straight into the repository, with the wanted status. */
    private Complaint seed(int worker, int platform, ComplaintStatus status) {
        Complaint c = new Complaint();
        c.setWorkerId(worker);
        c.setPlatformId(platform);
        c.setCategory(ComplaintCategory.UNFAIR_DEDUCTION);
        c.setDescription("Seeded complaint");
        c.setStatus(status);
        return complaints.save(c);
    }

    // ================================================================ worker: filing (FR-30)

    @Nested
    class Filing {

        @Test
        @DisplayName("a worker files a complaint: SUBMITTED, trimmed, and the platform's representatives are told")
        void fileValidComplaint() {
            Complaint c = service.fileComplaint(ALI, UBER, ComplaintCategory.MISSING_PAYMENT,
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
            Complaint c = service.fileComplaint(ALI, UBER, ComplaintCategory.UNFAIR_DEDUCTION, "Too much taken", 1);
            assertEquals(1, c.getRecordId());
        }

        @Test
        void earningOfAnotherWorkerIsRefused() {
            assertThrows(ValidationException.class,
                () -> service.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "Not mine", 3));
            assertTrue(complaints.items.isEmpty());
        }

        @Test
        void earningOfAnotherPlatformIsRefused() {
            ValidationException error = assertThrows(ValidationException.class,
                () -> service.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "Wrong platform", 2));
            assertTrue(error.getMessage().contains("different platform"));
        }

        @Test
        void unknownEarningIsRefused() {
            assertThrows(ValidationException.class,
                () -> service.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "Missing", 999));
        }

        @Test
        @DisplayName("a worker can only complain about a platform they are registered on")
        void mustBeRegistered() {
            ValidationException error = assertThrows(ValidationException.class,
                () -> service.fileComplaint(ALI, FOODPANDA, ComplaintCategory.OTHER, "Not my platform", null));
            assertTrue(error.getMessage().contains("not registered on Foodpanda"));
            assertTrue(sender.sentToRepresentatives.isEmpty());
        }

        @Test
        void unknownPlatformIsRefused() {
            assertThrows(ValidationException.class,
                () -> service.fileComplaint(ALI, 999, ComplaintCategory.OTHER, "Nowhere", null));
        }

        @Test
        @DisplayName("every problem is listed at once")
        void allProblemsAtOnce() {
            ValidationException error = assertThrows(ValidationException.class,
                () -> service.fileComplaint(ALI, UBER, null, "   ", null));
            assertEquals(2, error.getProblems().size());
        }

        @Test
        void tooLongDescriptionIsRefused() {
            assertThrows(ValidationException.class,
                () -> service.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "x".repeat(1001), null));
            service.fileComplaint(ALI, UBER, ComplaintCategory.OTHER, "x".repeat(1000), null);   // exactly the limit
        }
    }

    // ================================================================ worker: tracking (FR-31)

    @Nested
    class Tracking {

        @Test
        @DisplayName("a worker sees only their own complaints, with status and answers")
        void ownComplaintsWithAnswers() {
            Complaint mine = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            seed(SARA, UBER, ComplaintStatus.SUBMITTED);
            logInAsRep(UBER_REP, UBER);
            service.respond(UBER_REP, mine.getComplaintId(), "We are checking.");
            logInAsWorker(ALI);

            List<ComplaintView> views = service.myComplaints(ALI);
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
            List<ComplaintView> views = service.myComplaints(ALI);
            assertEquals(List.of(second.getComplaintId(), first.getComplaintId()),
                views.stream().map(v -> v.complaint().getComplaintId()).toList());
        }

        @Test
        @DisplayName("a worker cannot read another worker's complaints")
        void otherWorkerIsDenied() {
            assertThrows(AccessDeniedException.class, () -> service.myComplaints(SARA));
        }
    }

    // ================================================================ representative (FR-32)

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
            assertEquals(2, service.platformComplaints(UBER).size());
            assertThrows(AccessDeniedException.class, () -> service.platformComplaints(FOODPANDA));
        }

        @Test
        @DisplayName("an answer is stored, the worker is told, and the status stays Submitted")
        void respond() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            ComplaintResponse response = service.respond(UBER_REP, c.getComplaintId(), "  We are looking into it.  ");

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
            assertThrows(ValidationException.class, () -> service.respond(UBER_REP, c.getComplaintId(), "  "));
            assertThrows(ValidationException.class, () -> service.respond(UBER_REP, c.getComplaintId(), "x".repeat(1001)));
            assertTrue(complaints.responses.isEmpty());
            assertTrue(sender.sent.isEmpty());
        }

        @Test
        void resolvedComplaintCannotBeAnswered() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.RESOLVED);
            assertThrows(ValidationException.class, () -> service.respond(UBER_REP, c.getComplaintId(), "Too late"));
        }

        @Test
        @DisplayName("resolving: status, who resolved it, and the worker is told")
        void resolve() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint resolved = service.resolve(UBER_REP, c.getComplaintId());

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
                () -> service.resolve(UBER_REP, c.getComplaintId()));
            assertTrue(error.getMessage().contains("advocate"));
            assertEquals(ComplaintStatus.ESCALATED, complaints.items.get(c.getComplaintId()).getStatus());
        }

        @Test
        void resolvedStaysResolved() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.RESOLVED);
            assertThrows(ValidationException.class, () -> service.resolve(UBER_REP, c.getComplaintId()));
            assertTrue(sender.sent.isEmpty());
        }

        @Test
        @DisplayName("a representative cannot answer or resolve another platform's complaint (FR-06)")
        void otherPlatformIsDenied() {
            Complaint other = seed(ALI, FOODPANDA, ComplaintStatus.SUBMITTED);
            assertThrows(AccessDeniedException.class, () -> service.respond(UBER_REP, other.getComplaintId(), "Hi"));
            assertThrows(AccessDeniedException.class, () -> service.resolve(UBER_REP, other.getComplaintId()));
            assertEquals(ComplaintStatus.SUBMITTED, complaints.items.get(other.getComplaintId()).getStatus());
        }

        @Test
        void cannotActInSomeoneElsesName() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            assertThrows(AccessDeniedException.class, () -> service.respond(FP_REP, c.getComplaintId(), "Hi"));
            assertThrows(AccessDeniedException.class, () -> service.resolve(FP_REP, c.getComplaintId()));
        }

        @Test
        void unknownComplaintGivesAClearError() {
            assertThrows(NotFoundException.class, () -> service.respond(UBER_REP, 999, "Hi"));
        }

        private void assertNotNullTime(Complaint c) {
            assertTrue(c.getResolvedAt() != null);
        }
    }

    // ================================================================ advocate (FR-33)

    @Nested
    class AdvocateActions {

        @BeforeEach
        void asAdvocate() {
            logInAsAdvocate();
        }

        @Test
        @DisplayName("an advocate sees every complaint, with tags and the common issue")
        void viewAll() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            seed(SARA, UBER, ComplaintStatus.SUBMITTED);
            seed(ALI, UBER, ComplaintStatus.RESOLVED);
            service.tag(ADVOCATE, a.getComplaintId(), "commission");
            service.createIssue(ADVOCATE, List.of(a.getComplaintId()), "Sudden increase", null);

            List<ComplaintView> all = service.allComplaints();
            assertEquals(3, all.size());
            ComplaintView tagged = all.stream().filter(v -> v.complaint().getComplaintId().equals(a.getComplaintId()))
                .findFirst().orElseThrow();
            assertEquals(List.of("commission"), tagged.tags().stream().map(ComplaintTag::getName).toList());
            assertEquals("Sudden increase", tagged.issueTitle());
        }

        @Test
        @DisplayName("escalating: status, advocate recorded, worker told")
        void escalate() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint escalated = service.escalate(ADVOCATE, c.getComplaintId());

            assertEquals(ComplaintStatus.ESCALATED, escalated.getStatus());
            assertEquals(ADVOCATE, escalated.getHandledByAdvocateId());
            assertTrue(escalated.getEscalatedAt() != null);
            assertEquals(1, sender.sent.size());
            assertEquals(ALI, sender.sent.get(0).userId());
            assertTrue(sender.sent.get(0).message().contains("escalated"));
        }

        @Test
        void cannotEscalateTwiceOrAfterResolving() {
            Complaint escalated = seed(ALI, UBER, ComplaintStatus.ESCALATED);
            Complaint resolved = seed(ALI, UBER, ComplaintStatus.RESOLVED);
            assertThrows(ValidationException.class, () -> service.escalate(ADVOCATE, escalated.getComplaintId()));
            assertThrows(ValidationException.class, () -> service.escalate(ADVOCATE, resolved.getComplaintId()));
            assertTrue(sender.sent.isEmpty());
        }

        @Test
        @DisplayName("an advocate can resolve a Submitted or an Escalated complaint, but not a resolved one")
        void resolveAsAdvocate() {
            Complaint submitted = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint escalated = seed(ALI, UBER, ComplaintStatus.ESCALATED);
            Complaint resolved = seed(ALI, UBER, ComplaintStatus.RESOLVED);

            assertEquals(ComplaintStatus.RESOLVED, service.resolveAsAdvocate(ADVOCATE, submitted.getComplaintId()).getStatus());
            assertEquals(ComplaintStatus.RESOLVED, service.resolveAsAdvocate(ADVOCATE, escalated.getComplaintId()).getStatus());
            assertThrows(ValidationException.class, () -> service.resolveAsAdvocate(ADVOCATE, resolved.getComplaintId()));
            assertEquals(2, sender.sent.size());
            assertEquals(ADVOCATE, complaints.items.get(submitted.getComplaintId()).getHandledByAdvocateId());
        }

        @Test
        @DisplayName("if someone else changed the complaint first, nothing is saved and nobody is told (NFR-06)")
        void concurrentChangeIsDetected() {
            FakeComplaints racing = new FakeComplaints() {
                @Override
                public boolean markEscalated(Connection c, int id, ComplaintStatus expected, int advocateId) {
                    items.get(id).setStatus(ComplaintStatus.RESOLVED);          // a representative got there first
                    return super.markEscalated(c, id, expected, advocateId);
                }
            };
            Complaint c = new Complaint();
            c.setWorkerId(ALI);
            c.setPlatformId(UBER);
            c.setCategory(ComplaintCategory.OTHER);
            c.setDescription("Racing");
            racing.save(c);

            ValidationException error = assertThrows(ValidationException.class,
                () -> wrap(racing).escalate(ADVOCATE, c.getComplaintId()));
            assertTrue(error.getMessage().contains("Someone else"));
            assertTrue(sender.sent.isEmpty());
        }

        // ---- tags

        @Test
        @DisplayName("tags are normalised, created once and reused")
        void tagging() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint d = seed(SARA, UBER, ComplaintStatus.SUBMITTED);

            ComplaintTag first = service.tag(ADVOCATE, c.getComplaintId(), "  Payment Delay ");
            assertEquals("payment-delay", first.getName());
            ComplaintTag again = service.tag(ADVOCATE, d.getComplaintId(), "PAYMENT-DELAY");
            assertEquals(first.getTagId(), again.getTagId());
            assertEquals(1, tags.items.size());

            service.tag(ADVOCATE, c.getComplaintId(), "payment-delay");           // twice on the same complaint
            assertEquals(1, tags.findByComplaint(c.getComplaintId()).size());
        }

        @Test
        void badTagNamesAreRefused() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            assertThrows(ValidationException.class, () -> service.tag(ADVOCATE, c.getComplaintId(), "   "));
            assertThrows(ValidationException.class, () -> service.tag(ADVOCATE, c.getComplaintId(), "bad!tag"));
            assertThrows(ValidationException.class, () -> service.tag(ADVOCATE, c.getComplaintId(), "x".repeat(51)));
            assertTrue(tags.items.isEmpty());
        }

        @Test
        void removingTags() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            ComplaintTag tag = service.tag(ADVOCATE, c.getComplaintId(), "commission");
            service.removeTag(ADVOCATE, c.getComplaintId(), tag.getTagId());
            assertTrue(tags.findByComplaint(c.getComplaintId()).isEmpty());
            assertThrows(ValidationException.class,
                () -> service.removeTag(ADVOCATE, c.getComplaintId(), tag.getTagId()));
            assertEquals(1, service.allTags().size());
        }

        @Test
        void taggingAnUnknownComplaintGivesAClearError() {
            assertThrows(NotFoundException.class, () -> service.tag(ADVOCATE, 999, "commission"));
        }

        // ---- common issues

        @Test
        @DisplayName("several complaints are grouped under one new common issue")
        void groupIntoIssue() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint b = seed(SARA, UBER, ComplaintStatus.SUBMITTED);
            Complaint other = seed(ALI, UBER, ComplaintStatus.SUBMITTED);

            ComplaintCluster issue = service.createIssue(ADVOCATE,
                List.of(a.getComplaintId(), b.getComplaintId(), a.getComplaintId()),   // a duplicate id is harmless
                "  Sudden commission increase ", "  Since mid-October ");

            assertEquals("Sudden commission increase", issue.getTitle());
            assertEquals("Since mid-October", issue.getDescription());
            assertEquals(ADVOCATE, issue.getAdvocateId());
            assertEquals(issue.getClusterId(), complaints.items.get(a.getComplaintId()).getClusterId());
            assertEquals(issue.getClusterId(), complaints.items.get(b.getComplaintId()).getClusterId());
            assertNull(complaints.items.get(other.getComplaintId()).getClusterId());

            List<IssueSummary> issues = service.issues();
            assertEquals(1, issues.size());
            assertEquals(2, issues.get(0).complaintCount());
        }

        @Test
        void issueNeedsATitleAndAComplaint() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            ValidationException error = assertThrows(ValidationException.class,
                () -> service.createIssue(ADVOCATE, List.of(), " ", null));
            assertEquals(2, error.getProblems().size());
            assertThrows(ValidationException.class,
                () -> service.createIssue(ADVOCATE, List.of(a.getComplaintId()), "x".repeat(151), null));
            assertTrue(clusters.items.isEmpty());
        }

        @Test
        @DisplayName("an unknown complaint in the list stops the whole grouping: nothing is created")
        void unknownComplaintStopsGrouping() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            assertThrows(NotFoundException.class,
                () -> service.createIssue(ADVOCATE, List.of(a.getComplaintId(), 999), "Issue", null));
            assertTrue(clusters.items.isEmpty());
            assertNull(complaints.items.get(a.getComplaintId()).getClusterId());
        }

        @Test
        void moveIntoAndOutOfAnIssue() {
            Complaint a = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            Complaint b = seed(SARA, UBER, ComplaintStatus.SUBMITTED);
            ComplaintCluster issue = service.createIssue(ADVOCATE, List.of(a.getComplaintId()), "Issue", null);

            service.assignToIssue(ADVOCATE, b.getComplaintId(), issue.getClusterId());
            assertEquals(2, service.issues().get(0).complaintCount());

            service.assignToIssue(ADVOCATE, b.getComplaintId(), null);
            assertEquals(1, service.issues().get(0).complaintCount());

            assertThrows(NotFoundException.class, () -> service.assignToIssue(ADVOCATE, b.getComplaintId(), 999));
            assertThrows(NotFoundException.class, () -> service.assignToIssue(ADVOCATE, 999, issue.getClusterId()));
        }
    }

    // ================================================================ Proxy rules (FR-05, NFR-03)

    @Nested
    class Roles {

        @Test
        @DisplayName("workers cannot use the representative or advocate actions")
        void workerIsLimited() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            assertThrows(AccessDeniedException.class, () -> service.platformComplaints(UBER));
            assertThrows(AccessDeniedException.class, () -> service.respond(ALI, c.getComplaintId(), "Hi"));
            assertThrows(AccessDeniedException.class, () -> service.resolve(ALI, c.getComplaintId()));
            assertThrows(AccessDeniedException.class, () -> service.allComplaints());
            assertThrows(AccessDeniedException.class, () -> service.escalate(ALI, c.getComplaintId()));
            assertThrows(AccessDeniedException.class, () -> service.tag(ALI, c.getComplaintId(), "x"));
            assertThrows(AccessDeniedException.class, () -> service.issues());
        }

        @Test
        @DisplayName("representatives cannot file complaints or use the advocate actions")
        void representativeIsLimited() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            logInAsRep(UBER_REP, UBER);
            assertThrows(AccessDeniedException.class,
                () -> service.fileComplaint(UBER_REP, UBER, ComplaintCategory.OTHER, "x", null));
            assertThrows(AccessDeniedException.class, () -> service.escalate(UBER_REP, c.getComplaintId()));
            assertThrows(AccessDeniedException.class, () -> service.resolveAsAdvocate(UBER_REP, c.getComplaintId()));
            assertThrows(AccessDeniedException.class, () -> service.allComplaints());
            assertEquals(ComplaintStatus.SUBMITTED, complaints.items.get(c.getComplaintId()).getStatus());
        }

        @Test
        @DisplayName("advocates cannot answer or file, and cannot act in someone else's name")
        void advocateIsLimited() {
            Complaint c = seed(ALI, UBER, ComplaintStatus.SUBMITTED);
            logInAsAdvocate();
            assertThrows(AccessDeniedException.class, () -> service.respond(ADVOCATE, c.getComplaintId(), "Hi"));
            assertThrows(AccessDeniedException.class,
                () -> service.fileComplaint(ADVOCATE, UBER, ComplaintCategory.OTHER, "x", null));
            assertThrows(AccessDeniedException.class, () -> service.escalate(999, c.getComplaintId()));
            assertFalse(complaints.items.get(c.getComplaintId()).getStatus() == ComplaintStatus.ESCALATED);
        }

        @Test
        void loggedOutUsersAreDenied() {
            Session.clear();
            assertThrows(AccessDeniedException.class, () -> service.myComplaints(ALI));
            assertThrows(AccessDeniedException.class, () -> service.allComplaints());
        }
    }
}
