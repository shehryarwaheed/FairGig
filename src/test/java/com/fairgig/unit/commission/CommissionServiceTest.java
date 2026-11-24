package com.fairgig.unit.commission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.CommissionRate;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.security.Session;
import com.fairgig.service.commission.CommissionService;
import com.fairgig.service.commission.DefaultCommissionService;
import com.fairgig.unit.support.Fakes;
import com.fairgig.unit.support.Fakes.FakeCommissionRates;
import com.fairgig.unit.support.Fakes.FakePlatforms;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FR-26 and FR-27: recording rates and showing the history with sudden changes. */
class CommissionServiceTest {

    private static final int UBER = 1;
    private static final int FOODPANDA = 2;
    private static final int UBER_REP = 10;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    private FakeCommissionRates rates;
    private CommissionService service;

    @BeforeEach
    void setUp() {
        rates = new FakeCommissionRates()
            .with(UBER, "20.00", LocalDate.of(2026, 1, 1))
            .with(UBER, "22.00", LocalDate.of(2026, 5, 1))
            .with(UBER, "30.00", LocalDate.of(2026, 9, 1))          // +8 points: sudden
            .with(FOODPANDA, "15.00", LocalDate.of(2026, 3, 1));
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = AccessProxy.wrap(CommissionService.class, new DefaultCommissionService(
            rates, new FakePlatforms().with(UBER, "Uber").with(FOODPANDA, "Foodpanda"), clock));
        Session.start(Fakes.representative(UBER_REP, "Hina Malik", UBER));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    private static BigDecimal pct(String value) {
        return new BigDecimal(value);
    }

    // ---------------------------------------------------------------- history (FR-26)

    @Test
    @DisplayName("history is newest first and every rate knows the one before it")
    void historyOrderAndPrevious() {
        List<CommissionRate> history = service.history(UBER);
        assertEquals(3, history.size());
        assertEquals(LocalDate.of(2026, 9, 1), history.get(0).getEffectiveDate());
        assertEquals(LocalDate.of(2026, 1, 1), history.get(2).getEffectiveDate());
        assertEquals(0, pct("22.00").compareTo(history.get(0).getPreviousPercent()));
        assertEquals(null, history.get(2).getPreviousPercent());
    }

    @Test
    @DisplayName("a big jump is highlighted, a small one is not (FR-26)")
    void suddenChangesAreFlagged() {
        List<CommissionRate> history = service.history(UBER);
        assertTrue(history.get(0).isSuddenChange());      // 22 -> 30
        assertFalse(history.get(1).isSuddenChange());     // 20 -> 22
        assertFalse(history.get(2).isSuddenChange());     // first rate
    }

    @Test
    void historyOnlyContainsThePlatformsOwnRates() {
        Session.start(Fakes.worker(20, "Ali Raza"));
        assertEquals(1, service.history(FOODPANDA).size());
    }

    @Test
    void unknownPlatformGivesAClearError() {
        Session.start(Fakes.worker(20, "Ali Raza"));      // a rep would be stopped earlier by the Proxy
        assertThrows(NotFoundException.class, () -> service.history(999));
    }

    // ---------------------------------------------------------------- recording (FR-27)

    @Test
    @DisplayName("a representative records a rate for their own platform")
    void recordRate() {
        CommissionRate saved = service.recordRate(UBER_REP, UBER, pct("27.5"), LocalDate.of(2026, 11, 1));
        assertEquals(UBER, saved.getPlatformId());
        assertEquals(UBER_REP, saved.getRecordedByRepId());
        assertEquals(0, pct("27.50").compareTo(saved.getRatePercent()));
        assertEquals(LocalDate.of(2026, 11, 1), saved.getEffectiveDate());
        assertEquals(4, service.history(UBER).size());
    }

    @Test
    @DisplayName("a rate recorded in the middle of the history is compared with the right neighbour")
    void recordedRateFitsIntoHistory() {
        service.recordRate(UBER_REP, UBER, pct("40"), LocalDate.of(2026, 11, 1));
        CommissionRate newest = service.history(UBER).get(0);
        assertEquals(0, pct("40.00").compareTo(newest.getRatePercent()));
        assertEquals(0, pct("30.00").compareTo(newest.getPreviousPercent()));
        assertTrue(newest.isSuddenChange());
    }

    @Test
    void ratesOutsideZeroToHundredAreRejected() {
        assertThrows(ValidationException.class, () -> service.recordRate(UBER_REP, UBER, pct("-1"), TODAY));
        assertThrows(ValidationException.class, () -> service.recordRate(UBER_REP, UBER, pct("100.01"), TODAY));
        assertEquals(3, rates.findByPlatform(UBER).size());
    }

    @Test
    void zeroAndHundredAreAllowed() {
        service.recordRate(UBER_REP, UBER, pct("0"), LocalDate.of(2026, 10, 1));
        service.recordRate(UBER_REP, UBER, pct("100"), LocalDate.of(2026, 10, 2));
        assertEquals(5, rates.findByPlatform(UBER).size());
    }

    @Test
    void tooManyDecimalPlacesAreRejected() {
        assertThrows(ValidationException.class, () -> service.recordRate(UBER_REP, UBER, pct("22.555"), TODAY));
        // trailing zeros are fine
        service.recordRate(UBER_REP, UBER, pct("22.500"), TODAY);
    }

    @Test
    void missingValuesListEveryProblem() {
        ValidationException error = assertThrows(ValidationException.class,
            () -> service.recordRate(UBER_REP, UBER, null, null));
        assertEquals(2, error.getProblems().size());
    }

    @Test
    void dateLimitsAreChecked() {
        assertThrows(ValidationException.class,
            () -> service.recordRate(UBER_REP, UBER, pct("20"), LocalDate.of(1999, 12, 31)));
        assertThrows(ValidationException.class,
            () -> service.recordRate(UBER_REP, UBER, pct("20"), TODAY.plusDays(367)));
        // a planned change a few months ahead is fine
        service.recordRate(UBER_REP, UBER, pct("20"), TODAY.plusMonths(3));
    }

    @Test
    @DisplayName("two rates for the same day on one platform are refused")
    void duplicateDateIsRefused() {
        ValidationException error = assertThrows(ValidationException.class,
            () -> service.recordRate(UBER_REP, UBER, pct("21"), LocalDate.of(2026, 5, 1)));
        assertTrue(error.getMessage().contains("already recorded"));
        assertEquals(3, rates.findByPlatform(UBER).size());
    }

    // ---------------------------------------------------------------- Proxy rules (FR-05, FR-06)

    @Test
    @DisplayName("a representative cannot record or even view another platform's rates (FR-06)")
    void otherPlatformIsDenied() {
        assertThrows(AccessDeniedException.class,
            () -> service.recordRate(UBER_REP, FOODPANDA, pct("20"), TODAY));
        assertThrows(AccessDeniedException.class, () -> service.history(FOODPANDA));
        assertEquals(1, rates.findByPlatform(FOODPANDA).size());
    }

    @Test
    void cannotRecordInSomeoneElsesName() {
        assertThrows(AccessDeniedException.class,
            () -> service.recordRate(999, UBER, pct("20"), TODAY));
    }

    @Test
    @DisplayName("workers and advocates may view any platform's history but never record")
    void workersAndAdvocatesCanOnlyView() {
        Session.start(Fakes.worker(20, "Ali Raza"));
        assertEquals(3, service.history(UBER).size());
        assertThrows(AccessDeniedException.class, () -> service.recordRate(20, UBER, pct("20"), TODAY));
    }

    @Test
    void loggedOutUsersAreDenied() {
        Session.clear();
        assertThrows(AccessDeniedException.class, () -> service.history(UBER));
        assertThrows(AccessDeniedException.class, () -> service.recordRate(UBER_REP, UBER, pct("20"), TODAY));
    }
}
