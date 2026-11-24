package com.fairgig.controller;

import com.fairgig.database.DatabaseConnection;
import com.fairgig.proxy.AccessProxy;
import com.fairgig.repository.CsvImportRepository;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.EvidenceRepository;
import com.fairgig.repository.NotificationRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.ReviewRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.repository.WorkerPlatformRepository;
import com.fairgig.repository.jdbc.JdbcCsvImportRepository;
import com.fairgig.repository.jdbc.JdbcEarningsRepository;
import com.fairgig.repository.jdbc.JdbcEvidenceRepository;
import com.fairgig.repository.jdbc.JdbcNotificationRepository;
import com.fairgig.repository.jdbc.JdbcPlatformRepository;
import com.fairgig.repository.jdbc.JdbcReviewRepository;
import com.fairgig.repository.jdbc.JdbcUserRepository;
import com.fairgig.repository.jdbc.JdbcWorkerPlatformRepository;
import com.fairgig.service.auth.AuthService;
import com.fairgig.service.auth.DefaultAuthService;
import com.fairgig.service.csv.CsvImportService;
import com.fairgig.service.csv.DefaultCsvImportService;
import com.fairgig.service.earnings.DefaultEarningsHistoryService;
import com.fairgig.service.earnings.DefaultEarningsService;
import com.fairgig.service.earnings.EarningsHistoryService;
import com.fairgig.service.earnings.EarningsService;
import com.fairgig.service.evidence.DefaultEvidenceService;
import com.fairgig.service.evidence.EvidenceService;
import com.fairgig.service.notification.DefaultNotificationService;
import com.fairgig.service.notification.NotificationSender;
import com.fairgig.service.notification.NotificationService;
import com.fairgig.service.platform.DefaultPlatformService;
import com.fairgig.service.platform.PlatformService;
import com.fairgig.service.verification.DefaultVerificationService;
import com.fairgig.service.verification.VerificationService;
import com.fairgig.storage.EvidenceStorage;
import com.fairgig.storage.LocalEvidenceStorage;

/**
 * The ONE place where repositories, services, proxies and controllers are put together
 * ("composition root"). Screens ask for a controller here and never build anything themselves.
 *
 * <p><b>Teammates:</b> to add your feature, create your repositories/services in the marked
 * section of the constructor, wrap each service with {@code AccessProxy.wrap(...)}, build your
 * controller from the proxied services, and add a getter.
 */
public final class AppContext {

    private static AppContext instance;

    // ---- repositories (real PostgreSQL implementations)
    private final UserRepository users = new JdbcUserRepository();
    private final PlatformRepository platformRepository = new JdbcPlatformRepository();
    private final WorkerPlatformRepository workerPlatformRepository = new JdbcWorkerPlatformRepository();
    private final EarningsRepository earningsRepository = new JdbcEarningsRepository();
    private final EvidenceRepository evidenceRepository = new JdbcEvidenceRepository();
    private final CsvImportRepository csvImportRepository = new JdbcCsvImportRepository();
    private final NotificationRepository notificationRepository = new JdbcNotificationRepository();
    private final ReviewRepository reviewRepository = new JdbcReviewRepository();
    private final EvidenceStorage storage = LocalEvidenceStorage.fromEnv();

    // ---- internal helpers (not proxied: only trusted service code uses them)
    private final DefaultNotificationService notificationService =
        new DefaultNotificationService(notificationRepository, users);
    private final NotificationSender notificationSender = notificationService;

    // ---- proxied services: every call is checked for role / own account / own platform
    private final AuthService auth;
    private final PlatformService platforms;
    private final EarningsService earnings;
    private final CsvImportService csv;
    private final EvidenceService evidence;
    private final NotificationService notifications;
    private final VerificationService verification;
    private final EarningsHistoryService history;

    // ---- controllers (what the screens use)
    private final AuthController authController;
    private final WorkerController workerController;
    private final NotificationController notificationController;
    private final VerificationController verificationController;
    private final EarningsHistoryController historyController;

    private AppContext() {
        auth = AccessProxy.wrap(AuthService.class, new DefaultAuthService(users, platformRepository));
        platforms = AccessProxy.wrap(PlatformService.class,
            new DefaultPlatformService(platformRepository, workerPlatformRepository));
        earnings = AccessProxy.wrap(EarningsService.class,
            new DefaultEarningsService(earningsRepository, platformRepository, workerPlatformRepository));
        csv = AccessProxy.wrap(CsvImportService.class,
            new DefaultCsvImportService(earningsRepository, csvImportRepository, platformRepository, workerPlatformRepository));
        evidence = AccessProxy.wrap(EvidenceService.class,
            new DefaultEvidenceService(evidenceRepository, earningsRepository, platformRepository, users,
                storage, notificationSender));
        notifications = AccessProxy.wrap(NotificationService.class, notificationService);

        authController = new AuthController(auth, platforms);
        workerController = new WorkerController(platforms, earnings, csv, evidence);
        notificationController = new NotificationController(notifications);

        // ===== Add the verification / complaints / community / analytics features below =====

        // verification (FR-18 to FR-21)
        verification = AccessProxy.wrap(VerificationService.class,
            new DefaultVerificationService(earningsRepository, reviewRepository, evidenceRepository, users,
                platformRepository, notificationSender));
        verificationController = new VerificationController(verification, evidence);

        // worker earnings history with status and filters (FR-15, FR-16)
        history = AccessProxy.wrap(EarningsHistoryService.class,
            new DefaultEarningsHistoryService(earningsRepository, reviewRepository, platformRepository));
        historyController = new EarningsHistoryController(history);
    }

    /** The shared instance (created on first use). */
    public static synchronized AppContext get() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    /** True when the database can be reached with the settings in .env. */
    public static boolean databaseAvailable() {
        return DatabaseConnection.isAvailable();
    }

    public AuthController auth() {
        return authController;
    }

    public WorkerController worker() {
        return workerController;
    }

    public NotificationController notifications() {
        return notificationController;
    }

    public VerificationController verification() {
        return verificationController;
    }

    public EarningsHistoryController history() {
        return historyController;
    }

    /** For other services that need to send notifications (FR-41). Internal use only. */
    public NotificationSender notificationSender() {
        return notificationSender;
    }
}
