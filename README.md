# FairGig

**Know what you earn. Check what the platforms take.**

FairGig is a desktop app for gig workers (riders, drivers, delivery partners). Workers log their earnings and attach proof, a platform representative checks each record, and an advocate watches complaints and trends across all platforms.

> FairGig only **records and verifies data**. It never handles payments.

---

## Who uses it

| Role | What they do |
|---|---|
| **Worker** | Logs earnings (by hand or CSV), uploads evidence, sees analytics and reports, files complaints, posts anonymously, shares rates |
| **Representative** | Reviews pending earnings of **one** platform, marks them Verified / Flagged / Unverifiable, records commission rates, answers complaints |
| **Advocate** | Moderates the community board, groups and escalates complaints, studies trends across platforms |

## How an earning is verified

```
Worker logs earning  ->  Pending
                           |
        Representative checks the evidence
        /            |                \
   Verified     Flagged (needs         Unverifiable
   (final)      an explanation)
                     |                      |
                     +---- worker re-uploads evidence ----> Pending again
```

A Verified record can never go back to Pending.

---

## What you need

| Tool | Version | Check with |
|---|---|---|
| Java (JDK) | 17 or newer | `java -version` |
| Apache Maven | 3.9 or newer | `mvn -v` |
| PostgreSQL | 14 or newer | pgAdmin or `psql` |
| IntelliJ IDEA (optional) | any recent | |

## Set it up (first time only)

1. **Create the database.** In pgAdmin, create an empty database named `fairgig_db`.
2. **Create your settings file.** Copy `.env.example` to a new file called `.env` and put your own PostgreSQL password in it.
   `.env` is git-ignored, so your password never leaves your computer.
3. **Create the tables and sample data.** In pgAdmin open a Query Tool on `fairgig_db` and run, in this order:
   `src/main/resources/sql/schema.sql` (the 24 tables), then `src/main/resources/sql/seed.sql` (fake sample data).
   Run each file **once**, on an empty database.
4. **Open the project.** In IntelliJ choose *File > Open* and select `pom.xml`. Let it import the Maven project.
5. **Check the connection (optional).** Run `src/main/java/com/fairgig/database/ConnectionCheck.java`.
   It should say `Connection OK` and `Tables found: 24`.

## Sample accounts (fake data, password for all: `Test@1234`)

| Role | Name | Email |
|---|---|---|
| Worker | Ali Raza | `worker1@fairgig.test` |
| Worker | Sara Khan | `worker2@fairgig.test` |
| Worker | Bilal Ahmed | `worker3@fairgig.test` |
| Representative (Uber) | Hina Malik | `rep.uber@fairgig.test` |
| Representative (Foodpanda) | Omar Siddiqui | `rep.foodpanda@fairgig.test` |
| Advocate | Maryam Tariq | `advocate@fairgig.test` |

## Run it

**With IntelliJ:** open `src/main/java/com/fairgig/app/Launcher.java` and press the green play button.
(Run `Launcher`, not `Main`. It avoids a common JavaFX start-up error.)

**With the terminal:**
```
mvn javafx:run
```

**Run the tests:**
```
mvn test
```

## Project structure

```
src/main/java/com/fairgig
  app/          start-up (Main, Launcher)
  ui/           JavaFX screens: auth, worker, verifier, advocate
  controller/   connects screens to services
  proxy/        role and platform access checks
  security/     password hashing
  service/      business logic, one package per feature
  repository/   data-access interfaces
  repository/jdbc/   PostgreSQL implementations (JDBC only)
  database/     connection and transactions
  storage/      evidence files on disk
  factory/      Factory Method pattern
  strategy/     Strategy pattern
  model/        entities and enums
  util/         shared helpers
src/main/resources
  css/          the app theme (light and dark)
  sql/          database schema and sample data
```

## Design patterns used

Repository, Factory Method, State, Strategy and Proxy.

## Tests

`mvn test` runs about 154 JUnit 5 tests:
- **Unit tests** (`src/test/java/com/fairgig/unit`): the Factory, Strategy and Proxy patterns, password security,
  CSV parser, evidence file rules, validation rules and error handling.
- **Integration tests** (`src/test/java/com/fairgig/integration`): every repository and service against the real
  PostgreSQL database, the 5,000-record and 1,000-row speed targets, and the JavaFX screens driven without a window.
  They create their own uniquely named data and delete it afterwards. If PostgreSQL is not running they are skipped.

## Project status

| Area | Owner | Status |
|---|---|---|
| Foundation: schema, database, models, repositories, security, proxy, controllers, app shell and theme | Shehryar | Done |
| Accounts, platforms, earnings entry, CSV import, evidence, notifications (FR-01 to FR-14, FR-41, FR-42) | Shehryar | Done, with screens |
| Verification workflow, commission rates, complaints (FR-15 to FR-22, FR-26, FR-27, FR-30 to FR-33) | Aqsa | To do |
| Analytics, reports, community board, rate intelligence, advocate analytics (FR-23 to FR-25, FR-28, FR-29, FR-34 to FR-40) | Zainab | To do |

Tabs of the unfinished features show a friendly placeholder until their owner plugs them in.

## More documents

- `TEAM_GUIDE.md`: how to add a feature, the building blocks that already exist, and the exact feature list for each team member.
- `implementation_plan.md`: full workflow, architecture and how the work is split between the three members.

## Team

Muhammad Shehryar Waheed, Aqsa Ehtesham, Zainab Khurram.
Instructor: Mr. Arslan Asif. FAST NUCES, Lahore.
