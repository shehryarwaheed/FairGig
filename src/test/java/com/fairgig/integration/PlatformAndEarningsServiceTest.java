package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.EarningInput;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.EntryMethod;
import com.fairgig.model.Platform;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.service.auth.AuthService;
import com.fairgig.service.auth.DefaultAuthService;
import com.fairgig.service.earnings.DefaultEarningsService;
import com.fairgig.service.earnings.EarningsService;
import com.fairgig.service.platform.DefaultPlatformService;
import com.fairgig.service.platform.PlatformService;
import com.fairgig.service.platform.WorkerPlatformInfo;
import com.fairgig.strategy.validation.ValidationResult;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR-02, FR-06 to FR-11: platforms a worker works on, creating platforms, logging earnings. */
class PlatformAndEarningsServiceTest extends DbTestSupport {

    private final AuthService auth = AccessProxy.wrap(AuthService.class, new DefaultAuthService(users, platforms));
    private final PlatformService platformService =
        AccessProxy.wrap(PlatformService.class, new DefaultPlatformService(platforms, workerPlatforms));
    private final EarningsService earningsService =
        AccessProxy.wrap(EarningsService.class, new DefaultEarningsService(earnings, platforms, workerPlatforms));

    private static EarningInput input(Platform p, String hours, String gross, String ded, String net) {
        return new EarningInput(p == null ? null : p.getPlatformId(), LocalDate.now().minusDays(1),
            new BigDecimal(hours), new BigDecimal(gross), new BigDecimal(ded), new BigDecimal(net));
    }

    private static List<String> problemsOf(Runnable action) {
        return assertThrows(ValidationException.class, action::run).getProblems();
    }

    // ---------------------------------------------------------------- platforms

    @Test
    void guestsCanSeeActivePlatformsForTheRegistrationPicker() {
        Platform p = newPlatform();
        assertTrue(platformService.listActivePlatforms().stream().anyMatch(x -> x.getPlatformId().equals(p.getPlatformId())));
    }

    @Test
    void aWorkerRegistersAndRemovesPlatformsAndSeesOnlyTheirOwn() {
        Worker w = newWorker();
        Worker other = newWorker();
        Platform p = newPlatform();
        auth.login(w.getEmail(), PASSWORD);
        platformService.registerPlatform(w.getUserId(), p.getPlatformId(), "  my_handle ", LocalDate.of(2026, 1, 15));
        List<WorkerPlatformInfo> mine = platformService.myPlatforms(w.getUserId());
        assertEquals(1, mine.size());
        assertEquals("my_handle", mine.get(0).accountHandle());
        assertTrue(problemsOf(() -> platformService.registerPlatform(w.getUserId(), p.getPlatformId(), null, null)).get(0).contains("already registered"));
        assertThrows(AccessDeniedException.class, () -> platformService.myPlatforms(other.getUserId()));
        platformService.removePlatform(w.getUserId(), p.getPlatformId());
        assertTrue(platformService.myPlatforms(w.getUserId()).isEmpty());
        assertFalse(problemsOf(() -> platformService.removePlatform(w.getUserId(), p.getPlatformId())).isEmpty());
    }

    @Test
    void platformDetailsAreValidated() {
        Worker w = newWorker();
        Platform p = newPlatform();
        join(w, p);
        auth.login(w.getEmail(), PASSWORD);
        assertFalse(problemsOf(() -> platformService.updatePlatformDetails(w.getUserId(), p.getPlatformId(), null, LocalDate.now().plusDays(3))).isEmpty());
        assertFalse(problemsOf(() -> platformService.updatePlatformDetails(w.getUserId(), p.getPlatformId(), "x".repeat(101), null)).isEmpty());
        assertFalse(problemsOf(() -> platformService.registerPlatform(w.getUserId(), 99999999, null, null)).isEmpty());
    }

    @Test
    void aWorkerCanCreateANewPlatformButNotADuplicate() {
        Worker w = newWorker();
        auth.login(w.getEmail(), PASSWORD);
        String suffix = unique("x");
        // extra spaces are cleaned: the saved name is "ITPlat- x-1234abcd" (still found by the clean-up pattern)
        Platform created = platformService.createPlatform("   ITPlat-     " + suffix + "  ", " Courier ");
        assertTrue(created.getName().startsWith("ITPlat- "));
        assertNotNull(created.getPlatformId());
        assertTrue(created.isActive());
        assertEquals("Courier", created.getCategory());
        assertTrue(problemsOf(() -> platformService.createPlatform(created.getName().toUpperCase(), "x")).get(0).contains("already exists"));
        assertEquals(2, problemsOf(() -> platformService.createPlatform("  ", "")).size());
    }

