package com.fairgig.unit.earnings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.VerificationStatus;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.security.Session;
import com.fairgig.service.evidence.DefaultEvidenceService;
import com.fairgig.service.evidence.EvidenceService;
import com.fairgig.unit.support.Fakes;
import com.fairgig.unit.support.Fakes.FakeEarnings;
import com.fairgig.unit.support.Fakes.FakeEvidence;
import com.fairgig.unit.support.Fakes.FakePlatforms;
import com.fairgig.unit.support.Fakes.FakeStorage;
import com.fairgig.unit.support.Fakes.FakeUsers;
import com.fairgig.unit.support.Fakes.RecordingSender;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** FR-17: new evidence for a Flagged or Unverifiable earning sends it back to Pending (State pattern). */
class EvidenceResubmitTest {

    private static final int ALI = 20;
    private static final int SARA = 21;
    private static final int UBER = 1;

    /** The first bytes of a real PNG file: the service checks the content, not just the name. */
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    private FakeEarnings earnings;
    private FakeEvidence evidence;
    private FakeStorage storage;
    private RecordingSender sender;
    private EvidenceService service;

    @BeforeEach
    void setUp() {
        earnings = new FakeEarnings().with(
            Fakes.earning(1, ALI, UBER, VerificationStatus.PENDING),
            Fakes.earning(2, ALI, UBER, VerificationStatus.DISPUTED_FLAGGED),
            Fakes.earning(3, ALI, UBER, VerificationStatus.UNVERIFIABLE),
            Fakes.earning(4, ALI, UBER, VerificationStatus.VERIFIED),
            Fakes.earning(5, SARA, UBER, VerificationStatus.DISPUTED_FLAGGED));
        evidence = new FakeEvidence();
        storage = new FakeStorage();
        sender = new RecordingSender();
        service = wrap(earnings);
        Session.start(Fakes.worker(ALI, "Ali Raza"));
    }

    private EvidenceService wrap(FakeEarnings repository) {
        return AccessProxy.wrap(EvidenceService.class, new DefaultEvidenceService(
            evidence, repository, new FakePlatforms().with(UBER, "Uber"),
            new FakeUsers().with(Fakes.worker(ALI, "Ali Raza")), storage, sender, Fakes.noDatabase()));
    }

    @AfterEach
    void tearDown() {
        Session.clear();
    }

    @Test
    @DisplayName("Flagged + new evidence -> Pending, and the representatives are told")
    void flaggedGoesBackToPending() {
        service.uploadEvidence(ALI, 2, "proof.png", PNG);

        assertEquals(VerificationStatus.PENDING, earnings.records.get(2).getStatus());
        assertEquals(1, evidence.findByRecord(2).size());
        assertEquals(1, sender.sentToRepresentatives.size());
        assertEquals(UBER, sender.sentToRepresentatives.get(0).platformId());
    }

    @Test
    @DisplayName("Unverifiable + new evidence -> Pending")
    void unverifiableGoesBackToPending() {
        service.uploadEvidence(ALI, 3, "statement.png", PNG);
        assertEquals(VerificationStatus.PENDING, earnings.records.get(3).getStatus());
    }

    @Test
    @DisplayName("extra evidence on a Pending earning is fine and changes nothing else")
    void pendingStaysPending() {
        service.uploadEvidence(ALI, 1, "more.png", PNG);
        assertEquals(VerificationStatus.PENDING, earnings.records.get(1).getStatus());
        assertEquals(1, evidence.findByRecord(1).size());
    }

    @Test
    @DisplayName("Verified never returns to Pending: the upload is refused and nothing is stored (FR-22)")
    void verifiedIsRefused() {
        assertThrows(ValidationException.class, () -> service.uploadEvidence(ALI, 4, "late.png", PNG));
        assertEquals(VerificationStatus.VERIFIED, earnings.records.get(4).getStatus());
        assertTrue(evidence.findByRecord(4).isEmpty());
        assertTrue(storage.files.isEmpty());
    }

    @Test
    @DisplayName("an invalid file leaves a Flagged earning Flagged")
    void badFileChangesNothing() {
        assertThrows(ValidationException.class, () -> service.uploadEvidence(ALI, 2, "virus.exe", PNG));
        assertThrows(ValidationException.class, () -> service.uploadEvidence(ALI, 2, "fake.png", new byte[] {1, 2, 3}));
        assertEquals(VerificationStatus.DISPUTED_FLAGGED, earnings.records.get(2).getStatus());
        assertTrue(sender.sentToRepresentatives.isEmpty());
    }

    @Test
    @DisplayName("if someone changed the status meanwhile, nothing is saved and the file is removed (NFR-06)")
    void concurrentChangeRollsBack() {
        FakeEarnings racing = new FakeEarnings() {
            @Override
            public boolean updateStatus(java.sql.Connection c, int id, VerificationStatus expected,
                                        VerificationStatus next) {
                records.get(id).setStatus(VerificationStatus.VERIFIED);   // a reviewer got there first
                return super.updateStatus(c, id, expected, next);
            }
        }.with(Fakes.earning(2, ALI, UBER, VerificationStatus.DISPUTED_FLAGGED));

        assertThrows(ValidationException.class, () -> wrap(racing).uploadEvidence(ALI, 2, "proof.png", PNG));
        assertTrue(evidence.saved.isEmpty());
        assertTrue(storage.files.isEmpty());          // the stored file was cleaned up
        assertEquals(1, storage.deleted.size());
        assertTrue(sender.sentToRepresentatives.isEmpty());
    }

    @Test
    @DisplayName("a worker cannot add evidence to someone else's earning")
    void otherWorkersEarningIsDenied() {
        assertThrows(AccessDeniedException.class, () -> service.uploadEvidence(ALI, 5, "proof.png", PNG));
        EarningsRecord theirs = earnings.records.get(5);
        assertEquals(VerificationStatus.DISPUTED_FLAGGED, theirs.getStatus());
    }
}
