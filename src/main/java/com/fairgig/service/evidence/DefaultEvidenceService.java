package com.fairgig.service.evidence;

import com.fairgig.database.Transactions;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Evidence;
import com.fairgig.model.FileType;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.model.VerificationStatus;
import com.fairgig.proxy.AccessPolicy;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.EvidenceRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.service.notification.NotificationMessages;
import com.fairgig.service.notification.NotificationSender;
import com.fairgig.state.verification.VerificationState;
import com.fairgig.state.verification.VerificationStates;
import com.fairgig.storage.EvidenceStorage;
import com.fairgig.util.AccessDeniedException;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.sql.SQLException;
import java.util.List;

/** Real implementation of {@link EvidenceService}. Wrap it with AccessProxy before giving it to screens. */
public class DefaultEvidenceService implements EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final EarningsRepository earnings;
    private final PlatformRepository platforms;
    private final UserRepository users;
    private final EvidenceStorage storage;
    private final NotificationSender notifications;
    private final Transactions transactions;

    public DefaultEvidenceService(EvidenceRepository evidenceRepository, EarningsRepository earnings,
                                  PlatformRepository platforms, UserRepository users,
                                  EvidenceStorage storage, NotificationSender notifications) {
        this(evidenceRepository, earnings, platforms, users, storage, notifications, Transactions.standard());
    }

    public DefaultEvidenceService(EvidenceRepository evidenceRepository, EarningsRepository earnings,
                                  PlatformRepository platforms, UserRepository users,
                                  EvidenceStorage storage, NotificationSender notifications,
                                  Transactions transactions) {
        this.evidenceRepository = evidenceRepository;
        this.earnings = earnings;
        this.platforms = platforms;
        this.users = users;
        this.storage = storage;
        this.notifications = notifications;
        this.transactions = transactions;
    }

    @Override
    public Evidence uploadEvidence(int workerId, int recordId, String fileName, byte[] content) {
        EarningsRecord record = earnings.findById(recordId)
            .orElseThrow(() -> new NotFoundException("This earning no longer exists."));
        if (record.getWorkerId() != workerId) {
            throw new AccessDeniedException("You can only add evidence to your own earnings.");
        }
        if (record.getStatus() == VerificationStatus.VERIFIED) {
            throw new ValidationException("This earning is already verified, so no more evidence is needed.");
        }
        FileType type = EvidenceFileValidator.validate(fileName, content);

        // FR-17: new evidence for a Flagged or Unverifiable earning sends it back to Pending.
        // The State decides if that applies; extra evidence on a Pending earning changes nothing.
        VerificationStatus before = record.getStatus();
        VerificationState state = VerificationStates.of(before);
        boolean resubmitting = state.canResubmit();
        VerificationStatus after = resubmitting ? state.resubmit().status() : before;

        String workerName = users.findById(workerId).map(User::getName).orElse("A worker");
        String platformName = platforms.findById(record.getPlatformId()).map(p -> p.getName()).orElse("a platform");
        String message = NotificationMessages.evidenceUploaded(workerName, platformName, record.getShiftDate());

        // 1. the file goes to disk, 2. the database row, the status change (when resubmitting) and the
        // notifications are saved in ONE transaction, 3. if step 2 fails the file is removed again,
        // so nothing is left half-done.
        String path = storage.store(recordId, type, content);
        Evidence evidence = new Evidence();
        evidence.setRecordId(recordId);
        evidence.setFilePath(path);
        evidence.setFileType(type);
        try {
            transactions.run(connection -> {
                if (resubmitting && !earnings.updateStatus(connection, recordId, before, after)) {
                    throw new ValidationException(
                        "This earning was just reviewed by someone else. Please refresh and try again.");
                }
                evidenceRepository.save(connection, evidence);
                notifications.notifyPlatformRepresentatives(connection, record.getPlatformId(), message);
                return evidence;
            });
        } catch (SQLException | RuntimeException e) {
            storage.delete(path);
            if (e instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new DataAccessException("The evidence could not be saved. Please try again.", e);
        }
        return evidence;
    }

    @Override
    public List<Evidence> listEvidence(int recordId) {
        EarningsRecord record = earnings.findById(recordId)
            .orElseThrow(() -> new NotFoundException("This earning no longer exists."));
        authorize(record);
        return evidenceRepository.findByRecord(recordId);
    }

    @Override
    public EvidenceFile readEvidence(int evidenceId) {
        Evidence evidence = evidenceRepository.findById(evidenceId)
            .orElseThrow(() -> new NotFoundException("This evidence file no longer exists."));
        EarningsRecord record = earnings.findById(evidence.getRecordId())
            .orElseThrow(() -> new NotFoundException("This earning no longer exists."));
        authorize(record);
        return new EvidenceFile(evidence, storage.read(evidence.getFilePath()));
    }

    /** Worker: only own earnings. Representative: only earnings of their own platform (FR-06). */
    private void authorize(EarningsRecord record) {
        User user = AccessPolicy.requireLogin();
        if (user.getRole() == Role.WORKER) {
            if (!record.getWorkerId().equals(user.getUserId())) {
                throw new AccessDeniedException("You can only view evidence of your own earnings.");
            }
        } else {
            AccessPolicy.requirePlatform(record.getPlatformId());
        }
    }
}
