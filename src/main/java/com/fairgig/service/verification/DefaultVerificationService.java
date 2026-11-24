package com.fairgig.service.verification;

import com.fairgig.database.TransactionManager;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.ReviewDecision;
import com.fairgig.model.User;
import com.fairgig.model.VerificationReview;
import com.fairgig.proxy.AccessPolicy;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.EvidenceRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.ReviewRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.service.notification.NotificationMessages;
import com.fairgig.service.notification.NotificationSender;
import com.fairgig.state.verification.VerificationState;
import com.fairgig.state.verification.VerificationStates;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Real implementation of {@link VerificationService}. Wrap it with AccessProxy before giving it to screens. */
public class DefaultVerificationService implements VerificationService {

    /** Runs work as one transaction. Tests hand in a fake so no database is needed. */
    @FunctionalInterface
    public interface Transactions {
        <T> T run(TransactionManager.Work<T> work) throws SQLException;
    }

    private final EarningsRepository earnings;
    private final ReviewRepository reviews;
    private final EvidenceRepository evidence;
    private final UserRepository users;
    private final PlatformRepository platforms;
    private final NotificationSender notifications;
    private final Transactions transactions;

    public DefaultVerificationService(EarningsRepository earnings, ReviewRepository reviews,
                                      EvidenceRepository evidence, UserRepository users,
                                      PlatformRepository platforms, NotificationSender notifications) {
        this(earnings, reviews, evidence, users, platforms, notifications, TransactionManager::inTransaction);
    }

    public DefaultVerificationService(EarningsRepository earnings, ReviewRepository reviews,
                                      EvidenceRepository evidence, UserRepository users,
                                      PlatformRepository platforms, NotificationSender notifications,
                                      Transactions transactions) {
        this.earnings = earnings;
        this.reviews = reviews;
        this.evidence = evidence;
        this.users = users;
        this.platforms = platforms;
        this.notifications = notifications;
        this.transactions = transactions;
    }

    @Override
    public List<ReviewCase> pendingEarnings(int platformId) {
        Map<Integer, String> workerNames = new HashMap<>();
        String platformName = platformName(platformId);
        List<ReviewCase> cases = new ArrayList<>();
        for (EarningsRecord record : earnings.findPendingByPlatform(platformId)) {
            cases.add(toCase(record, platformName, workerNames));
        }
        return cases;
    }

    @Override
    public ReviewCase openReview(int recordId) {
        EarningsRecord record = loadForReview(recordId);
        return toCase(record, platformName(record.getPlatformId()), new HashMap<>());
    }

    @Override
    public VerificationReview review(int representativeId, int recordId, ReviewDecision decision, String explanation) {
        EarningsRecord record = loadForReview(recordId);

        // The State pattern decides if this move is allowed and checks the explanation (FR-21, FR-22).
        VerificationState next = VerificationStates.of(record.getStatus()).review(decision, explanation);

        VerificationReview review = new VerificationReview();
        review.setRecordId(recordId);
        review.setRepresentativeId(representativeId);
        review.setDecision(decision);
        review.setExplanation(Validators.blankToNull(Validators.clean(explanation)));

        String message = NotificationMessages.recordReviewed(
            platformName(record.getPlatformId()), record.getShiftDate(), decision.label());

        try {
            transactions.run(connection -> {
                // 1. change the status only if nobody else did it first, 2. store the decision,
                // 3. notify the worker. Either all three happen or none (NFR-06).
                if (!earnings.updateStatus(connection, recordId, record.getStatus(), next.status())) {
                    throw new ValidationException(
                        "Another reviewer already decided this earning. Please refresh the list.");
                }
                reviews.save(connection, review);
                notifications.notifyUser(connection, record.getWorkerId(), message);
                return review;
            });
        } catch (SQLException e) {
            throw new DataAccessException("The decision could not be saved. Please try again.", e);
        }
        return review;
    }

    @Override
    public List<ReviewEntry> reviewHistory(int platformId) {
        Map<Integer, String> workerNames = new HashMap<>();
        List<ReviewEntry> entries = new ArrayList<>();
        for (VerificationReview review : reviews.findByPlatform(platformId)) {
            earnings.findById(review.getRecordId()).ifPresent(record ->
                entries.add(new ReviewEntry(review, record, workerName(record.getWorkerId(), workerNames))));
        }
        return entries;
    }

    // ---------------------------------------------------------------- helpers

    /** Loads the earning and makes sure a representative only touches their own platform (FR-06). */
    private EarningsRecord loadForReview(int recordId) {
        EarningsRecord record = earnings.findById(recordId)
            .orElseThrow(() -> new NotFoundException("This earning no longer exists."));
        AccessPolicy.requirePlatform(record.getPlatformId());
        return record;
    }

    private ReviewCase toCase(EarningsRecord record, String platformName, Map<Integer, String> workerNames) {
        return new ReviewCase(record, workerName(record.getWorkerId(), workerNames), platformName,
            evidence.findByRecord(record.getRecordId()));
    }

    private String workerName(int workerId, Map<Integer, String> cache) {
        return cache.computeIfAbsent(workerId,
            id -> users.findById(id).map(User::getName).orElse("A worker"));
    }

    private String platformName(int platformId) {
        return platforms.findById(platformId).map(p -> p.getName()).orElse("a platform");
    }
}
