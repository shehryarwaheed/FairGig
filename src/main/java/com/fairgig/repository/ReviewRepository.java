package com.fairgig.repository;

import com.fairgig.model.VerificationReview;
import java.sql.Connection;
import java.util.List;

/**
 * Data access for representatives' decisions (table "verification_reviews").
 * The Connection overload lets the verification service save a decision, change the earning's
 * status and notify the worker in ONE transaction (NFR-06).
 */
public interface ReviewRepository {

    /** Inserts the review and fills in its id and time. */
    VerificationReview save(VerificationReview review);

    /** Same, but inside the caller's transaction (connection is not closed or committed). */
    VerificationReview save(Connection connection, VerificationReview review);

    /** Every decision ever made about one earning, newest first. */
    List<VerificationReview> findByRecord(int recordId);

    /** Decisions made by one representative, newest first. */
    List<VerificationReview> findByRepresentative(int representativeId);

    /** Decisions about earnings of one platform (the review history screen), newest first. */
    List<VerificationReview> findByPlatform(int platformId);
}
