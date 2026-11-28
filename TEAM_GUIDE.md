# FairGig Team Guide

For the whole team, and for any AI assistant helping them.
All three members' features are built and merged into `master`. This guide explains how to set up,
how the code is layered, how to add or change a feature, and who built what.

---

## 1. First-time setup (10 minutes)

1. `git pull` the latest code.
2. Create an empty PostgreSQL database named `fairgig_db` (pgAdmin).
3. Copy `.env.example` to `.env` and put your own DB password in it (`.env` is git-ignored).
4. Run `src/main/resources/sql/schema.sql`, then `src/main/resources/sql/seed.sql` (once each, on the empty database).
5. In IntelliJ open `pom.xml`, let Maven import, run `src/main/java/com/fairgig/app/Launcher.java`.
6. Check everything: `mvn test` (runs 321 tests; database tests are skipped automatically if PostgreSQL is not reachable).

Sample logins (password `Test@1234`): `worker1@fairgig.test`, `worker2@fairgig.test`, `worker3@fairgig.test`,
`rep.uber@fairgig.test` (Uber), `rep.foodpanda@fairgig.test` (Foodpanda), `advocate@fairgig.test`.
The seed already contains sample complaints, a cluster, tags, community posts, commission rates and shared rates.

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

## 5. Who built what

Use this to find code, and to know who explains which part.

### Muhammad Shehryar Waheed (Member A)

Foundation: PostgreSQL schema (24 tables) and seed data, connection and transactions, repository pattern (JDBC),
password security (BCrypt), Factory Method for users, Proxy access control, Strategy for earning validation,
controller layer (`Outcome`, `AppContext`), theme and app shell.

Features (FR-01 to FR-14, FR-41, FR-42): accounts and profile, role and platform restrictions, platform registration,
manual earnings with validation, CSV import with an error report, evidence upload, notifications.

### Aqsa Ehtesham (Member B)

| Feature | FRs | Main code |
|---|---|---|
| Earnings history, status and filters; verification workflow (State pattern) | FR-15 to FR-22 | `service/earnings`, `service/verification`, `state/verification` |
| Commission rates and sudden-change highlighting | FR-26, FR-27 | `service/commission` |
| Complaints, tags, issue clusters, escalation | FR-30 to FR-33 | `service/complaint`, `state/complaint` |

### Zainab Khurram (Member C)

| Feature | FRs | Main code |
|---|---|---|
| Worker analytics and city median (Strategy pattern) | FR-23 to FR-25 | `service/analytics`, `strategy/analytics` |
| Earnings reports and income certificate (Factory Method) | FR-28, FR-29 | `service/report`, `factory/report` |
| Community board, moderation, shared rates | FR-34 to FR-38 | `service/community` |
| Commission trends, city-zone income, complaint categories | FR-39, FR-40 | `service/analytics`, `ui/advocate` |

## 6. Screens for each role

* **Worker** (`ui/worker`): Home, Platforms, Earnings, Import CSV, Evidence, History, Commission History,
  Complaints, Analytics, Reports, Community, Rate Insights
* **Representative** (`ui/verifier`): Home, Pending Reviews, Review History, Commission Rates, Complaints
* **Advocate** (`ui/advocate`): Home, Complaints, Issue Clusters, Moderation, Analytics, Commission Trends
* Every role also has Notifications and Profile.

## 7. Before the demo

* Run `mvn test` on the latest `master`.
* Click through each role by hand: the tests build screens without a window, so they do not check how a screen looks.
* Try printing a report and an income certificate on the demo computer.

## 8. Rules of the road

* Never hide a function only in the UI: put the restriction in the service interface (`@Access`...).
* Never build SQL with string concatenation; never store or log passwords or hashes.
* Show clear messages (what is wrong and how to fix it); never crash: use `Outcome`.
* Before changing another member's code, tell them. Wiring goes in `AppContext`, new tabs get a `case` in `PageFactory`.
* Run `mvn test` before every commit. Do not commit `.env` or anything in `uploads/`.

## 9. For AI assistants (paste this into your own `CLAUDE.md`)

> Project FairGig: Java 17, JavaFX, PostgreSQL via JDBC only, Maven, JUnit 5. Read `TEAM_GUIDE.md` first.
> Follow the layering ui -> controller (Outcome) -> proxy-guarded service interface -> repository interface -> jdbc.
> New services are annotated interfaces (`@Access`, `@Self`, `@OwnPlatform`) wrapped with `AccessProxy` in `AppContext`.
> Use PreparedStatement only. Use `TransactionManager` for multi-step writes. Use `NotificationSender` for FR-41.
> Add tests (unit and `DbTestSupport` integration). Ask the user before destructive actions; never commit or push unless the user asks.
