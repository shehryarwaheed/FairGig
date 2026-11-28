# FairGig Team Guide

For **Aqsa (Member B)** and **Zainab (Member C)**, and for any AI assistant helping them.
Shehryar (Member A) finished the foundation and his own features. This guide tells you what exists,
how to plug your features in, and exactly what is still yours to build.

> **Working order:** Shehryar -> **Aqsa** -> **Zainab**.
> Zainab's commission-trend and complaint-category analytics read data that Aqsa's features create,
> so Aqsa goes first.

---

## 1. First-time setup (10 minutes)

1. `git pull` the latest code.
2. Create an empty PostgreSQL database named `fairgig_db` (pgAdmin).
3. Copy `.env.example` to `.env` and put your own DB password in it (`.env` is git-ignored).
4. Run `src/main/resources/sql/schema.sql`, then `src/main/resources/sql/seed.sql` (once each, on the empty database).
5. In IntelliJ open `pom.xml`, let Maven import, run `src/main/java/com/fairgig/app/Launcher.java`.
6. Check everything: `mvn test` (runs about 154 tests; database tests are skipped automatically if PostgreSQL is not reachable).

Sample logins (password `Test@1234`): `worker1@fairgig.test`, `worker2@fairgig.test`, `worker3@fairgig.test`,
`rep.uber@fairgig.test` (Uber), `rep.foodpanda@fairgig.test` (Foodpanda), `advocate@fairgig.test`.
The seed already contains sample complaints, a cluster, tags, community posts, commission rates and shared rates
that your features can read right away.

## 2. How the code is layered

```
ui (JavaFX pages)  ->  controller (returns Outcome)  ->  proxy (role / own-account / own-platform checks)
      ->  service (rules)  ->  repository (interface)  ->  repository.jdbc (PostgreSQL)  ->  database
shared:  model, util, security, storage, factory.*, strategy.*
```

* Screens talk ONLY to controllers. Controllers return `Outcome<T>` (a value, or friendly messages) and never throw.
* Every service is an **interface with annotations**, wrapped by `AccessProxy`. A method without `@Access` is denied.
* Everything is wired in ONE place: `controller/AppContext.java` (there is a marked spot at the end of its constructor).

## 3. Recipe: adding a feature (example: "Commission Rates")

1. **Model**: add the entity (and enums such as `ComplaintStatus`) in `model/`. Money is `BigDecimal`, ids are `Integer`.
2. **Repository**: interface in `repository/` (+ methods that take a `Connection` if they must join a transaction),
   implementation in `repository/jdbc/` using the package-private `Jdbc` helper
   (`queryList`, `queryOne`, `update`; `INSERT ... RETURNING id`). Only `PreparedStatement` with `?`, never string-built SQL.
3. **Service**: `XService` interface with annotations, plus `DefaultXService`:
   ```java
   public interface CommissionService {
       @Access(roles = Role.VERIFIER)
       CommissionRate recordRate(@OwnPlatform int platformId, BigDecimal percent, LocalDate effectiveDate);
       @Access
       List<CommissionRate> history(@OwnPlatform int platformId);
   }
   ```
   `@Access(anyone = true)` = guests too, `@Access` = any logged-in user, `@Access(roles = {...})` = those roles.
   `@Self` on a user-id parameter = only your own id. `@OwnPlatform` on a platform-id parameter = representatives
   can only use their own platform. When the platform is only known after loading a record, call
   `AccessPolicy.requirePlatform(record.getPlatformId())` inside the service.
   Throw `ValidationException` (messages the user can act on), `NotFoundException`, `AccessDeniedException`.
4. **Wire it** in `AppContext`: create the repository/service, wrap with `AccessProxy.wrap(XService.class, new DefaultXService(...))`,
   create your controller, add a getter.
5. **Controller**: one method per action returning `Outcome` (see `WorkerController`).
6. **Page**: build a page class (see `ui/worker/WorkerEarningsPage.java` as the model), then add one `case` for your
   tab title in `ui/PageFactory.java`. Tab titles are listed in `ui/worker/WorkerPages`, `ui/verifier/VerifierPages`,
   `ui/advocate/AdvocatePages` (rename or add tabs there if you need to).
