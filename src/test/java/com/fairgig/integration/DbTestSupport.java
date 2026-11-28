package com.fairgig.integration;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.Advocate;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Platform;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.model.WorkerPlatform;
import com.fairgig.repository.jdbc.JdbcCsvImportRepository;
import com.fairgig.repository.jdbc.JdbcEarningsRepository;
import com.fairgig.repository.jdbc.JdbcEvidenceRepository;
import com.fairgig.repository.jdbc.JdbcNotificationRepository;
import com.fairgig.repository.jdbc.JdbcPlatformRepository;
import com.fairgig.repository.jdbc.JdbcUserRepository;
import com.fairgig.repository.jdbc.JdbcWorkerPlatformRepository;
import com.fairgig.security.PasswordHasher;
import com.fairgig.security.Session;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class for tests that use the real PostgreSQL database (integration tests, NFR-13).
 * <ul>
 *   <li>If the database cannot be reached the tests are SKIPPED, not failed.</li>
 *   <li>Every test creates its OWN data with a unique name: users with an email like
 *       {@code it.xxxx@fairgig.test} and platforms named {@code ITPlat-xxxx}. The seed data is never touched.</li>
 *   <li>All such data is deleted before and after each test class (even after an earlier crash).</li>
 * </ul>
 * Teammates: if your tests create other rows (complaints, posts, ...), add their clean-up to {@link #cleanUp()}.
 */
public abstract class DbTestSupport {

    protected static final JdbcUserRepository users = new JdbcUserRepository();
    protected static final JdbcPlatformRepository platforms = new JdbcPlatformRepository();
    protected static final JdbcWorkerPlatformRepository workerPlatforms = new JdbcWorkerPlatformRepository();
    protected static final JdbcEarningsRepository earnings = new JdbcEarningsRepository();
    protected static final JdbcEvidenceRepository evidence = new JdbcEvidenceRepository();
    protected static final JdbcCsvImportRepository csvImports = new JdbcCsvImportRepository();
    protected static final JdbcNotificationRepository notifications = new JdbcNotificationRepository();

    /** The password of every test user. */
    protected static final String PASSWORD = "Passw0rd1";
    private static final String PASSWORD_HASH = PasswordHasher.hash(PASSWORD);

    @BeforeAll
    static void requireDatabaseAndStartClean() {
        Assumptions.assumeTrue(DatabaseConnection.isAvailable(),
            "PostgreSQL is not reachable (check .env): skipping database tests.");
        cleanUp();
    }

    @AfterAll
    static void removeTestData() {
        if (DatabaseConnection.isAvailable()) {
            cleanUp();
        }
    }

    @AfterEach
    void logoutAfterEachTest() {
        Session.clear();
    }

    // ---------------------------------------------------------------- unique data

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** An email that the clean-up will find. */
    protected static String testEmail() {
        return "it." + UUID.randomUUID().toString().substring(0, 8) + "@fairgig.test";
    }

    protected static Worker newWorker() {
        Worker w = new Worker();
        w.setName("IT Worker");
        w.setEmail(testEmail());
        w.setPasswordHash(PASSWORD_HASH);
        w.setCity("Lahore");
        w.setCityZone("Gulberg");
        users.save(w);
        return w;
    }

    protected static PlatformRepresentative newRepresentative(int platformId) {
        PlatformRepresentative r = new PlatformRepresentative();
        r.setName("IT Representative");
        r.setEmail(testEmail());
        r.setPasswordHash(PASSWORD_HASH);
        r.setPlatformId(platformId);
        users.save(r);
        return r;
    }

    protected static Advocate newAdvocate() {
        Advocate a = new Advocate();
        a.setName("IT Advocate");
        a.setEmail(testEmail());
        a.setPasswordHash(PASSWORD_HASH);
        users.save(a);
        return a;
    }

    protected static Platform newPlatform() {
        return platforms.save(new Platform(unique("ITPlat"), "Test"));
    }

    /** Registers the worker on the platform. */
    protected static void join(Worker worker, Platform platform) {
        workerPlatforms.add(new WorkerPlatform(worker.getUserId(), platform.getPlatformId(), null, null));
    }

    protected static EarningsRecord newEarning(Worker worker, Platform platform, LocalDate date, String net, VerificationStatus status) {
        EarningsRecord r = new EarningsRecord();
        r.setWorkerId(worker.getUserId());
        r.setPlatformId(platform.getPlatformId());
        r.setShiftDate(date);
        r.setHoursWorked(new BigDecimal("5"));
        r.setGrossEarnings(new BigDecimal("100"));
        r.setDeductions(new BigDecimal("20"));
        r.setNetReceived(new BigDecimal(net));
        r.setStatus(status);
        return earnings.save(r);
    }

    // ---------------------------------------------------------------- clean-up

    /** Deletes everything the tests created (found by the "it." email and "ITPlat-" name patterns). */
    protected static void cleanUp() {
        String testUsers = "SELECT user_id FROM users WHERE email LIKE 'it.%@fairgig.test'";
        try (Connection c = DatabaseConnection.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM notifications WHERE message LIKE 'IT-%'");   // broadcast test messages
            st.executeUpdate("DELETE FROM earnings_records WHERE worker_id IN (" + testUsers + ")");
            st.executeUpdate("DELETE FROM csv_imports WHERE worker_id IN (" + testUsers + ")");
            st.executeUpdate("DELETE FROM users WHERE email LIKE 'it.%@fairgig.test'");
            st.executeUpdate("DELETE FROM platforms WHERE name LIKE 'ITPlat-%'");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not clean up test data: " + e.getMessage(), e);
        }
    }
}
