package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintStatus;
import com.fairgig.model.ComplaintTag;
import com.fairgig.repository.ComplaintClusterRepository;
import com.fairgig.repository.ComplaintRepository;
import com.fairgig.repository.ComplaintTagRepository;
import com.fairgig.repository.jdbc.JdbcComplaintClusterRepository;
import com.fairgig.repository.jdbc.JdbcComplaintRepository;
import com.fairgig.repository.jdbc.JdbcComplaintTagRepository;
import com.fairgig.util.DataAccessException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * NFR-13: the complaint repositories against the real PostgreSQL database.
 * Reads use the seed data. Every WRITE runs inside a transaction that is rolled back at the end,
 * so the database is left exactly as it was. The tests are skipped when no database is reachable.
 */
class ComplaintRepositoriesIntegrationTest {

    private final ComplaintRepository complaints = new JdbcComplaintRepository();
    private final ComplaintClusterRepository clusters = new JdbcComplaintClusterRepository();
    private final ComplaintTagRepository tags = new JdbcComplaintTagRepository();

    @BeforeAll
    static void needDatabase() {
        Assumptions.assumeTrue(DatabaseConnection.isAvailable(), "PostgreSQL is not reachable: skipping");
    }

    // ---------------------------------------------------------------- helpers

    @FunctionalInterface
    private interface Work {
        void run(Connection connection) throws SQLException;
    }

