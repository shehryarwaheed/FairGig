package com.fairgig.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.controller.AppContext;
import com.fairgig.database.DatabaseConnection;
import com.fairgig.model.Advocate;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Platform;
import com.fairgig.model.PostCategory;
import com.fairgig.model.ReportType;
import com.fairgig.model.User;
import com.fairgig.model.VerificationStatus;
import com.fairgig.model.Worker;
import com.fairgig.repository.jdbc.JdbcEarningsRepository;
import com.fairgig.repository.jdbc.JdbcPlatformRepository;
import com.fairgig.repository.jdbc.JdbcUserRepository;
import com.fairgig.security.PasswordHasher;
import com.fairgig.security.Session;
import com.fairgig.service.analytics.AdvocateAnalytics;
import com.fairgig.service.analytics.WorkerAnalytics;
import com.fairgig.service.community.CommentView;
import com.fairgig.service.community.PostView;
import com.fairgig.service.report.ReportDocument;
import com.fairgig.ui.AppShell;
import com.fairgig.ui.NavItem;
import com.fairgig.ui.UiRole;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Community, rates, analytics and reports end to end (controllers, proxy, services, PostgreSQL).
 * Creates its own users ("itc.xxxx@fairgig.test", in a city of their own) and platforms
 * ("ITCPlat-xxxx") and deletes them afterwards. Skipped when PostgreSQL is not reachable.
 */
class CommunityAnalyticsTest {

    private static final String PASSWORD = "Passw0rd1";
    private static final String HASH = PasswordHasher.hash(PASSWORD);
    private final AppContext app = AppContext.get();

    @BeforeAll
    static void start() throws SQLException {
        Assumptions.assumeTrue(DatabaseConnection.isAvailable(), "PostgreSQL is not reachable.");
        cleanUp();
    }

    @AfterAll
    static void finish() throws SQLException {
        if (DatabaseConnection.isAvailable()) {
            cleanUp();
        }
    }

    @AfterEach
    void logout() {
        Session.clear();
    }

    private static void cleanUp() throws SQLException {
        String mine = "(SELECT user_id FROM users WHERE email LIKE 'itc.%@fairgig.test')";
        try (Connection c = DatabaseConnection.getConnection(); Statement st = c.createStatement()) {
            for (String sql : new String[] {
                    "DELETE FROM posts WHERE worker_id IN " + mine,
                    "DELETE FROM rate_reports WHERE worker_id IN " + mine,
                    "DELETE FROM commission_rates WHERE recorded_by_rep_id IN " + mine,
                    "DELETE FROM reports WHERE worker_id IN " + mine,
                    "DELETE FROM earnings_records WHERE worker_id IN " + mine,
                    "DELETE FROM users WHERE email LIKE 'itc.%@fairgig.test'",
                    "DELETE FROM platforms WHERE name LIKE 'ITCPlat-%'"}) {
                st.executeUpdate(sql);
            }
        }
    }

    private static String id() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static Worker worker(String city) {
        Worker w = new Worker();
        w.setName("ITC Worker");
        w.setEmail("itc." + id() + "@fairgig.test");
        w.setPasswordHash(HASH);
        w.setCity(city);
        w.setCityZone("Zone-" + city);
        new JdbcUserRepository().save(w);
        return w;
    }

    private static Advocate advocate() {
        Advocate a = new Advocate();
        a.setName("ITC Advocate");
        a.setEmail("itc." + id() + "@fairgig.test");
        a.setPasswordHash(HASH);
        new JdbcUserRepository().save(a);
        return a;
    }

    private static Platform platform() {
        return new JdbcPlatformRepository().save(new Platform("ITCPlat-" + id(), "Test"));
    }

    private static void earning(Worker w, Platform p, LocalDate date, String net, VerificationStatus status) {
        EarningsRecord r = new EarningsRecord();
        r.setWorkerId(w.getUserId());
        r.setPlatformId(p.getPlatformId());
        r.setShiftDate(date);
        r.setHoursWorked(new BigDecimal("5"));
        r.setGrossEarnings(new BigDecimal(net));
        r.setDeductions(BigDecimal.ZERO);
        r.setNetReceived(new BigDecimal(net));
        r.setStatus(status);
        new JdbcEarningsRepository().save(r);
    }

    private User login(User user) {
        return app.auth().login(user.getEmail(), PASSWORD).value();
    }

