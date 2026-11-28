package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.database.TransactionManager;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Platform;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.service.auth.AuthService;
import com.fairgig.service.auth.DefaultAuthService;
import com.fairgig.service.earnings.DefaultEarningsService;
import com.fairgig.service.earnings.EarningsService;
import com.fairgig.service.notification.DefaultNotificationService;
import com.fairgig.service.platform.DefaultPlatformService;
import com.fairgig.service.platform.PlatformService;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** NFR-07: common screens load within 3 seconds for a worker with up to 5,000 records. */
class PerformanceTest extends DbTestSupport {

    private static final long LIMIT_MS = 3_000;

    private final AuthService auth = AccessProxy.wrap(AuthService.class, new DefaultAuthService(users, platforms));
    private final EarningsService earningsService =
        AccessProxy.wrap(EarningsService.class, new DefaultEarningsService(earnings, platforms, workerPlatforms));
    private final PlatformService platformService =
        AccessProxy.wrap(PlatformService.class, new DefaultPlatformService(platforms, workerPlatforms));
    private final DefaultNotificationService notificationService = new DefaultNotificationService(notifications, users);

    @Test
    void screensLoadInUnderThreeSecondsWithFiveThousandEarnings() throws SQLException {
        Worker w = newWorker();
        Platform a = newPlatform();
        Platform b = newPlatform();
        join(w, a);
        join(w, b);
        TransactionManager.inTransaction(c -> {
            for (int i = 0; i < 5000; i++) {
                EarningsRecord r = new EarningsRecord();
                r.setWorkerId(w.getUserId());
                r.setPlatformId(i % 2 == 0 ? a.getPlatformId() : b.getPlatformId());
                r.setShiftDate(LocalDate.of(2024, 1, 1).plusDays(i % 700));
                r.setHoursWorked(new BigDecimal("5"));
                r.setGrossEarnings(new BigDecimal("1000.00"));
                r.setDeductions(new BigDecimal("200.00"));
                r.setNetReceived(new BigDecimal("800.00"));
                r.setStatus(i % 5 == 0 ? VerificationStatus.VERIFIED : VerificationStatus.PENDING);
                earnings.save(c, r);
            }
            return null;
        });
        auth.login(w.getEmail(), PASSWORD);
        int id = w.getUserId();

        assertFast("full earnings history (5,000 rows)", () ->
            assertEquals(5000, earnings.findByWorker(id, EarningsFilter.none()).size()));
        assertFast("history filtered by platform, dates and status", () -> {
            List<EarningsRecord> filtered = earnings.findByWorker(id,
                new EarningsFilter(a.getPlatformId(), LocalDate.of(2024, 3, 1), LocalDate.of(2024, 9, 30), VerificationStatus.PENDING));
            assertTrue(filtered.size() > 0);
        });
        assertFast("home page data: latest earnings, platforms and unread count", () -> {
            assertEquals(20, earningsService.recentEarnings(id, 20).size());
            assertEquals(2, platformService.myPlatforms(id).size());
            notificationService.unreadCount(id);
        });
    }

    private static void assertFast(String what, Runnable action) {
        long start = System.nanoTime();
        action.run();
        long millis = (System.nanoTime() - start) / 1_000_000;
        System.out.println("NFR-07  " + what + ": " + millis + " ms");
        assertTrue(millis < LIMIT_MS, what + " took " + millis + " ms (limit " + LIMIT_MS + " ms)");
    }
}
