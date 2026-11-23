-- =====================================================================
-- FairGig - database schema (PostgreSQL)
-- 24 tables, taken from the ERD in FairGig-Phase1.pdf.
--
-- HOW TO USE
--   1. Create an EMPTY database named fairgig_db yourself (pgAdmin).
--   2. Open a query window on fairgig_db and run this whole file once.
--   This file never creates or drops the database itself.
--
-- NFR-05: every table has a primary key; foreign keys, NOT NULL and
--         CHECK constraints are enforced by the database.
-- NFR-02: the Java code only uses parameterized statements (JDBC).
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Accounts and access control
-- ---------------------------------------------------------------------

CREATE TABLE users (
    user_id        INTEGER GENERATED ALWAYS AS IDENTITY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,            -- BCrypt hash only (NFR-01)
    role           VARCHAR(10)  NOT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users        PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email  UNIQUE (email),
    CONSTRAINT ck_users_role   CHECK (role IN ('WORKER', 'VERIFIER', 'ADVOCATE')),
    CONSTRAINT ck_users_name   CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_users_email  CHECK (position('@' IN email) > 1)
);

CREATE TABLE platforms (
    platform_id  INTEGER GENERATED ALWAYS AS IDENTITY,
    name         VARCHAR(100) NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_platforms       PRIMARY KEY (platform_id),
    CONSTRAINT uq_platforms_name  UNIQUE (name),
    CONSTRAINT ck_platforms_name  CHECK (length(trim(name)) > 0)
);