    /** Runs the work and ALWAYS rolls back, so nothing is saved. */
    private static void rolledBack(Work work) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
            } finally {
                connection.rollback();
            }
        }
    }

    private static Object scalar(Connection connection, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getObject(1) : null;
            }
        }
    }

    private static int userId(Connection c, String email) throws SQLException {
        return (Integer) scalar(c, "SELECT user_id FROM users WHERE email = ?", email);
    }

    private static int platformId(Connection c, String name) throws SQLException {
        return (Integer) scalar(c, "SELECT platform_id FROM platforms WHERE name = ?", name);
    }

    private static Complaint newComplaint(int workerId, int platformId) {
        Complaint c = new Complaint();
        c.setWorkerId(workerId);
        c.setPlatformId(platformId);
        c.setCategory(ComplaintCategory.UNFAIR_DEDUCTION);
        c.setDescription("Test complaint written by the integration test.");
        return c;
    }

    // ---------------------------------------------------------------- reads on the seed data

    @Test
    @DisplayName("an advocate's list: every complaint, newest first")
    void findAllReadsTheSeededComplaints() {
        List<Complaint> all = complaints.findAll();
        assertTrue(all.size() >= 3);
        for (int i = 1; i < all.size(); i++) {
            assertFalse(all.get(i).getCreatedAt().isAfter(all.get(i - 1).getCreatedAt()), "newest first");
        }
        assertTrue(all.stream().anyMatch(c -> c.getStatus() == ComplaintStatus.ESCALATED && c.getEscalatedAt() != null));
        assertTrue(all.stream().anyMatch(c -> c.getStatus() == ComplaintStatus.RESOLVED && c.getResolvedAt() != null));
    }

    @Test
    @DisplayName("a worker sees only their own complaints, with the optional earning link")
    void findByWorker() throws SQLException {
        int ali;
        try (Connection c = DatabaseConnection.getConnection()) {
            ali = userId(c, "worker1@fairgig.test");
        }
        List<Complaint> mine = complaints.findByWorker(ali);
        assertFalse(mine.isEmpty());
        assertTrue(mine.stream().allMatch(c -> c.getWorkerId() == ali));
        Complaint deduction = mine.stream().filter(c -> c.getCategory() == ComplaintCategory.UNFAIR_DEDUCTION)
            .findFirst().orElseThrow();
        assertNotNull(deduction.getRecordId(), "linked to an earning");
        assertNotNull(deduction.getClusterId(), "seeded into a cluster");
    }

    @Test
    @DisplayName("a platform's list only holds that platform's complaints")
    void findByPlatform() throws SQLException {
        int uber;
        try (Connection c = DatabaseConnection.getConnection()) {
            uber = platformId(c, "Uber");
        }
        List<Complaint> list = complaints.findByPlatform(uber);
        assertTrue(list.size() >= 2);
        assertTrue(list.stream().allMatch(c -> c.getPlatformId() == uber));
    }

    @Test
    void responsesAndClusters() {
        Complaint resolved = complaints.findAll().stream()
            .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).findFirst().orElseThrow();
        List<ComplaintResponse> responses = complaints.findResponses(resolved.getComplaintId());
        assertFalse(responses.isEmpty());
        assertNotNull(responses.get(0).getRespondedAt());

        ComplaintCluster cluster = clusters.findAll().stream()
            .filter(c -> c.getTitle().equals("Sudden commission increase")).findFirst().orElseThrow();
        assertEquals(cluster.getTitle(), clusters.findById(cluster.getClusterId()).orElseThrow().getTitle());
        assertFalse(complaints.findByCluster(cluster.getClusterId()).isEmpty());
        assertTrue(clusters.findById(-1).isEmpty());
    }

    @Test
    @DisplayName("tags: lookup ignores case, and the list screen gets all tags in one query")
    void tagReads() {
        assertTrue(tags.findAll().stream().anyMatch(t -> t.getName().equals("commission")));
        assertEquals("commission", tags.findByName("COMMISSION").orElseThrow().getName());
        assertTrue(tags.findByName("no-such-tag").isEmpty());

        Map<Integer, List<ComplaintTag>> byComplaint = tags.findAllByComplaint();
        assertFalse(byComplaint.isEmpty());
        int someComplaint = byComplaint.keySet().iterator().next();
        assertEquals(byComplaint.get(someComplaint).size(), tags.findByComplaint(someComplaint).size());
    }

    // ---------------------------------------------------------------- writes (always rolled back)

    @Test
    @DisplayName("filing a complaint: id and time come back, status starts as SUBMITTED, earning link is optional")
    void saveComplaint() throws SQLException {
        rolledBack(c -> {
            Complaint complaint = newComplaint(userId(c, "worker1@fairgig.test"), platformId(c, "Uber"));
            complaints.save(c, complaint);

            assertNotNull(complaint.getComplaintId());
            assertNotNull(complaint.getCreatedAt());
            assertEquals("SUBMITTED", scalar(c, "SELECT status FROM complaints WHERE complaint_id = ?",
                complaint.getComplaintId()));
            assertNull(scalar(c, "SELECT record_id FROM complaints WHERE complaint_id = ?",
                complaint.getComplaintId()));
        });
    }

    @Test
    @DisplayName("a complaint can be linked to an earning")
    void saveComplaintWithEarning() throws SQLException {
        rolledBack(c -> {
            int worker = userId(c, "worker1@fairgig.test");
            int record = (Integer) scalar(c, "SELECT min(record_id) FROM earnings_records WHERE worker_id = ?", worker);
            Complaint complaint = newComplaint(worker, platformId(c, "Uber"));
            complaint.setRecordId(record);
            complaints.save(c, complaint);
            assertEquals(record, scalar(c, "SELECT record_id FROM complaints WHERE complaint_id = ?",
                complaint.getComplaintId()));
        });
    }

    @Test
    @DisplayName("escalating works once and only from the status the caller saw")
    void escalate() throws SQLException {
        rolledBack(c -> {
            Complaint complaint = complaints.save(c,
                newComplaint(userId(c, "worker1@fairgig.test"), platformId(c, "Uber")));
            int id = complaint.getComplaintId();
            int advocate = userId(c, "advocate@fairgig.test");

            assertFalse(complaints.markEscalated(c, id, ComplaintStatus.RESOLVED, advocate), "wrong expected status");
            assertTrue(complaints.markEscalated(c, id, ComplaintStatus.SUBMITTED, advocate));
            assertFalse(complaints.markEscalated(c, id, ComplaintStatus.SUBMITTED, advocate), "already escalated");

            assertEquals("ESCALATED", scalar(c, "SELECT status FROM complaints WHERE complaint_id = ?", id));
            assertNotNull(scalar(c, "SELECT escalated_at FROM complaints WHERE complaint_id = ?", id));
            assertEquals(advocate, scalar(c, "SELECT handled_by_advocate_id FROM complaints WHERE complaint_id = ?", id));
        });
    }

    @Test
    @DisplayName("a representative resolving keeps the advocate who escalated it recorded")
    void resolveKeepsTheOtherHandler() throws SQLException {
        rolledBack(c -> {
            int id = complaints.save(c, newComplaint(userId(c, "worker1@fairgig.test"), platformId(c, "Uber")))
                .getComplaintId();
            int advocate = userId(c, "advocate@fairgig.test");
            int rep = userId(c, "rep.uber@fairgig.test");
            complaints.markEscalated(c, id, ComplaintStatus.SUBMITTED, advocate);

            assertTrue(complaints.markResolved(c, id, ComplaintStatus.ESCALATED, rep, null));
            assertEquals("RESOLVED", scalar(c, "SELECT status FROM complaints WHERE complaint_id = ?", id));
            assertNotNull(scalar(c, "SELECT resolved_at FROM complaints WHERE complaint_id = ?", id));
            assertEquals(rep, scalar(c, "SELECT resolved_by_rep_id FROM complaints WHERE complaint_id = ?", id));
            assertEquals(advocate, scalar(c, "SELECT handled_by_advocate_id FROM complaints WHERE complaint_id = ?", id));
            assertFalse(complaints.markResolved(c, id, ComplaintStatus.ESCALATED, rep, null), "already resolved");
        });
    }

    @Test
    @DisplayName("an advocate resolving records themselves as the handler")
    void advocateResolves() throws SQLException {
        rolledBack(c -> {
            int id = complaints.save(c, newComplaint(userId(c, "worker1@fairgig.test"), platformId(c, "Uber")))
                .getComplaintId();
            int advocate = userId(c, "advocate@fairgig.test");
            assertTrue(complaints.markResolved(c, id, ComplaintStatus.SUBMITTED, null, advocate));
            assertEquals(advocate, scalar(c, "SELECT handled_by_advocate_id FROM complaints WHERE complaint_id = ?", id));
            assertNull(scalar(c, "SELECT resolved_by_rep_id FROM complaints WHERE complaint_id = ?", id));
        });
    }

    @Test
    void storingAResponse() throws SQLException {
        rolledBack(c -> {
            int id = complaints.save(c, newComplaint(userId(c, "worker1@fairgig.test"), platformId(c, "Uber")))
                .getComplaintId();
            ComplaintResponse response = new ComplaintResponse();
            response.setComplaintId(id);
            response.setRepresentativeId(userId(c, "rep.uber@fairgig.test"));
            response.setMessage("We are looking into it.");
            complaints.saveResponse(c, response);

            assertNotNull(response.getResponseId());
            assertNotNull(response.getRespondedAt());
            assertEquals(1L, scalar(c, "SELECT count(*) FROM complaint_responses WHERE complaint_id = ?", id));
        });
    }

    @Test
    @DisplayName("a new common issue can be created and complaints put into it and taken out again")
    void clusterAssignment() throws SQLException {
        rolledBack(c -> {
            int id = complaints.save(c, newComplaint(userId(c, "worker1@fairgig.test"), platformId(c, "Uber")))
                .getComplaintId();
            ComplaintCluster cluster = new ComplaintCluster();
            cluster.setAdvocateId(userId(c, "advocate@fairgig.test"));
            cluster.setTitle("Test issue");
            clusters.save(c, cluster);
            assertNotNull(cluster.getClusterId());
            assertNotNull(cluster.getCreatedAt());

            assertTrue(complaints.assignCluster(c, id, cluster.getClusterId()));
            assertEquals(cluster.getClusterId(), scalar(c, "SELECT cluster_id FROM complaints WHERE complaint_id = ?", id));
            assertTrue(complaints.assignCluster(c, id, null));
            assertNull(scalar(c, "SELECT cluster_id FROM complaints WHERE complaint_id = ?", id));
            assertFalse(complaints.assignCluster(c, -1, cluster.getClusterId()), "unknown complaint");
        });
    }

    @Test
    @DisplayName("tagging twice is harmless, and tags can be removed")
    void tagging() throws SQLException {
        rolledBack(c -> {
            int id = complaints.save(c, newComplaint(userId(c, "worker1@fairgig.test"), platformId(c, "Uber")))
                .getComplaintId();
            ComplaintTag tag = new ComplaintTag();
            tag.setAdvocateId(userId(c, "advocate@fairgig.test"));
            tag.setName("integration-test-tag");
            tags.save(c, tag);
            assertNotNull(tag.getTagId());

            assertTrue(tags.addToComplaint(c, id, tag.getTagId()));
            assertFalse(tags.addToComplaint(c, id, tag.getTagId()), "already tagged");
            assertTrue(tags.removeFromComplaint(c, id, tag.getTagId()));
            assertFalse(tags.removeFromComplaint(c, id, tag.getTagId()), "already removed");
        });
    }

    // ---------------------------------------------------------------- the database enforces the rules too (NFR-05)

    @Test
    @DisplayName("the database refuses a blank description")
    void blankDescriptionIsRejected() throws SQLException {
        rolledBack(c -> {
            Complaint complaint = newComplaint(userId(c, "worker1@fairgig.test"), platformId(c, "Uber"));
            complaint.setDescription("   ");
            assertThrows(DataAccessException.class, () -> complaints.save(c, complaint));
        });
    }

    @Test
    @DisplayName("tag names are unique")
    void duplicateTagNameIsRejected() throws SQLException {
        rolledBack(c -> {
            ComplaintTag duplicate = new ComplaintTag();
            duplicate.setAdvocateId(userId(c, "advocate@fairgig.test"));
            duplicate.setName("commission");                 // already in the seed data
            assertThrows(DataAccessException.class, () -> tags.save(c, duplicate));
        });
    }
}
