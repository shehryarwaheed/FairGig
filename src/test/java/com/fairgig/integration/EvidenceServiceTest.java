package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Evidence;
import com.fairgig.model.FileType;
import com.fairgig.model.Platform;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.service.auth.AuthService;
import com.fairgig.service.auth.DefaultAuthService;
import com.fairgig.service.evidence.DefaultEvidenceService;
import com.fairgig.service.evidence.EvidenceService;
import com.fairgig.service.notification.DefaultNotificationService;
import com.fairgig.service.notification.NotificationSender;
import com.fairgig.storage.LocalEvidenceStorage;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** FR-14, FR-41, FR-06, NFR-09: evidence upload, storage, notifications and who may see the files. */
class EvidenceServiceTest extends DbTestSupport {

    @TempDir
    Path storageRoot;

    private final AuthService auth = AccessProxy.wrap(AuthService.class, new DefaultAuthService(users, platforms));
    private DefaultNotificationService notificationService;
    private EvidenceService service;
    private Worker worker;
    private Platform platform;
    private PlatformRepresentative rep;
    private EarningsRecord pending;

    @BeforeEach
    void setUp() {
        notificationService = new DefaultNotificationService(notifications, users);
        service = AccessProxy.wrap(EvidenceService.class, new DefaultEvidenceService(
            evidence, earnings, platforms, users, new LocalEvidenceStorage(storageRoot), notificationService));
        worker = newWorker();
        platform = newPlatform();
        join(worker, platform);
        rep = newRepresentative(platform.getPlatformId());
        pending = newEarning(worker, platform, LocalDate.of(2026, 11, 2), "100", VerificationStatus.PENDING);
        auth.login(worker.getEmail(), PASSWORD);
    }

    private static byte[] png(int size) {
        byte[] b = new byte[size];
        byte[] header = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(header, 0, b, 0, 8);
        return b;
    }

    private long filesOnDisk() throws IOException {
        try (Stream<Path> files = Files.walk(storageRoot)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    @Test
    void uploadSavesTheFileUnderASafeNameAndTheDatabaseRow() throws IOException {
        byte[] original = png(200);
        Evidence ev = service.uploadEvidence(worker.getUserId(), pending.getRecordId(), "My Screenshot 1.PNG", original);
        assertNotNull(ev.getEvidenceId());
        assertEquals(FileType.PNG, ev.getFileType());
        assertTrue(ev.getFilePath().startsWith("record-" + pending.getRecordId() + "/"));
        assertFalse(ev.getFilePath().contains("Screenshot"), "the user's file name is never used on disk");
        assertArrayEquals(original, Files.readAllBytes(storageRoot.resolve(ev.getFilePath())));
        assertEquals(1, service.listEvidence(pending.getRecordId()).size());
    }

    @Test
    void theRepresentativeOfThePlatformIsNotifiedInTheSameTransaction() {
        service.uploadEvidence(worker.getUserId(), pending.getRecordId(), "a.png", png(50));
        var messages = notifications.findByUser(rep.getUserId());
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).getMessage().contains("IT Worker uploaded evidence for a " + platform.getName()));
    }

    @Test
    void invalidFilesAreRefusedAndWriteNothing() throws IOException {
        assertTrue(assertThrows(ValidationException.class,
            () -> service.uploadEvidence(worker.getUserId(), pending.getRecordId(), "virus.exe", png(10))).getMessage().contains("Only PNG, JPG or PDF"));
        assertThrows(ValidationException.class, () -> service.uploadEvidence(worker.getUserId(), pending.getRecordId(), "proof.png", "hello".getBytes()));
        assertThrows(ValidationException.class, () -> service.uploadEvidence(worker.getUserId(), pending.getRecordId(), "a.png", new byte[0]));
        assertThrows(ValidationException.class, () -> service.uploadEvidence(worker.getUserId(), pending.getRecordId(), "big.png", png(5 * 1024 * 1024 + 1)));
        assertEquals(0, filesOnDisk());
        assertTrue(service.listEvidence(pending.getRecordId()).isEmpty());
    }

    @Test
    void exactlyFiveMegabytesIsAccepted() {
        assertNotNull(service.uploadEvidence(worker.getUserId(), pending.getRecordId(), "exact.png", png(5 * 1024 * 1024)));
    }