    private static String one(String sql) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    @Test
    void postsAreAnonymousLikedOnceCommentedAndRemovedWithAReason() throws SQLException {
        Worker author = worker("ITCity-a");
        Worker other = worker("ITCity-a");
        Advocate advocate = advocate();

        login(author);
        PostView post = app.community().createPost(author.getUserId(), PostCategory.SUPPORT_REQUEST, "Title",
            "Content", null).value();
        assertNotNull(post);
        assertFalse(post.alias().contains("ITC"), "the alias says nothing about the person");
        assertTrue(Arrays.stream(PostView.class.getRecordComponents()).noneMatch(c -> c.getName().startsWith("worker")));
        assertFalse(app.community().createPost(author.getUserId(), null, "", "", null).isOk());
        assertFalse(app.community().createPost(other.getUserId(), PostCategory.COMPLAINT, "x", "y", null).isOk(),
            "nobody can post as somebody else");

        login(other);
        assertTrue(app.community().like(other.getUserId(), post.postId()).isOk());
        assertTrue(app.community().like(other.getUserId(), post.postId()).message().contains("already liked"));
        CommentView comment = app.community().addComment(other.getUserId(), post.postId(), null, "Same here").value();
        CommentView reply = app.community()
            .addComment(other.getUserId(), post.postId(), comment.commentId(), "More").value();
        assertEquals(comment.commentId(), reply.parentCommentId());
        assertEquals(comment.alias(), reply.alias(), "one alias per person within a thread");
        assertEquals(2, app.community().comments(other.getUserId(), post.postId()).value().size());
        assertFalse(app.community().removePost(other.getUserId(), post.postId(), "no").isOk(), "workers cannot moderate");

        login(advocate);
        assertFalse(app.community().removePost(advocate.getUserId(), post.postId(), "  ").isOk(), "reason required");
        assertTrue(app.community().removePost(advocate.getUserId(), post.postId(), "Off topic").isOk());
        assertTrue(app.community().board(advocate.getUserId()).value().stream()
            .noneMatch(p -> p.postId() == post.postId()));
        assertEquals("Off topic", one("SELECT removal_reason FROM posts WHERE post_id = " + post.postId()));
        assertEquals(String.valueOf(advocate.getUserId()),
            one("SELECT moderated_by FROM posts WHERE post_id = " + post.postId()));
    }

    @Test
    void sharedRatesAreAveragedPerPlatform() {
        Worker a = worker("ITCity-b");
        Worker b = worker("ITCity-b");
        Platform p = platform();
        login(a);
        assertTrue(app.community().shareRate(a.getUserId(), p.getPlatformId(), new BigDecimal("20"), null, null).isOk());
        assertFalse(app.community().shareRate(a.getUserId(), p.getPlatformId(), new BigDecimal("101"), null, null).isOk());
        login(b);
        assertTrue(app.community()
            .shareRate(b.getUserId(), p.getPlatformId(), new BigDecimal("30"), new BigDecimal("100"), null).isOk());
        var summary = app.community().aggregatedRates().value().stream()
            .filter(s -> s.platformName().equals(p.getName())).findFirst().orElseThrow();
        assertEquals(2, summary.reports());
        assertEquals(new BigDecimal("25.00"), summary.averageCommission());
        assertEquals(new BigDecimal("100.00"), summary.averageIncentive());
    }

    @Test
    void analyticsReportsAndCertificate() throws SQLException {
        String city = "ITCity-" + id();
        Worker me = worker(city);
        Worker second = worker(city);
        Worker third = worker(city);
        Platform p = platform();
        LocalDate day = LocalDate.now().minusDays(1);
        earning(me, p, day, "1000", VerificationStatus.VERIFIED);
        earning(me, p, day, "500", VerificationStatus.PENDING);
        earning(second, p, day, "2000", VerificationStatus.PENDING);
        earning(third, p, day, "4000", VerificationStatus.PENDING);

        login(me);
        WorkerAnalytics mine = app.analytics().myAnalytics(me.getUserId(), null, null).value();
        assertEquals(new BigDecimal("1500.00"), mine.summary().totalNet());
        assertEquals(new BigDecimal("2000.00"), mine.comparison().medianNet());
        assertFalse(app.analytics().myAnalytics(second.getUserId(), null, null).isOk());
        assertFalse(app.analytics().advocateOverview().isOk(), "advocates only");

        ReportDocument month = app.analytics().monthly(me.getUserId(), YearMonth.from(day)).value();
        assertEquals(new BigDecimal("1500.00"), month.report().totalNet());
        assertTrue(month.csv().startsWith("platform,date"));
        ReportDocument certificate = app.analytics().incomeCertificate(me.getUserId(), day, day).value();
        assertEquals(ReportType.INCOME_CERTIFICATE, certificate.report().type());
        assertEquals(new BigDecimal("1000.00"), certificate.report().totalNet());
        assertFalse(certificate.text().contains("500.00"), "unverified money never appears on a certificate");
        assertEquals("1", one("SELECT count(*) FROM report_earnings WHERE report_id = "
            + certificate.report().reportId()));
        assertEquals(2, app.analytics().myReports(me.getUserId()).value().size());

        login(second);
        assertTrue(app.analytics().incomeCertificate(second.getUserId(), day, day).message().contains("VERIFIED"));

        Advocate advocate = advocate();
        login(advocate);
        AdvocateAnalytics overview = app.analytics().advocateOverview().value();
        assertTrue(overview.incomeByZone().stream().anyMatch(z -> z.city().equals(city) && z.workers() == 3));
        assertNotNull(overview.complaintCounts());
        assertNotNull(overview.commissionTrends());
    }