    @Test
    void aRepresentativeSeesOnlyTheirOwnPlatformAndCannotCreateOrJoin() {
        Platform mine = newPlatform();
        Platform other = newPlatform();
        var rep = newRepresentative(mine.getPlatformId());
        auth.login(rep.getEmail(), PASSWORD);
        assertEquals(mine.getName(), platformService.getPlatform(mine.getPlatformId()).getName());
        assertThrows(AccessDeniedException.class, () -> platformService.getPlatform(other.getPlatformId()));
        assertThrows(AccessDeniedException.class, () -> platformService.createPlatform("Nope", "Nope"));
        assertThrows(AccessDeniedException.class, () -> platformService.myPlatforms(rep.getUserId()));
    }

    // ---------------------------------------------------------------- earnings

    @Test
    void validEarningIsSavedAsPendingAndManualWithExactMoney() {
        Worker w = newWorker();
        Platform p = newPlatform();
        join(w, p);
        auth.login(w.getEmail(), PASSWORD);
        EarningsRecord saved = earningsService.logEarning(w.getUserId(), input(p, "6.5", "5200.00", "1300", "3900.00"));
        EarningsRecord back = earnings.findById(saved.getRecordId()).orElseThrow();
        assertEquals(VerificationStatus.PENDING, back.getStatus());
        assertEquals(EntryMethod.MANUAL, back.getEntryMethod());
        assertEquals(new BigDecimal("3900.00"), back.getNetReceived());
        assertEquals(new BigDecimal("600.00"), back.calculateEffectiveRate());
    }

    @Test
    void invalidEarningListsEveryProblemAndSavesNothing() {
        Worker w = newWorker();
        Platform p = newPlatform();
        join(w, p);
        auth.login(w.getEmail(), PASSWORD);
        List<String> problems = problemsOf(() -> earningsService.logEarning(w.getUserId(), input(p, "30", "-5", "1.234", "1")));
        assertTrue(problems.size() >= 3);
        assertTrue(earnings.findByWorker(w.getUserId(), EarningsFilter.none()).isEmpty());
    }

    @Test
    void checkReportsProblemsWithoutSaving() {
        Worker w = newWorker();
        auth.login(w.getEmail(), PASSWORD);
        ValidationResult result = earningsService.check(w.getUserId(), new EarningInput(null, null, null, null, null, null));
        assertEquals(6, result.problems().size());
        assertTrue(earnings.findByWorker(w.getUserId(), EarningsFilter.none()).isEmpty());
    }

    @Test
    void thePlatformMustExistBeActiveAndBeOneTheWorkerWorksOn() {
        Worker w = newWorker();
        Platform notJoined = newPlatform();
        auth.login(w.getEmail(), PASSWORD);
        assertTrue(problemsOf(() -> earningsService.logEarning(w.getUserId(), input(notJoined, "5", "100", "20", "80"))).get(0).contains("not registered on"));
        assertFalse(problemsOf(() -> earningsService.logEarning(w.getUserId(),
            new EarningInput(99999999, LocalDate.now(), BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE))).isEmpty());
    }

    @Test
    void onlyWorkersLogEarningsAndOnlyTheirOwn() {
        Worker w = newWorker();
        Worker other = newWorker();
        Platform p = newPlatform();
        join(w, p);
        join(other, p);
        assertThrows(AccessDeniedException.class, () -> earningsService.logEarning(w.getUserId(), input(p, "5", "100", "20", "80")), "guest");
        auth.login(w.getEmail(), PASSWORD);
        assertThrows(AccessDeniedException.class, () -> earningsService.logEarning(other.getUserId(), input(p, "5", "100", "20", "80")));
        auth.logout();
        var rep = newRepresentative(p.getPlatformId());
        auth.login(rep.getEmail(), PASSWORD);
        assertThrows(AccessDeniedException.class, () -> earningsService.logEarning(rep.getUserId(), input(p, "5", "100", "20", "80")));
    }

    @Test
    void recentEarningsAreNewestFirstAndTheLimitIsClamped() {
        Worker w = newWorker();
        Platform p = newPlatform();
        join(w, p);
        for (int day = 1; day <= 5; day++) {
            newEarning(w, p, LocalDate.of(2026, 11, day), "10", VerificationStatus.PENDING);
        }
        auth.login(w.getEmail(), PASSWORD);
        List<EarningsRecord> three = earningsService.recentEarnings(w.getUserId(), 3);
        assertEquals(3, three.size());
        assertEquals(LocalDate.of(2026, 11, 5), three.get(0).getShiftDate());
        assertEquals(1, earningsService.recentEarnings(w.getUserId(), 0).size());
        assertEquals(5, earningsService.recentEarnings(w.getUserId(), 9999).size());
    }
}
