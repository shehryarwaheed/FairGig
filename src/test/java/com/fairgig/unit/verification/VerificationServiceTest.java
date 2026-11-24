package com.fairgig.unit.verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.Evidence;
import com.fairgig.model.FileType;
import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationReview;
import com.fairgig.model.VerificationStatus;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.security.Session;
import com.fairgig.service.verification.DefaultVerificationService;
import com.fairgig.service.verification.ReviewCase;
import com.fairgig.service.verification.ReviewEntry;
import com.fairgig.service.verification.VerificationService;
import com.fairgig.unit.support.Fakes;
import com.fairgig.unit.support.Fakes.FakeEarnings;
import com.fairgig.unit.support.Fakes.FakeEvidence;
import com.fairgig.unit.support.Fakes.FakePlatforms;
import com.fairgig.unit.support.Fakes.FakeReviews;
import com.fairgig.unit.support.Fakes.FakeUsers;
import com.fairgig.unit.support.Fakes.RecordingSender;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FR-18 to FR-21, FR-41 and the proxy rules of the verification service (NFR-03, NFR-13). */
class VerificationServiceTest {

    private static final int UBER = 1;
    private static final int FOODPANDA = 2;
    private static final int UBER_REP = 10;
    private static final int WORKER = 20;

    private FakeEarnings earnings;
    private FakeReviews reviews;
    private FakeEvidence evidence;
    private RecordingSender sender;
    private VerificationService service;          // the plain service
    private VerificationService guarded;          // wrapped by the Proxy