    @Test
    void verifiedEarningsRefuseEvidenceButFlaggedOnesAcceptIt() {
        EarningsRecord verified = newEarning(worker, platform, LocalDate.of(2026, 11, 3), "10", VerificationStatus.VERIFIED);
        EarningsRecord flagged = newEarning(worker, platform, LocalDate.of(2026, 11, 4), "10", VerificationStatus.DISPUTED_FLAGGED);
        assertTrue(assertThrows(ValidationException.class,
            () -> service.uploadEvidence(worker.getUserId(), verified.getRecordId(), "a.png", png(20))).getMessage().contains("already verified"));
        assertNotNull(service.uploadEvidence(worker.getUserId(), flagged.getRecordId(), "again.png", png(20)));
    }

    @Test
    void workersCannotUseSomeoneElsesEarningsOrIds() {
        Worker stranger = newWorker();
        Platform p = newPlatform();
        join(stranger, p);
        EarningsRecord strangers = newEarning(stranger, p, LocalDate.of(2026, 11, 2), "10", VerificationStatus.PENDING);
        assertThrows(AccessDeniedException.class, () -> service.uploadEvidence(worker.getUserId(), strangers.getRecordId(), "a.png", png(20)));
        assertThrows(AccessDeniedException.class, () -> service.uploadEvidence(stranger.getUserId(), strangers.getRecordId(), "a.png", png(20)));
        assertThrows(NotFoundException.class, () -> service.uploadEvidence(worker.getUserId(), 999999999, "a.png", png(20)));
        assertThrows(AccessDeniedException.class, () -> service.listEvidence(strangers.getRecordId()));
    }

    @Test
    void aRepresentativeSeesEvidenceOfTheirOwnPlatformOnly() {
        Evidence ev = service.uploadEvidence(worker.getUserId(), pending.getRecordId(), "a.png", png(60));
        auth.logout();
        auth.login(rep.getEmail(), PASSWORD);
        assertEquals(1, service.listEvidence(pending.getRecordId()).size());
        assertEquals(60, service.readEvidence(ev.getEvidenceId()).content().length);
        assertThrows(AccessDeniedException.class, () -> service.uploadEvidence(rep.getUserId(), pending.getRecordId(), "a.png", png(20)));

        PlatformRepresentative otherRep = newRepresentative(newPlatform().getPlatformId());
        auth.logout();
        auth.login(otherRep.getEmail(), PASSWORD);
        assertThrows(AccessDeniedException.class, () -> service.listEvidence(pending.getRecordId()));
        assertThrows(AccessDeniedException.class, () -> service.readEvidence(ev.getEvidenceId()));

        auth.logout();
        auth.login(newAdvocate().getEmail(), PASSWORD);
        assertThrows(AccessDeniedException.class, () -> service.listEvidence(pending.getRecordId()));
    }

    @Test
    void anotherWorkerCannotReadYourFile() {
        Evidence ev = service.uploadEvidence(worker.getUserId(), pending.getRecordId(), "a.png", png(60));
        Worker stranger = newWorker();
        auth.logout();
        auth.login(stranger.getEmail(), PASSWORD);
        assertThrows(AccessDeniedException.class, () -> service.readEvidence(ev.getEvidenceId()));
    }

    @Test
    void ifSavingToTheDatabaseFailsTheFileIsRemovedAgain() throws IOException {
        NotificationSender broken = new NotificationSender() {
            public void notifyUser(int u, String m) { throw new DataAccessException("boom"); }
            public void notifyUser(Connection c, int u, String m) { throw new DataAccessException("boom"); }
            public int notifyPlatformRepresentatives(int p, String m) { throw new DataAccessException("boom"); }
            public int notifyPlatformRepresentatives(Connection c, int p, String m) { throw new DataAccessException("boom"); }
            public int notifyAdvocates(String m) { throw new DataAccessException("boom"); }
        };
        EvidenceService fragile = new DefaultEvidenceService(
            evidence, earnings, platforms, users, new LocalEvidenceStorage(storageRoot), broken);
        assertThrows(DataAccessException.class, () -> fragile.uploadEvidence(worker.getUserId(), pending.getRecordId(), "x.png", png(30)));
        assertEquals(0, evidence.findByRecord(pending.getRecordId()).size(), "no evidence row");
        assertEquals(0, filesOnDisk(), "and no file left behind: all or nothing");
    }

    @Test
    void storageRefusesPathsThatLeaveItsFolder() {
        LocalEvidenceStorage storage = new LocalEvidenceStorage(storageRoot);
        assertThrows(DataAccessException.class, () -> storage.read("../../secret.txt"));
        storage.delete("record-1/does-not-exist.png");   // deleting a missing file is harmless
    }
}
