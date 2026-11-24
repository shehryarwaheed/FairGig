package com.fairgig.repository;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.VerificationStatus;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data access for earnings (table "earnings_records").
 * The overloads that take a Connection run inside the caller's transaction (the CSV import
 * uses them so that a failure leaves no partial data, NFR-06).
 */
public interface EarningsRepository {

    /** Inserts the earning (status PENDING unless set otherwise) and fills in id and createdAt. */
    EarningsRecord save(EarningsRecord record);

    /** Same, but inside the given transaction. The connection is NOT closed or committed here. */
    EarningsRecord save(Connection connection, EarningsRecord record);

    Optional<EarningsRecord> findById(int recordId);

    /** A worker's earnings, newest shift first, narrowed by the filter (use EarningsFilter.none() for all). */
    List<EarningsRecord> findByWorker(int workerId, EarningsFilter filter);

    /** Earnings of one platform that are still PENDING, oldest first (the representative's queue, FR-18). */
    List<EarningsRecord> findPendingByPlatform(int platformId);

    /**
     * Changes the status of an earning ONLY if it still has the status the caller saw
     * ({@code expected}). Returns false when someone else changed it first, so two reviewers can
     * never both decide the same earning. Runs inside the caller's transaction (FR-22, NFR-06).
     */
    boolean updateStatus(Connection connection, int recordId,
                         VerificationStatus expected, VerificationStatus newStatus);
}
