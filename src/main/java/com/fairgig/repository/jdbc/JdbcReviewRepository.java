package com.fairgig.repository.jdbc;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.ReviewDecision;
import com.fairgig.model.VerificationReview;
import com.fairgig.repository.ReviewRepository;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/** PostgreSQL implementation of {@link ReviewRepository}. */
public class JdbcReviewRepository implements ReviewRepository {

    private static final String SELECT =
        "SELECT r.review_id, r.record_id, r.representative_id, r.decision, r.explanation, r.reviewed_at "
        + "FROM verification_reviews r ";

    private static final String NEWEST_FIRST = " ORDER BY r.reviewed_at DESC, r.review_id DESC";

    @Override
    public VerificationReview save(VerificationReview review) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, review);
        } catch (SQLException e) {
            throw new DataAccessException("Could not connect to the database.", e);
        }
    }

    @Override
    public VerificationReview save(Connection connection, VerificationReview review) {
        return Jdbc.queryOne(connection,
            "INSERT INTO verification_reviews (record_id, representative_id, decision, explanation) "
                + "VALUES (?, ?, ?, ?) RETURNING review_id, reviewed_at",
            ps -> {
                ps.setInt(1, review.getRecordId());
                ps.setInt(2, review.getRepresentativeId());
                ps.setString(3, review.getDecision().name());
                ps.setString(4, review.getExplanation());
            },
            rs -> {
                review.setReviewId(rs.getInt(1));
                review.setReviewedAt(rs.getObject(2, LocalDateTime.class));
                return review;
            }).orElseThrow();
    }

    @Override
    public List<VerificationReview> findByRecord(int recordId) {
        return Jdbc.queryList(SELECT + "WHERE r.record_id = ?" + NEWEST_FIRST,
            ps -> ps.setInt(1, recordId), JdbcReviewRepository::map);
    }

    @Override
    public List<VerificationReview> findByRepresentative(int representativeId) {
        return Jdbc.queryList(SELECT + "WHERE r.representative_id = ?" + NEWEST_FIRST,
            ps -> ps.setInt(1, representativeId), JdbcReviewRepository::map);
    }

    @Override
    public List<VerificationReview> findByPlatform(int platformId) {
        return Jdbc.queryList(
            SELECT + "JOIN earnings_records e ON e.record_id = r.record_id WHERE e.platform_id = ?" + NEWEST_FIRST,
            ps -> ps.setInt(1, platformId), JdbcReviewRepository::map);
    }

    private static VerificationReview map(ResultSet rs) throws SQLException {
        VerificationReview review = new VerificationReview();
        review.setReviewId(rs.getInt("review_id"));
        review.setRecordId(rs.getInt("record_id"));
        review.setRepresentativeId(rs.getInt("representative_id"));
        review.setDecision(ReviewDecision.fromDb(rs.getString("decision")));
        review.setExplanation(rs.getString("explanation"));
        review.setReviewedAt(Jdbc.getDateTime(rs, "reviewed_at"));
        return review;
    }
}