7. **Tests**: JUnit 5. Unit tests in `src/test/java/com/fairgig/unit`; database tests in `.../integration` extending
   `DbTestSupport` (it creates uniquely named test data and deletes it afterwards; if your tests create other rows,
   add their `DELETE` to `DbTestSupport.cleanUp()`).

## 4. Ready-made building blocks you can use

| Need | Use |
|---|---|
| Notify someone (FR-41) | `AppContext.get().notificationSender()` -> `notifyUser(...)`, `notifyPlatformRepresentatives(platformId, ...)`, `notifyAdvocates(...)`; Connection variants join your transaction. Wording in `NotificationMessages` (`recordReviewed`, `complaintFiled`, `complaintAnswered`, `complaintResolved`) |
| Several DB steps that must all succeed | `TransactionManager.inTransaction(connection -> { ... })` |
| Validation rules (Strategy pattern) | `strategy/validation` (`ValidationStrategy<T>`, `CompositeValidation`, `ValidationResult`). Add your own strategies the same way |
| Who is logged in | `Session.currentUser()`, `AccessPolicy.requireLogin()/requireRole(...)` |
| Evidence files of an earning | `EvidenceService.listEvidence(recordId)` / `readEvidence(evidenceId)` (workers: own; representatives: own platform) |
| Show money in the chosen currency | `Fmt.money(amount)` (label bound to the currency picker) |
| UI building blocks | `FormField`, `Banner`, `ChipGroup`, `SimpleTable`, `Fmt.statusChip`, `Pages.section/header/scroll`, `Widgets.*`, `Illustrations.emptyState` |
| Earnings of a worker | `EarningsRepository.findByWorker(workerId, EarningsFilter)`; `EarningsRecord.calculateEffectiveRate()` |

Database facts worth knowing: a Verified earning can never change status again (a trigger enforces it, FR-22);
`complaints.status` is `SUBMITTED / ESCALATED / RESOLVED`; `posts` keep only an anonymous alias for other workers (NFR-04);
all tables and columns are in `schema.sql`.

## 5. What Shehryar already finished (do not redo)

Foundation: PostgreSQL schema (24 tables) and seed data, connection and transactions, models, repository pattern (JDBC),
password security (BCrypt), Factory Method for users, Proxy access control, Strategy for earning validation,
controller layer (`Outcome`, `AppContext`), the whole theme and app shell, and these features:

* Register an account with a role; a representative selects their assigned platform
* Log in and log out; view and update your profile; change your password
* Role and platform restrictions on every function (server side)
* Register and manage the platforms a worker works on; add a platform that is not in the system yet
* Log an earning manually with validation (starts as Pending); import earnings from a CSV file with a row-by-row error report
* Upload a screenshot or payment statement as evidence (PNG, JPG, PDF up to 5 MB); the platform's representative is notified
* View your own notifications; mark them read; the notification bell with unread count
* Notification sender used by all features (FR-41)

## 6. Aqsa (Member B): what you build

**Verification workflow**
1. Worker views the verification status of each earning (Pending, Verified, Disputed/Flagged, Unverifiable). (FR-15)
2. Worker views their earnings history and filters it by platform, date range and status. (FR-16)
3. Worker re-uploads evidence for a Flagged or Unverifiable earning, which returns it to Pending. (FR-17, extra)
4. Representative views the pending earnings of their platform. (FR-18, extra)
5. Representative views an earning's evidence next to the submitted figures. (FR-19)
6. Representative marks an earning Verified, Disputed/Flagged or Unverifiable. (FR-20)
7. Flagging requires an explanation, and every decision is stored with the reviewer and the time. (FR-21)
8. The system enforces the workflow transitions with the **State pattern**; a Verified record never returns to Pending. (FR-22)

**Commission rates**
9. The system shows the commission rate history of each platform and highlights sudden changes. (FR-26)
10. Representative records a platform's commission rate with its effective date. (FR-27, extra)