    @Test
    void theEarningFormShowsTheCommissionRateAndLeavesDeductionsToTheWorker() throws Exception {
        Worker me = worker("ITCity-rate");
        Platform rated = platform();
        Platform unrated = platform();
        var workerPlatforms = new com.fairgig.repository.jdbc.JdbcWorkerPlatformRepository();
        workerPlatforms.add(new com.fairgig.model.WorkerPlatform(me.getUserId(), rated.getPlatformId(), null, null));
        workerPlatforms.add(new com.fairgig.model.WorkerPlatform(me.getUserId(), unrated.getPlatformId(), null, null));
        com.fairgig.model.PlatformRepresentative rep = new com.fairgig.model.PlatformRepresentative();
        rep.setName("ITC Rep");
        rep.setEmail("itc." + id() + "@fairgig.test");
        rep.setPasswordHash(HASH);
        rep.setPlatformId(rated.getPlatformId());
        new JdbcUserRepository().save(rep);
        login(rep);
        assertTrue(app.commission().recordRate(rep.getUserId(), rated.getPlatformId(), new BigDecimal("27"),
            LocalDate.now().minusDays(10)).isOk());
        User worker = login(me);

        startToolkit();
        CompletableFuture<Void> done = new CompletableFuture<>();
        javafx.application.Platform.runLater(() -> {
            try {
                var context = new com.fairgig.ui.PageContext(worker, UiRole.WORKER, app, null);
                var page = new com.fairgig.ui.worker.WorkerEarningsPage(context);
                page.build();
                @SuppressWarnings("unchecked")
                var chips = (com.fairgig.ui.ChipGroup<com.fairgig.service.platform.WorkerPlatformInfo>)
                    field(page, "platformChips");
                var hint = (javafx.scene.control.Label) field(page, "rateHint");
                var gross = (com.fairgig.ui.FormField) field(page, "gross");
                var deductions = (com.fairgig.ui.FormField) field(page, "deductions");
                var date = (com.fairgig.ui.FormField) field(page, "date");

                chips.select(chips.items().stream()
                    .filter(i -> i.platform().getName().equals(rated.getName())).findFirst().orElseThrow());
                gross.setText("13000");
                assertTrue(hint.getText().contains("27%"), hint.getText());
                assertTrue(hint.getText().contains("3,510.00"), hint.getText());
                assertEquals("", deductions.text(), "the app never fills the deductions in");

                date.setText(LocalDate.now().minusDays(20).toString());
                assertTrue(hint.getText().contains("0%"), "before the rate started: " + hint.getText());

                date.setText(LocalDate.now().toString());
                chips.select(chips.items().stream()
                    .filter(i -> i.platform().getName().equals(unrated.getName())).findFirst().orElseThrow());
                assertTrue(hint.getText().contains("0% (no rate recorded"), hint.getText());
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        done.get(60, TimeUnit.SECONDS);
    }

    private static void startToolkit() {
        try {
            javafx.application.Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // another test started the toolkit
        } catch (Throwable notAvailable) {
            Assumptions.abort("JavaFX is not available: " + notAvailable);
        }
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        var f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    @Test
    void theNewTabsBuildForWorkersAndAdvocates() throws Exception {
        try {
            javafx.application.Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // another test started the toolkit
        } catch (Throwable notAvailable) {
            Assumptions.abort("JavaFX is not available: " + notAvailable);
        }
        for (User user : new User[] {worker("ITCity-ui"), advocate()}) {
            User loggedIn = login(user);
            CompletableFuture<Void> done = new CompletableFuture<>();
            javafx.application.Platform.runLater(() -> {
                try {
                    AppShell shell = new AppShell(loggedIn, () -> { });
                    for (NavItem item : UiRole.of(loggedIn.getRole()).nav()) {
                        shell.open(item);
                    }
                    shell.dispose();
                    done.complete(null);
                } catch (Throwable t) {
                    done.completeExceptionally(t);
                }
            });
            done.get(60, TimeUnit.SECONDS);
        }
    }
}
