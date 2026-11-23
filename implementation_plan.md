# 🚀 FairGig — Implementation Plan

**Team:** Muhammad Shehryar Waheed (Lead) · Aqsa Ehtesham · Zainab Khurram
**Deadline:** 27 Nov 2026 (full working application) · **Plan date:** 23 Nov 2026 → **4 working days**
**Stack (decided):** Java 17 · JavaFX desktop UI · PostgreSQL · JDBC only · Maven · JUnit 5 · BCrypt

---

## 1. What FairGig does (workflow in plain words)

Gig workers (riders, drivers, delivery partners) often don't know if a platform deducted fairly. FairGig lets them **record earnings, back them with evidence, get them verified by the platform's representative, and see analytics**, while an **advocate** watches the big picture (complaints, commission trends). FairGig **never handles money** (NFR-15).

### The 4 roles
| Role | What they do |
|---|---|
| Guest | Register or log in |
| Worker | Platforms, log earnings (manual/CSV), evidence, analytics, reports, complaints, anonymous community, share rates |
| Platform Representative (Verifier) | Tied to ONE platform; reviews pending earnings + evidence, records commission rates, answers complaints |
| Advocate | Moderates posts, tags/groups/escalates complaints, sees cross-platform analytics |

### Main flow 1 — Earning verification (the heart of the app)
```
Worker logs earning (manual or CSV)  ──►  status = PENDING
Worker uploads evidence (PNG/JPG/PDF ≤ 5 MB)  ──►  Representative is notified
Representative opens earning + evidence side by side
   ├─► VERIFIED         (final — can never go back to Pending)
   ├─► DISPUTED/FLAGGED (explanation REQUIRED)
   └─► UNVERIFIABLE
Decision saved with reviewer + timestamp  ──►  Worker is notified
Worker may re-upload evidence for FLAGGED / UNVERIFIABLE  ──►  back to PENDING
```
### Main flow 2 — Complaints
`Worker files complaint (platform + category + description, optional earning)` → **SUBMITTED** → Representative responds / resolves (own platform only) → Advocate tags, groups similar ones into a *cluster*, **ESCALATES** or **RESOLVES** → worker notified at each step.

### Main flow 3 — Analytics & reports
Total income / deductions / effective hourly rate → compare platforms → weekly & monthly trends → compare to anonymized city-wide median → commission-rate history with sudden-change highlight → monthly/yearly report + printable income certificate (verified earnings only).

### Main flow 4 — Community
Anonymous posts (alias only; real identity visible to system only) · comments, replies, one like per post · advocate removes posts with a reason · workers share rates → aggregated, anonymized rates per platform.

---

## 2. Architecture (from the package diagram)

```
app (bootstrap)
 └─ ui.auth | ui.worker | ui.verifier | ui.advocate      (JavaFX screens)
     └─ controller
         └─ proxy  (Proxy pattern: role + platform checks, server-side — NFR-03)  + security (BCrypt)
             └─ service.*  (12 sub-packages)
                 └─ repository (interfaces) ◄─ repository.jdbc (JDBC impl) ─ database ─ PostgreSQL (24 tables)
                 └─ storage (evidence files on disk)
 shared: model · util · factory.report · factory.user · strategy.analytics · strategy.validation
 tests:  test.unit · test.integration (JUnit)
```
**Services:** auth, platform, earnings, csv, evidence, verification, notification, analytics, report, commission, complaint, community.

**5 required patterns & who builds them (NFR-12):**
| Pattern | Where | Owner |
|---|---|---|
| Repository | `repository` + `repository.jdbc` | Shehryar |
| Proxy | `proxy` (role/platform guard) | Shehryar |
| Factory Method | `factory.user` (Shehryar) · `factory.report` (Zainab) | both |
| State | Verification workflow (Pending→Verified/Flagged/Unverifiable) | Aqsa |
| Strategy | `strategy.validation` (Shehryar) · `strategy.analytics` (Zainab) | both |

---

## 3. 👥 Work split — vertical, one block each, NO going back

Rule of the split: **each member owns a complete feature block (UI + controller + service + repository + JUnit tests) from start to finish and never needs to return to it or touch another block.** Effort is balanced, not just FR count (Shehryar also carries the shared foundation, so his FR block is the simplest CRUD).

### 🅰️ Member A — Muhammad Shehryar Waheed (Lead): "Foundation + Accounts + Worker Data Entry"
**FRs (16):** FR-01–FR-14, FR-41, FR-42
- Maven project, `schema.sql` (all **24 tables** with PK/FK/NOT NULL/CHECK — NFR-05), JDBC connection (`database`), base `model` classes, **all repository interfaces** (the contract B and C code against), seed data script
- Auth: register, login/logout, profile, BCrypt (`security`), `factory.user`, **Proxy** + role checks (FR-01–06, NFR-01/03)
- Platforms: register/manage, add new platform (FR-07, 08)
- Earnings: manual log, validation (`strategy.validation`), Pending status (FR-09–11)
- CSV import in a **transaction**, per-row error report (FR-12, 13, NFR-06/08)
- Evidence upload with type/size checks, `storage` (FR-14, NFR-09)
- **Notification service** + view notifications (FR-41, 42) — B and C only call it
- UI: `ui.auth`, worker screens for the above, main app shell/navigation
- **Tables owned:** users, workers, platform_representatives, advocates, platforms, worker_platforms, earnings_records, evidence, csv_imports, csv_import_errors, notifications

