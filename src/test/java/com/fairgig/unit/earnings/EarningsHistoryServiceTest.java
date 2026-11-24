package com.fairgig.unit.earnings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationReview;
import com.fairgig.model.VerificationStatus;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.security.Session;
import com.fairgig.service.earnings.DefaultEarningsHistoryService;
import com.fairgig.service.earnings.EarningStatusView;
import com.fairgig.service.earnings.EarningsHistoryService;
import com.fairgig.unit.support.Fakes;
import com.fairgig.unit.support.Fakes.FakeEarnings;
import com.fairgig.unit.support.Fakes.FakePlatforms;
import com.fairgig.unit.support.Fakes.FakeReviews;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.ValidationException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FR-15 (status of each earning) and FR-16 (history with filters). */
class EarningsHistoryServiceTest {

    private static final int ALI = 20;
    private static final int SARA = 21;
    private static final int UBER = 1;
    private static final int FOODPANDA = 2;

    private FakeEarnings earnings;
    private FakeReviews reviews;
    private EarningsHistoryService service;

    private static EarningsRecord earning(int id, int worker, int platform, VerificationStatus status, LocalDate date) {
        EarningsRecord e = Fakes.earning(id, worker, platform, status);
        e.setShiftDate(date);
        return e;
    }

    @BeforeEach
    void setUp() {
        earnings = new FakeEarnings().with(
            earning(1, ALI, UBER, VerificationStatus.PENDING, LocalDate.of(2026, 11, 20)),
            earning(2, ALI, UBER, VerificationStatus.VERIFIED, LocalDate.of(2026, 11, 10)),
            earning(3, ALI, FOODPANDA, VerificationStatus.DISPUTED_FLAGGED, LocalDate.of(2026, 10, 15)),
            earning(4, ALI, FOODPANDA, VerificationStatus.UNVERIFIABLE, LocalDate.of(2026, 9, 1)),
            earning(5, SARA, UBER, VerificationStatus.PENDING, LocalDate.of(2026, 11, 21)));
        reviews = new FakeReviews(earnings);
        FakePlatforms platforms = new FakePlatforms().with(UBER, "Uber").with(FOODPANDA, "Foodpanda");

        service = AccessProxy.wrap(EarningsHistoryService.class,
            new DefaultEarningsHistoryService(earnings, reviews, platforms));
        Session.start(Fakes.worker(ALI, "Ali Raza"));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    private void review(int recordId, ReviewDecision decision, String explanation) {
        VerificationReview r = new VerificationReview();
        r.setRecordId(recordId);
        r.setRepresentativeId(10);
        r.setDecision(decision);
        r.setExplanation(explanation);
        reviews.save(r);
    }

    private static List<Integer> ids(List<EarningStatusView> rows) {
        return rows.stream().map(r -> r.record().getRecordId()).toList();
    }

    @Test
    @DisplayName("shows only the worker's own earnings, newest shift first, with platform names")
    void ownEarningsNewestFirst() {
        List<EarningStatusView> rows = service.myEarnings(ALI, EarningsFilter.none());
        assertEquals(List.of(1, 2, 3, 4), ids(rows));
        assertEquals("Uber", rows.get(0).platformName());
        assertEquals("Foodpanda", rows.get(2).platformName());
    }

    @Test
    @DisplayName("every earning carries its verification status (FR-15)")
    void statusIsVisible() {
        List<EarningStatusView> rows = service.myEarnings(ALI, EarningsFilter.none());
        assertEquals(VerificationStatus.PENDING, rows.get(0).record().getStatus());
        assertEquals(VerificationStatus.VERIFIED, rows.get(1).record().getStatus());
        assertEquals(VerificationStatus.DISPUTED_FLAGGED, rows.get(2).record().getStatus());
        assertEquals(VerificationStatus.UNVERIFIABLE, rows.get(3).record().getStatus());
    }

    @Test
    @DisplayName("a flagged earning shows the latest explanation, so the worker knows why")
    void latestReviewIsShown() {
        review(3, ReviewDecision.DISPUTED_FLAGGED, "Old reason");
        review(3, ReviewDecision.DISPUTED_FLAGGED, "Amount differs from the screenshot");

        EarningStatusView flagged = service.myEarnings(ALI, EarningsFilter.none()).get(2);
        assertEquals("Amount differs from the screenshot", flagged.latestReview().getExplanation());
    }

    @Test
    void pendingEarningsHaveNoReview() {
        assertNull(service.myEarnings(ALI, EarningsFilter.none()).get(0).latestReview());
    }

    @Test
    @DisplayName("only Flagged and Unverifiable earnings can get new evidence (FR-17)")
    void resubmitFlag() {
        List<EarningStatusView> rows = service.myEarnings(ALI, EarningsFilter.none());
        assertFalse(rows.get(0).canResubmitEvidence());   // pending
        assertFalse(rows.get(1).canResubmitEvidence());   // verified
        assertTrue(rows.get(2).canResubmitEvidence());    // flagged
        assertTrue(rows.get(3).canResubmitEvidence());    // unverifiable
    }

    // ---------------------------------------------------------------- filters (FR-16)

    @Test
    void filterByPlatform() {
        List<EarningStatusView> rows = service.myEarnings(ALI, new EarningsFilter(FOODPANDA, null, null, null));
        assertEquals(List.of(3, 4), ids(rows));
    }

    @Test
    void filterByStatus() {
        List<EarningStatusView> rows =
            service.myEarnings(ALI, new EarningsFilter(null, null, null, VerificationStatus.VERIFIED));
        assertEquals(List.of(2), ids(rows));
    }

    @Test
    void filterByDateRangeIncludesBothEnds() {
        List<EarningStatusView> rows = service.myEarnings(ALI,
            new EarningsFilter(null, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 10), null));
        assertEquals(List.of(2, 3), ids(rows));
    }

    @Test
    void filtersCanBeCombined() {
        List<EarningStatusView> rows = service.myEarnings(ALI,
            new EarningsFilter(UBER, LocalDate.of(2026, 11, 1), null, VerificationStatus.PENDING));
        assertEquals(List.of(1), ids(rows));
    }

    @Test
    void nothingMatchingGivesAnEmptyList() {
        assertTrue(service.myEarnings(ALI, new EarningsFilter(UBER, null, null, VerificationStatus.UNVERIFIABLE)).isEmpty());
    }

    @Test
    @DisplayName("a start date after the end date gives a clear message")
    void reversedDatesAreRejected() {
        ValidationException error = assertThrows(ValidationException.class, () -> service.myEarnings(ALI,
            new EarningsFilter(null, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 1), null)));
        assertTrue(error.getMessage().contains("start date"));
    }

    @Test
    void nullFilterMeansNoFilter() {
        assertEquals(4, service.myEarnings(ALI, null).size());
    }

    // ---------------------------------------------------------------- Proxy rules

    @Test
    @DisplayName("a worker cannot read another worker's history")
    void otherWorkersHistoryIsDenied() {
        assertThrows(AccessDeniedException.class, () -> service.myEarnings(SARA, EarningsFilter.none()));
    }

    @Test
    @DisplayName("representatives and logged-out users cannot use the worker history")
    void onlyWorkersAllowed() {
        Session.start(Fakes.representative(10, "Hina Malik", UBER));
        assertThrows(AccessDeniedException.class, () -> service.myEarnings(ALI, EarningsFilter.none()));
        Session.clear();
        assertThrows(AccessDeniedException.class, () -> service.myEarnings(ALI, EarningsFilter.none()));
    }
}