**Complaints (grievance management)**
11. Worker files a complaint by choosing a platform, a category and a description, optionally linked to an earning. (FR-30)
12. Worker tracks the status of a complaint (Submitted, Escalated, Resolved). (FR-31)
13. Representative views, responds to and resolves the complaints of their platform. (FR-32)
14. Advocate views all complaints, tags them, groups similar ones into a common issue (cluster), and escalates or resolves them. (FR-33)

**Notifications you must trigger (FR-41):** representative when a complaint is filed; worker when an earning is reviewed;
worker when a complaint is answered or resolved.

**Also yours:** models and repositories for `verification_reviews`, `commission_rates`, `complaints`, `complaint_responses`,
`complaint_clusters`, `complaint_tags`, `complaint_tag_map` (enums `ReviewDecision`, `ComplaintStatus`, `ComplaintCategory`);
the representative tabs (Pending Reviews, Review History, Commission Rates, Complaints), the worker tabs (Earnings history
and status, Complaints), the advocate tabs (Complaints, Issue Clusters); JUnit unit tests for the verification workflow (NFR-13).

**Hooks Shehryar left for you:** `EarningsRepository` has no `updateStatus(...)` and no `findPendingByPlatform(...)` yet: add them.
Re-submitting = call `EvidenceService.uploadEvidence(...)` first, then move the earning back to Pending in your verification service.
The current worker "Earnings" tab already lists the latest entries with status chips: extend it with the full history and filters.

## 7. Zainab (Member C): what you build (after Aqsa)

**Analytics and reports**
1. The system shows a worker's total income, total deductions and effective hourly rate. (FR-23)
2. The system compares income across platforms and shows weekly and monthly trends. (FR-24)
3. The system compares the worker's earnings with the anonymized city-wide median. (FR-25)
4. The system generates monthly and yearly earnings reports with deductions and hours, and lets the worker print or export them. (FR-28)
5. The system generates a printable income certificate for a date range showing verified earnings. (FR-29)

**Community board and rate intelligence**
6. Worker creates a post anonymously, with a category. (FR-34)
7. Workers comment on posts, reply to comments and like a post once. (FR-35)
8. Advocate removes inappropriate posts and records the reason. (FR-36)
9. Worker shares platform rates, incentives and deductions. (FR-37)
10. The system shows aggregated, anonymized rates per platform. (FR-38)

**Advocate analytics**
11. Advocate views commission trends across platforms. (FR-39, uses Aqsa's commission rates)
12. Advocate views income distribution by city zone and the most common complaint categories. (FR-40, uses Aqsa's complaints)

**Patterns:** Strategy for analytics (`strategy/analytics`) and Factory Method for reports (`factory/report`).
**Also yours:** models and repositories for `posts`, `comments`, `likes`, `rate_reports`, `reports`, `report_earnings`
(enums `PostCategory`, `ReportType`); the worker tabs (Analytics, Reports, Community, Rate Insights), the advocate tabs
(Moderation, Analytics, Commission Trends); unit tests for the analytics strategies (NFR-13) and the final demo run-through.

## 8. Rules of the road

* Never hide a function only in the UI: put the restriction in the service interface (`@Access`...).
* Never build SQL with string concatenation; never store or log passwords or hashes.
* Show clear messages (what is wrong and how to fix it); never crash: use `Outcome`.
* Keep your changes inside your own packages and your own tabs; add your wiring in `AppContext` and your `case` in `PageFactory`.
* Run `mvn test` before every commit. Do not commit `.env` or anything in `uploads/`.

## 9. For AI assistants (paste this into your own `CLAUDE.md`)

> Project FairGig: Java 17, JavaFX, PostgreSQL via JDBC only, Maven, JUnit 5. Read `TEAM_GUIDE.md` first.
> Follow the layering ui -> controller (Outcome) -> proxy-guarded service interface -> repository interface -> jdbc.
> New services are annotated interfaces (`@Access`, `@Self`, `@OwnPlatform`) wrapped with `AccessProxy` in `AppContext`.
> Use PreparedStatement only. Use `TransactionManager` for multi-step writes. Use `NotificationSender` for FR-41.
> Add tests (unit and `DbTestSupport` integration). Ask the user before destructive actions; never commit or push unless the user asks.