### 🅱️ Member B — Aqsa Ehtesham: "Verification + Commission + Complaints"
**FRs (14):** FR-15–FR-22, FR-26, FR-27, FR-30–FR-33
- Worker: view verification status, earnings history + filters, re-upload evidence → Pending (FR-15–17)
- Representative: pending list, evidence beside figures, mark Verified/Flagged/Unverifiable, mandatory explanation, stored decision (FR-18–21)
- **State pattern** enforcing transitions; Verified never returns to Pending (FR-22)
- Commission rates: rep records rate + effective date, history with `isSuddenChange()` highlight (FR-26, 27)
- Complaints: worker files/tracks; rep views/responds/resolves (own platform only); advocate views all, tags, clusters, escalates, resolves (FR-30–33)
- UI: `ui.verifier`, complaint screens (worker + advocate side), earnings history screen
- **Tables owned:** verification_reviews, commission_rates, complaints, complaint_responses, complaint_clusters, complaint_tags, complaint_tag_map

### 🅲 Member C — Zainab Khurram: "Analytics + Reports + Community + Final Tests"
**FRs (12):** FR-23–FR-25, FR-28, FR-29, FR-34–FR-40
- Analytics: total income/deductions/hourly rate, platform comparison, weekly/monthly trends, city-wide median (FR-23–25) with **`strategy.analytics`**
- Reports: monthly/yearly report with print/export, income certificate (verified only) via **`factory.report`** (FR-28, 29)
- Community: anonymous posts with alias, comments/replies, one like per post, advocate removal + reason (FR-34–36, NFR-04)
- Rate intelligence: share rates, aggregated anonymized rates (FR-37, 38)
- Advocate analytics: commission trends, income by city zone, common complaint categories (FR-39, 40)
- UI: analytics dashboard, report screens, community board, advocate analytics
- Final JUnit coverage for analytics + validation strategies and the demo-data/README wrap-up (NFR-13)
- **Tables owned:** posts, comments, likes, rate_reports, reports, report_earnings

> ✅ Check: 16 + 14 + 12 = **42 FRs**; 11 + 7 + 6 = **24 tables**; every member owns ≥ 2 design patterns.

### How B and C can start on Day 1 without waiting for A
A's **first deliverable (end of Day 1 morning)** is the *contract*: `schema.sql`, `model` classes, repository **interfaces**, and seed data. B and C code services/UI against those interfaces and the seed data, so nobody is blocked or has to come back later.

---

## 4. 📅 Schedule (23 → 27 Nov)

| Day | Shehryar (A) | Aqsa (B) | Zainab (C) |
|---|---|---|---|
| **Sun 23 Nov** | Maven setup, schema (24 tables), models, repo interfaces, seed; share the contract with the team (shared folder / USB / chat) | Install PostgreSQL + JavaFX, read plan, State-pattern design + verification service on seed data | Same setup, strategy interfaces (analytics), report factory, analytics queries on seed data |
| **Mon 24 Nov** | Auth + Proxy + profile, platforms | Verification service/UI (rep side), commission rates | Analytics service + dashboard |
| **Tue 25 Nov** | Earnings + validation + CSV + evidence + notifications | Complaints (worker, rep, advocate), earnings history/filters | Reports, income certificate, community board |
| **Wed 26 Nov** | Finish UI, NFR checks (transactions, file limits), unit + integration tests | Finish UI, State/transition tests | Rate intelligence, advocate analytics, strategy tests |
| **Thu 27 Nov** | Joint smoke test of the whole app, fix own bugs only, demo run-through, submit | ← same → | ← same → |

Each person finishes their block by **Wed night**; Thursday is only a smoke test + demo. ⚠️ 4 days is tight, so build the **must-haves first** and treat the `(+)` FRs (08, 17, 18, 27, 41, 42) as the first things to polish last.

---

## 5. 🔧 Why Maven (you asked me to decide)
Maven is a tool that **downloads libraries (JavaFX, PostgreSQL driver, BCrypt, JUnit) for you** and builds/runs the project with one command, so all 3 laptops behave the same. You only edit one file, `pom.xml`. In the Android project you used Gradle, which is the same idea; Maven is simply more standard for plain Java + JavaFX. (Note: I haven't installed or run anything; I created `pom.xml` only as a starting point.)

## 6. 📏 Team conventions
- **Root package:** `com.fairgig` · files in `src/main/java/com/fairgig/...`
- Plain JDBC with `PreparedStatement` everywhere (NFR-02); never concatenate SQL
- Every user action goes controller → proxy → service → repository; **UI never touches the database**
- Friendly error messages for every failure; no crashing (NFR-10/11)
- Performance targets: screens < 3 s at 5,000 records; CSV 1,000 rows < 30 s (NFR-07/08)
- Own your folder: don't edit another member's package; ask them or the lead

## 7. ✅ Definition of Done (per feature)
1. Works end to end in the JavaFX UI for the correct role only
2. Invalid input shows a clear message
3. Uses the proxy for access checks
4. JUnit test(s) written and passing
5. Notification triggered where the FR says so

## 8. ⚠️ Risks & suggestions
| Risk | Mitigation |
|---|---|
| Only 4 days | Must-haves first, `(+)` features last |
| B/C blocked by A | A delivers contract (schema/models/interfaces/seed) on Day 1 morning |
| Different DB setups | Same PostgreSQL version, one shared `schema.sql` + `seed.sql` |
| PDF issue: UC-4/5/6 pages identical | We follow the FR list for Verifier and Advocate flows (confirmed by lead) |
| JavaFX setup problems on a laptop | Run via Maven `javafx:run`, not manually |

## 9. 📌 Open items to decide later
- PostgreSQL credentials (db name, user, password) — kept in a local config file, never in shared code
- Evidence folder location on disk (default planned: `uploads/`)
- Report export format (printable view / PDF / CSV?) — suggest printable view + CSV to save time
