package com.fairgig.repository;

import com.fairgig.model.Evidence;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Data access for evidence files attached to earnings (table "evidence"). */
public interface EvidenceRepository {

    /** Inserts the evidence row and fills in its id and upload time. */
    Evidence save(Evidence evidence);

    /** Same, but inside the caller's transaction (connection is not closed or committed). */
    Evidence save(Connection connection, Evidence evidence);

    Optional<Evidence> findById(int evidenceId);

    /** All evidence of one earning, newest first. */
    List<Evidence> findByRecord(int recordId);
}
