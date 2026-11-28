package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.database.TransactionManager;
import com.fairgig.model.Advocate;
import com.fairgig.model.Notification;
import com.fairgig.model.Platform;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Worker;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.service.auth.AuthService;
import com.fairgig.service.auth.DefaultAuthService;
import com.fairgig.service.notification.DefaultNotificationService;
import com.fairgig.service.notification.NotificationMessages;
import com.fairgig.service.notification.NotificationService;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.NotFoundException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** FR-41, FR-42: sending notifications (internal) and reading your own (guarded). */
class NotificationServiceTest extends DbTestSupport {

    private final AuthService auth = AccessProxy.wrap(AuthService.class, new DefaultAuthService(users, platforms));
    private final DefaultNotificationService real = new DefaultNotificationService(notifications, users);
    private final NotificationService service = AccessProxy.wrap(NotificationService.class, real);

    @Test
    void aUserReadsOnlyTheirOwnNotificationsNewestFirstAndSeesTheUnreadCount() {
        Worker w = newWorker();
        real.notifyUser(w.getUserId(), "first");
        real.notifyUser(w.getUserId(), "second");
        auth.login(w.getEmail(), PASSWORD);
        List<Notification> mine = service.myNotifications(w.getUserId());
        assertEquals("second", mine.get(0).getMessage());
        assertEquals(2, service.unreadCount(w.getUserId()));
        service.markRead(w.getUserId(), mine.get(0).getNotificationId());
        assertEquals(1, service.unreadCount(w.getUserId()));
        service.markAllRead(w.getUserId());
        assertEquals(0, service.unreadCount(w.getUserId()));
    }

    @Test
    void nobodyCanReadOrMarkSomeoneElsesNotifications() {
        Worker a = newWorker();
        Worker b = newWorker();
        real.notifyUser(b.getUserId(), "private");
        int bNote = notifications.findByUser(b.getUserId()).get(0).getNotificationId();
        auth.login(a.getEmail(), PASSWORD);
        assertThrows(AccessDeniedException.class, () -> service.myNotifications(b.getUserId()));
        assertThrows(AccessDeniedException.class, () -> service.unreadCount(b.getUserId()));
        assertThrows(AccessDeniedException.class, () -> service.markRead(b.getUserId(), bNote));
        assertThrows(NotFoundException.class, () -> service.markRead(a.getUserId(), bNote), "own id + foreign notification id");
        assertFalse(notifications.findByUser(b.getUserId()).get(0).isRead());
    }

    @Test
    void guestsCannotReadNotifications() {
        assertThrows(AccessDeniedException.class, () -> service.myNotifications(1));
    }

    @Test
    void platformRepresentativesAreNotifiedOnlyForTheirOwnPlatform() {
        Platform a = newPlatform();
        Platform b = newPlatform();
        PlatformRepresentative repA = newRepresentative(a.getPlatformId());
        PlatformRepresentative repB = newRepresentative(b.getPlatformId());
        assertEquals(1, real.notifyPlatformRepresentatives(a.getPlatformId(), "for A"));
        assertEquals(1, notifications.findByUser(repA.getUserId()).size());
        assertTrue(notifications.findByUser(repB.getUserId()).isEmpty());
        assertEquals(0, real.notifyPlatformRepresentatives(newPlatform().getPlatformId(), "nobody"), "no reps = no error");
    }

    @Test
    void advocatesCanBeNotified() {
        Advocate adv = newAdvocate();
        // "IT-" marks messages that reach EVERY advocate (also the seeded one): the clean-up removes them
        assertTrue(real.notifyAdvocates("IT-for advocates") >= 1);
        assertEquals("IT-for advocates", notifications.findByUser(adv.getUserId()).get(0).getMessage());
    }

    @Test
    void messagesAreTrimmedShortenedAndBlankOnesRefused() {
        Worker w = newWorker();
        real.notifyUser(w.getUserId(), "  hello  ");
        assertEquals("hello", notifications.findByUser(w.getUserId()).get(0).getMessage());
        real.notifyUser(w.getUserId(), "x".repeat(400));
        String shortened = notifications.findByUser(w.getUserId()).get(0).getMessage();
        assertEquals(255, shortened.length());
        assertTrue(shortened.endsWith("..."));
        assertThrows(IllegalArgumentException.class, () -> real.notifyUser(w.getUserId(), "   "));
    }

    @Test
    void notificationsSentInsideATransactionAreRolledBackWithIt() throws SQLException {
        Worker w = newWorker();
        assertThrows(SQLException.class, () -> TransactionManager.inTransaction(c -> {
            real.notifyUser(c, w.getUserId(), "inside a failing transaction");
            throw new SQLException("simulated failure");
        }));
        assertTrue(notifications.findByUser(w.getUserId()).isEmpty());
        TransactionManager.inTransaction(c -> {
            real.notifyUser(c, w.getUserId(), "committed");
            return null;
        });
        assertEquals("committed", notifications.findByUser(w.getUserId()).get(0).getMessage());
    }

    @Test
    void standardWording() {
        assertEquals("Ali uploaded evidence for a Uber earning of 2026-11-03.",
            NotificationMessages.evidenceUploaded("Ali", "Uber", LocalDate.of(2026, 11, 3)));
        assertTrue(NotificationMessages.recordReviewed("Uber", LocalDate.of(2026, 11, 3), "Verified").contains("marked Verified"));
        assertTrue(NotificationMessages.complaintFiled("Ali", "Uber").contains("new complaint"));
        assertTrue(NotificationMessages.complaintAnswered("Uber").contains("new response"));
        assertTrue(NotificationMessages.complaintResolved("Uber").contains("resolved"));
    }
}