    @BeforeEach
    void setUp() {
        earnings = new FakeEarnings().with(
            Fakes.earning(1, WORKER, UBER, VerificationStatus.PENDING),
            Fakes.earning(2, WORKER, UBER, VerificationStatus.PENDING),
            Fakes.earning(3, WORKER, UBER, VerificationStatus.VERIFIED),
            Fakes.earning(4, WORKER, UBER, VerificationStatus.DISPUTED_FLAGGED),
            Fakes.earning(5, WORKER, FOODPANDA, VerificationStatus.PENDING));
        reviews = new FakeReviews(earnings);
        evidence = new FakeEvidence();
        sender = new RecordingSender();
        FakeUsers users = new FakeUsers().with(
            Fakes.worker(WORKER, "Ali Raza"),
            Fakes.representative(UBER_REP, "Hina Malik", UBER));
        FakePlatforms platforms = new FakePlatforms().with(UBER, "Uber").with(FOODPANDA, "Foodpanda");

        // no database: the "transaction" just runs the work
        service = new DefaultVerificationService(earnings, reviews, evidence, users, platforms, sender,
            noDatabase());
        guarded = AccessProxy.wrap(VerificationService.class, service);

        Session.start(Fakes.representative(UBER_REP, "Hina Malik", UBER));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    /** A "transaction" that just runs the work, so no database is needed. */
    private static DefaultVerificationService.Transactions noDatabase() {
        return new DefaultVerificationService.Transactions() {
            @Override
            public <T> T run(com.fairgig.database.TransactionManager.Work<T> work) throws SQLException {
                return work.execute(null);
            }
        };
    }

    private void addEvidence(int recordId) {
        Evidence e = new Evidence();
        e.setRecordId(recordId);
        e.setFilePath("uploads/" + recordId + ".png");
        e.setFileType(FileType.PNG);
        evidence.save(e);
    }

    // ---------------------------------------------------------------- FR-18, FR-19

    @Test
    @DisplayName("the queue shows only PENDING earnings of the representative's platform (FR-18)")
    void pendingListIsScopedToPlatformAndStatus() {
        List<ReviewCase> cases = guarded.pendingEarnings(UBER);
        assertEquals(List.of(1, 2), cases.stream().map(c -> c.record().getRecordId()).toList());
        assertEquals("Ali Raza", cases.get(0).workerName());
        assertEquals("Uber", cases.get(0).platformName());
    }

    @Test
    @DisplayName("an earning is shown together with its evidence (FR-19)")
    void openReviewIncludesEvidence() {
        addEvidence(1);
        addEvidence(1);
        ReviewCase review = guarded.openReview(1);
        assertEquals(2, review.evidence().size());
        assertEquals(1, review.record().getRecordId());
    }

    @Test
    void openingAnUnknownEarningGivesAClearError() {
        assertThrows(NotFoundException.class, () -> guarded.openReview(999));
    }

    // ---------------------------------------------------------------- FR-20, FR-21, FR-41

    @Test
    @DisplayName("Verified: status changes, decision is stored with reviewer, worker is notified")
    void verifyStoresDecisionAndNotifies() {
        VerificationReview review = guarded.review(UBER_REP, 1, ReviewDecision.VERIFIED, null);

        assertEquals(VerificationStatus.VERIFIED, earnings.records.get(1).getStatus());
        assertEquals(1, reviews.saved.size());
        assertEquals(UBER_REP, review.getRepresentativeId());
        assertEquals(ReviewDecision.VERIFIED, review.getDecision());
        assertNull(review.getExplanation());
        assertTrue(review.getReviewedAt() != null);

        assertEquals(1, sender.sent.size());
        assertEquals(WORKER, sender.sent.get(0).userId());
        assertTrue(sender.sent.get(0).message().contains("Verified"));
        assertTrue(sender.sent.get(0).message().contains("Uber"));
    }

    @Test
    @DisplayName("Flagged with an explanation is stored (FR-21)")
    void flagWithExplanation() {
        VerificationReview review = guarded.review(UBER_REP, 2, ReviewDecision.DISPUTED_FLAGGED, "  Hours do not match  ");
        assertEquals(VerificationStatus.DISPUTED_FLAGGED, earnings.records.get(2).getStatus());
        assertEquals("Hours do not match", review.getExplanation());
    }

    @Test
    @DisplayName("Flagged without an explanation changes nothing (FR-21)")
    void flagWithoutExplanationIsRejected() {
        assertThrows(ValidationException.class,
            () -> guarded.review(UBER_REP, 1, ReviewDecision.DISPUTED_FLAGGED, "   "));
        assertEquals(VerificationStatus.PENDING, earnings.records.get(1).getStatus());
        assertTrue(reviews.saved.isEmpty());
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    void unverifiableIsStored() {
        guarded.review(UBER_REP, 1, ReviewDecision.UNVERIFIABLE, null);
        assertEquals(VerificationStatus.UNVERIFIABLE, earnings.records.get(1).getStatus());
    }

    // ---------------------------------------------------------------- FR-22

    @Test
    @DisplayName("a Verified earning can not be reviewed again (FR-22)")
    void verifiedIsFinal() {
        assertThrows(ValidationException.class,
            () -> guarded.review(UBER_REP, 3, ReviewDecision.UNVERIFIABLE, "changed my mind"));
        assertEquals(VerificationStatus.VERIFIED, earnings.records.get(3).getStatus());
        assertTrue(reviews.saved.isEmpty());
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    @DisplayName("a Flagged earning needs new evidence before the next decision (FR-22)")
    void flaggedCannotBeDecidedDirectly() {
        assertThrows(ValidationException.class,
            () -> guarded.review(UBER_REP, 4, ReviewDecision.VERIFIED, null));
        assertEquals(VerificationStatus.DISPUTED_FLAGGED, earnings.records.get(4).getStatus());
    }

    @Test
    @DisplayName("if someone else decided first, nothing is stored and nobody is notified (NFR-06)")
    void concurrentDecisionIsDetected() {
        // the service loads status PENDING, but the row changes before the update runs
        FakeEarnings racing = new FakeEarnings() {
            @Override
            public boolean updateStatus(java.sql.Connection c, int id, VerificationStatus expected,
                                        VerificationStatus next) {
                records.get(id).setStatus(VerificationStatus.VERIFIED);   // another reviewer won
                return super.updateStatus(c, id, expected, next);
            }
        }.with(Fakes.earning(1, WORKER, UBER, VerificationStatus.PENDING));
        VerificationService racingService = new DefaultVerificationService(racing, new FakeReviews(racing),
            evidence, new FakeUsers().with(Fakes.worker(WORKER, "Ali Raza")),
            new FakePlatforms().with(UBER, "Uber"), sender, noDatabase());

        ValidationException error = assertThrows(ValidationException.class,
            () -> racingService.review(UBER_REP, 1, ReviewDecision.VERIFIED, null));
        assertTrue(error.getMessage().contains("Another reviewer"));
        assertTrue(sender.sent.isEmpty());
    }

    // ---------------------------------------------------------------- history

    @Test
    @DisplayName("history lists the platform's decisions, newest first")
    void historyIsNewestFirst() {
        guarded.review(UBER_REP, 1, ReviewDecision.VERIFIED, null);
        guarded.review(UBER_REP, 2, ReviewDecision.UNVERIFIABLE, null);

        List<ReviewEntry> history = guarded.reviewHistory(UBER);
        assertEquals(List.of(2, 1), history.stream().map(h -> h.record().getRecordId()).toList());
        assertEquals("Ali Raza", history.get(0).workerName());
    }

    // ---------------------------------------------------------------- Proxy rules (FR-05, FR-06, NFR-03)

    @Test
    @DisplayName("a representative cannot see another platform's queue (FR-06)")
    void otherPlatformQueueIsDenied() {
        assertThrows(AccessDeniedException.class, () -> guarded.pendingEarnings(FOODPANDA));
        assertThrows(AccessDeniedException.class, () -> guarded.reviewHistory(FOODPANDA));
    }

    @Test
    @DisplayName("a representative cannot open or decide another platform's earning (FR-06)")
    void otherPlatformEarningIsDenied() {
        assertThrows(AccessDeniedException.class, () -> guarded.openReview(5));
        assertThrows(AccessDeniedException.class,
            () -> guarded.review(UBER_REP, 5, ReviewDecision.VERIFIED, null));
        assertEquals(VerificationStatus.PENDING, earnings.records.get(5).getStatus());
    }

    @Test
    @DisplayName("a representative cannot decide in someone else's name")
    void reviewerIdMustBeOwn() {
        assertThrows(AccessDeniedException.class,
            () -> guarded.review(999, 1, ReviewDecision.VERIFIED, null));
    }

    @Test
    @DisplayName("workers are not allowed to use the verification service (FR-05)")
    void workersAreDenied() {
        Session.start(Fakes.worker(WORKER, "Ali Raza"));
        assertThrows(AccessDeniedException.class, () -> guarded.pendingEarnings(UBER));
        assertThrows(AccessDeniedException.class,
            () -> guarded.review(WORKER, 1, ReviewDecision.VERIFIED, null));
    }

    @Test
    void loggedOutUsersAreDenied() {
        Session.clear();
        assertThrows(AccessDeniedException.class, () -> guarded.pendingEarnings(UBER));
    }
}