CREATE TABLE workers (
    user_id    INTEGER     NOT NULL,
    phone      VARCHAR(20),
    city       VARCHAR(50) NOT NULL,
    city_zone  VARCHAR(50) NOT NULL,
    CONSTRAINT pk_workers      PRIMARY KEY (user_id),
    CONSTRAINT fk_workers_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE TABLE advocates (
    user_id  INTEGER NOT NULL,
    CONSTRAINT pk_advocates      PRIMARY KEY (user_id),
    CONSTRAINT fk_advocates_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

-- A representative is tied to exactly ONE platform (FR-02, FR-06).
CREATE TABLE platform_representatives (
    user_id      INTEGER NOT NULL,
    platform_id  INTEGER NOT NULL,
    CONSTRAINT pk_platform_reps          PRIMARY KEY (user_id),
    CONSTRAINT fk_platform_reps_user     FOREIGN KEY (user_id)     REFERENCES users (user_id)         ON DELETE CASCADE,
    CONSTRAINT fk_platform_reps_platform FOREIGN KEY (platform_id) REFERENCES platforms (platform_id)
);

-- Platforms a worker works on (FR-07).
CREATE TABLE worker_platforms (
    worker_id       INTEGER NOT NULL,
    platform_id     INTEGER NOT NULL,
    account_handle  VARCHAR(100),
    joined_date     DATE,
    CONSTRAINT pk_worker_platforms          PRIMARY KEY (worker_id, platform_id),
    CONSTRAINT fk_worker_platforms_worker   FOREIGN KEY (worker_id)   REFERENCES workers (user_id)       ON DELETE CASCADE,
    CONSTRAINT fk_worker_platforms_platform FOREIGN KEY (platform_id) REFERENCES platforms (platform_id)
);


-- ---------------------------------------------------------------------
-- 2. Earnings, CSV import and evidence
-- ---------------------------------------------------------------------

CREATE TABLE csv_imports (
    import_id     INTEGER GENERATED ALWAYS AS IDENTITY,
    worker_id     INTEGER      NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    imported_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_rows    INTEGER      NOT NULL DEFAULT 0,
    invalid_rows  INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pk_csv_imports        PRIMARY KEY (import_id),
    CONSTRAINT fk_csv_imports_worker FOREIGN KEY (worker_id) REFERENCES workers (user_id),
    CONSTRAINT ck_csv_imports_rows   CHECK (valid_rows >= 0 AND invalid_rows >= 0)
);

CREATE TABLE earnings_records (
    record_id       INTEGER GENERATED ALWAYS AS IDENTITY,
    worker_id       INTEGER       NOT NULL,
    platform_id     INTEGER       NOT NULL,
    import_id       INTEGER,                              -- only set for CSV rows
    shift_date      DATE          NOT NULL,
    hours_worked    DECIMAL(5,2)  NOT NULL,
    gross_earnings  DECIMAL(10,2) NOT NULL,
    deductions      DECIMAL(10,2) NOT NULL,
    net_received    DECIMAL(10,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',   -- FR-11
    entry_method    VARCHAR(15)   NOT NULL DEFAULT 'MANUAL',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_earnings_records          PRIMARY KEY (record_id),
    CONSTRAINT fk_earnings_records_worker   FOREIGN KEY (worker_id)   REFERENCES workers (user_id),
    CONSTRAINT fk_earnings_records_platform FOREIGN KEY (platform_id) REFERENCES platforms (platform_id),
    CONSTRAINT fk_earnings_records_import   FOREIGN KEY (import_id)   REFERENCES csv_imports (import_id),
    CONSTRAINT ck_earnings_hours   CHECK (hours_worked >= 0 AND hours_worked <= 24),
    CONSTRAINT ck_earnings_gross   CHECK (gross_earnings >= 0),
    CONSTRAINT ck_earnings_deduct  CHECK (deductions >= 0),
    CONSTRAINT ck_earnings_net     CHECK (net_received >= 0),
    CONSTRAINT ck_earnings_status  CHECK (status IN ('PENDING', 'VERIFIED', 'DISPUTED_FLAGGED', 'UNVERIFIABLE')),
    CONSTRAINT ck_earnings_method  CHECK (entry_method IN ('MANUAL', 'CSV_IMPORT')),
    CONSTRAINT ck_earnings_import  CHECK ((entry_method = 'CSV_IMPORT') = (import_id IS NOT NULL))
);

CREATE TABLE csv_import_errors (
    error_id    INTEGER GENERATED ALWAYS AS IDENTITY,
    import_id   INTEGER      NOT NULL,
    row_num     INTEGER      NOT NULL,
    field_name  VARCHAR(50),
    message     VARCHAR(255) NOT NULL,
    CONSTRAINT pk_csv_import_errors        PRIMARY KEY (error_id),
    CONSTRAINT fk_csv_import_errors_import FOREIGN KEY (import_id) REFERENCES csv_imports (import_id) ON DELETE CASCADE,
    CONSTRAINT ck_csv_import_errors_row    CHECK (row_num > 0)
);

CREATE TABLE evidence (
    evidence_id  INTEGER GENERATED ALWAYS AS IDENTITY,
    record_id    INTEGER      NOT NULL,
    file_path    VARCHAR(255) NOT NULL,
    file_type    VARCHAR(20)  NOT NULL,
    uploaded_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_evidence        PRIMARY KEY (evidence_id),
    CONSTRAINT fk_evidence_record FOREIGN KEY (record_id) REFERENCES earnings_records (record_id) ON DELETE CASCADE,
    CONSTRAINT ck_evidence_type   CHECK (file_type IN ('PNG', 'JPG', 'PDF'))          -- NFR-09
);


-- ---------------------------------------------------------------------
-- 3. Verification and commission rates
-- ---------------------------------------------------------------------

-- Every decision is stored with the reviewer and the time (FR-21).
CREATE TABLE verification_reviews (
    review_id          INTEGER GENERATED ALWAYS AS IDENTITY,
    record_id          INTEGER      NOT NULL,
    representative_id  INTEGER      NOT NULL,
    decision           VARCHAR(20)  NOT NULL,
    explanation        VARCHAR(500),
    reviewed_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_verification_reviews        PRIMARY KEY (review_id),
    CONSTRAINT fk_verification_reviews_record FOREIGN KEY (record_id)         REFERENCES earnings_records (record_id),
    CONSTRAINT fk_verification_reviews_rep    FOREIGN KEY (representative_id) REFERENCES platform_representatives (user_id),
    CONSTRAINT ck_verification_decision       CHECK (decision IN ('VERIFIED', 'DISPUTED_FLAGGED', 'UNVERIFIABLE')),
    -- Flagging requires an explanation (FR-21)
    CONSTRAINT ck_verification_explanation   CHECK (decision <> 'DISPUTED_FLAGGED' OR length(trim(coalesce(explanation, ''))) > 0)
);

CREATE TABLE commission_rates (
    rate_id             INTEGER GENERATED ALWAYS AS IDENTITY,
    platform_id         INTEGER      NOT NULL,
    recorded_by_rep_id  INTEGER      NOT NULL,
    rate_percent        DECIMAL(5,2) NOT NULL,
    effective_date      DATE         NOT NULL,
    CONSTRAINT pk_commission_rates          PRIMARY KEY (rate_id),
    CONSTRAINT fk_commission_rates_platform FOREIGN KEY (platform_id)        REFERENCES platforms (platform_id),
    CONSTRAINT fk_commission_rates_rep      FOREIGN KEY (recorded_by_rep_id) REFERENCES platform_representatives (user_id),
    CONSTRAINT ck_commission_rate_percent   CHECK (rate_percent >= 0 AND rate_percent <= 100)
);


-- ---------------------------------------------------------------------
-- 4. Complaints (grievance management)
-- ---------------------------------------------------------------------

CREATE TABLE complaint_clusters (
    cluster_id   INTEGER GENERATED ALWAYS AS IDENTITY,
    advocate_id  INTEGER      NOT NULL,
    title        VARCHAR(150) NOT NULL,
    description  VARCHAR(500),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_complaint_clusters          PRIMARY KEY (cluster_id),
    CONSTRAINT fk_complaint_clusters_advocate FOREIGN KEY (advocate_id) REFERENCES advocates (user_id)
);

CREATE TABLE complaint_tags (
    tag_id       INTEGER GENERATED ALWAYS AS IDENTITY,
    advocate_id  INTEGER     NOT NULL,
    name         VARCHAR(50) NOT NULL,
    CONSTRAINT pk_complaint_tags          PRIMARY KEY (tag_id),
    CONSTRAINT uq_complaint_tags_name     UNIQUE (name),
    CONSTRAINT fk_complaint_tags_advocate FOREIGN KEY (advocate_id) REFERENCES advocates (user_id)
);

CREATE TABLE complaints (
    complaint_id            INTEGER GENERATED ALWAYS AS IDENTITY,
    worker_id               INTEGER       NOT NULL,
    platform_id             INTEGER       NOT NULL,
    record_id               INTEGER,                      -- optional link to an earning (FR-30)
    cluster_id              INTEGER,
    handled_by_advocate_id  INTEGER,
    resolved_by_rep_id      INTEGER,
    category                VARCHAR(20)   NOT NULL,
    description             VARCHAR(1000) NOT NULL,
    status                  VARCHAR(15)   NOT NULL DEFAULT 'SUBMITTED',
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    escalated_at            TIMESTAMP,
    resolved_at             TIMESTAMP,
    CONSTRAINT pk_complaints           PRIMARY KEY (complaint_id),
    CONSTRAINT fk_complaints_worker    FOREIGN KEY (worker_id)              REFERENCES workers (user_id),
    CONSTRAINT fk_complaints_platform  FOREIGN KEY (platform_id)            REFERENCES platforms (platform_id),
    CONSTRAINT fk_complaints_record    FOREIGN KEY (record_id)              REFERENCES earnings_records (record_id),
    CONSTRAINT fk_complaints_cluster   FOREIGN KEY (cluster_id)             REFERENCES complaint_clusters (cluster_id),
    CONSTRAINT fk_complaints_advocate  FOREIGN KEY (handled_by_advocate_id) REFERENCES advocates (user_id),
    CONSTRAINT fk_complaints_rep       FOREIGN KEY (resolved_by_rep_id)     REFERENCES platform_representatives (user_id),
    CONSTRAINT ck_complaints_category  CHECK (category IN ('UNFAIR_DEDUCTION', 'MISSING_PAYMENT', 'OTHER')),
    CONSTRAINT ck_complaints_status    CHECK (status IN ('SUBMITTED', 'ESCALATED', 'RESOLVED')),
    CONSTRAINT ck_complaints_desc      CHECK (length(trim(description)) > 0)
);

-- Many-to-many: complaints <-> tags (FR-33)
CREATE TABLE complaint_tag_map (
    complaint_id  INTEGER NOT NULL,
    tag_id        INTEGER NOT NULL,
    CONSTRAINT pk_complaint_tag_map           PRIMARY KEY (complaint_id, tag_id),
    CONSTRAINT fk_complaint_tag_map_complaint FOREIGN KEY (complaint_id) REFERENCES complaints (complaint_id) ON DELETE CASCADE,
    CONSTRAINT fk_complaint_tag_map_tag       FOREIGN KEY (tag_id)       REFERENCES complaint_tags (tag_id)   ON DELETE CASCADE
);

CREATE TABLE complaint_responses (
    response_id        INTEGER GENERATED ALWAYS AS IDENTITY,
    complaint_id       INTEGER       NOT NULL,
    representative_id  INTEGER       NOT NULL,
    message            VARCHAR(1000) NOT NULL,
    responded_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_complaint_responses           PRIMARY KEY (response_id),
    CONSTRAINT fk_complaint_responses_complaint FOREIGN KEY (complaint_id)      REFERENCES complaints (complaint_id) ON DELETE CASCADE,
    CONSTRAINT fk_complaint_responses_rep       FOREIGN KEY (representative_id) REFERENCES platform_representatives (user_id),
    CONSTRAINT ck_complaint_responses_message   CHECK (length(trim(message)) > 0)
);


-- ---------------------------------------------------------------------
-- 5. Community board and rate intelligence
--    Posts and comments store only an anonymous alias for other workers
--    to see; worker_id is visible to the system only (NFR-04).
-- ---------------------------------------------------------------------

CREATE TABLE posts (
    post_id          INTEGER GENERATED ALWAYS AS IDENTITY,
    worker_id        INTEGER       NOT NULL,
    platform_id      INTEGER,
    moderated_by     INTEGER,                              -- advocate who removed it
    title            VARCHAR(150)  NOT NULL,
    content          VARCHAR(2000) NOT NULL,
    category         VARCHAR(20)   NOT NULL,
    anonymous_alias  VARCHAR(50)   NOT NULL,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_removed       BOOLEAN       NOT NULL DEFAULT FALSE,
    removed_at       TIMESTAMP,
    removal_reason   VARCHAR(255),
    CONSTRAINT pk_posts           PRIMARY KEY (post_id),
    CONSTRAINT fk_posts_worker    FOREIGN KEY (worker_id)    REFERENCES workers (user_id),
    CONSTRAINT fk_posts_platform  FOREIGN KEY (platform_id)  REFERENCES platforms (platform_id),
    CONSTRAINT fk_posts_moderator FOREIGN KEY (moderated_by) REFERENCES advocates (user_id),
    CONSTRAINT ck_posts_category  CHECK (category IN ('PLATFORM_RATES', 'COMPLAINT', 'SUPPORT_REQUEST')),
    -- A removed post must record why (FR-36)
    CONSTRAINT ck_posts_removal   CHECK (is_removed = FALSE OR length(trim(coalesce(removal_reason, ''))) > 0)
);

CREATE TABLE comments (
    comment_id         INTEGER GENERATED ALWAYS AS IDENTITY,
    post_id            INTEGER       NOT NULL,
    worker_id          INTEGER       NOT NULL,
    parent_comment_id  INTEGER,                            -- set when this is a reply
    content            VARCHAR(1000) NOT NULL,
    anonymous_alias    VARCHAR(50)   NOT NULL,
    created_at         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_comments         PRIMARY KEY (comment_id),
    CONSTRAINT fk_comments_post    FOREIGN KEY (post_id)           REFERENCES posts (post_id)       ON DELETE CASCADE,
    CONSTRAINT fk_comments_worker  FOREIGN KEY (worker_id)         REFERENCES workers (user_id),
    CONSTRAINT fk_comments_parent  FOREIGN KEY (parent_comment_id) REFERENCES comments (comment_id) ON DELETE CASCADE,
    CONSTRAINT ck_comments_parent  CHECK (parent_comment_id IS NULL OR parent_comment_id <> comment_id),
    CONSTRAINT ck_comments_content CHECK (length(trim(content)) > 0)
);

-- The composite primary key lets a worker like a post only once (FR-35).
CREATE TABLE likes (
    post_id    INTEGER   NOT NULL,
    worker_id  INTEGER   NOT NULL,
    liked_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_likes        PRIMARY KEY (post_id, worker_id),
    CONSTRAINT fk_likes_post   FOREIGN KEY (post_id)   REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_likes_worker FOREIGN KEY (worker_id) REFERENCES workers (user_id)
);

CREATE TABLE rate_reports (
    rate_report_id      INTEGER GENERATED ALWAYS AS IDENTITY,
    worker_id           INTEGER      NOT NULL,
    platform_id         INTEGER      NOT NULL,
    commission_percent  DECIMAL(5,2) NOT NULL,
    incentive           DECIMAL(10,2),
    deductions          DECIMAL(10,2),
    city                VARCHAR(50)  NOT NULL,
    city_zone           VARCHAR(50),
    reported_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rate_reports          PRIMARY KEY (rate_report_id),
    CONSTRAINT fk_rate_reports_worker   FOREIGN KEY (worker_id)   REFERENCES workers (user_id),
    CONSTRAINT fk_rate_reports_platform FOREIGN KEY (platform_id) REFERENCES platforms (platform_id),
    CONSTRAINT ck_rate_reports_percent  CHECK (commission_percent >= 0 AND commission_percent <= 100),
    CONSTRAINT ck_rate_reports_amounts  CHECK ((incentive IS NULL OR incentive >= 0) AND (deductions IS NULL OR deductions >= 0))
);


-- ---------------------------------------------------------------------
-- 6. Reports and notifications
-- ---------------------------------------------------------------------

CREATE TABLE reports (
    report_id                INTEGER GENERATED ALWAYS AS IDENTITY,
    worker_id                INTEGER       NOT NULL,
    type                     VARCHAR(20)   NOT NULL,
    from_date                DATE          NOT NULL,
    to_date                  DATE          NOT NULL,
    total_gross              DECIMAL(12,2) NOT NULL,
    total_deductions         DECIMAL(12,2) NOT NULL,
    total_net                DECIMAL(12,2) NOT NULL,
    total_hours              DECIMAL(8,2)  NOT NULL,
    verified_earnings_total  DECIMAL(12,2) NOT NULL,
    generated_at             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_reports        PRIMARY KEY (report_id),
    CONSTRAINT fk_reports_worker FOREIGN KEY (worker_id) REFERENCES workers (user_id),
    CONSTRAINT ck_reports_type   CHECK (type IN ('MONTHLY', 'YEARLY', 'INCOME_CERTIFICATE')),
    CONSTRAINT ck_reports_dates  CHECK (from_date <= to_date),
    CONSTRAINT ck_reports_totals CHECK (total_gross >= 0 AND total_deductions >= 0 AND total_net >= 0
                                        AND total_hours >= 0 AND verified_earnings_total >= 0)
);

-- Which earnings a report covers.
CREATE TABLE report_earnings (
    report_id  INTEGER NOT NULL,
    record_id  INTEGER NOT NULL,
    CONSTRAINT pk_report_earnings        PRIMARY KEY (report_id, record_id),
    CONSTRAINT fk_report_earnings_report FOREIGN KEY (report_id) REFERENCES reports (report_id)           ON DELETE CASCADE,
    CONSTRAINT fk_report_earnings_record FOREIGN KEY (record_id) REFERENCES earnings_records (record_id)
);

CREATE TABLE notifications (
    notification_id  INTEGER GENERATED ALWAYS AS IDENTITY,
    user_id          INTEGER      NOT NULL,
    message          VARCHAR(255) NOT NULL,
    is_read          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_notifications      PRIMARY KEY (notification_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);


-- ---------------------------------------------------------------------
-- 7. Indexes for speed (NFR-07: screens < 3 s with 5,000 records)
--    Primary keys and unique constraints are indexed automatically.
-- ---------------------------------------------------------------------

CREATE INDEX ix_earnings_worker_date     ON earnings_records (worker_id, shift_date);
CREATE INDEX ix_earnings_platform_status ON earnings_records (platform_id, status);
CREATE INDEX ix_earnings_import          ON earnings_records (import_id);
CREATE INDEX ix_evidence_record          ON evidence (record_id);
CREATE INDEX ix_reviews_record           ON verification_reviews (record_id);
CREATE INDEX ix_csv_errors_import        ON csv_import_errors (import_id);
CREATE INDEX ix_commission_platform_date ON commission_rates (platform_id, effective_date);
CREATE INDEX ix_complaints_platform      ON complaints (platform_id, status);
CREATE INDEX ix_complaints_worker        ON complaints (worker_id);
CREATE INDEX ix_complaints_cluster       ON complaints (cluster_id);
CREATE INDEX ix_responses_complaint      ON complaint_responses (complaint_id);
CREATE INDEX ix_posts_created            ON posts (created_at DESC);
CREATE INDEX ix_comments_post            ON comments (post_id);
CREATE INDEX ix_rate_reports_platform    ON rate_reports (platform_id);
CREATE INDEX ix_reports_worker           ON reports (worker_id);
CREATE INDEX ix_notifications_user       ON notifications (user_id, is_read);


-- ---------------------------------------------------------------------
-- 8. Safety net: a Verified earning is final (FR-22)
--    The Java State pattern is the main enforcement; this trigger makes
--    sure no bug can ever move a VERIFIED record back to another status.
--    (Remove this section if the team decides Verified may be changed.)
-- ---------------------------------------------------------------------

CREATE FUNCTION fn_verified_is_final() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'VERIFIED' AND NEW.status <> 'VERIFIED' THEN
        RAISE EXCEPTION 'A Verified earning cannot change its status (record %)', OLD.record_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_verified_is_final
    BEFORE UPDATE OF status ON earnings_records
    FOR EACH ROW EXECUTE FUNCTION fn_verified_is_final();
