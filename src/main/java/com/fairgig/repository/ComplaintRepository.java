package com.fairgig.repository;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintStatus;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Data access for complaints and the answers to them (tables "complaints" and "complaint_responses").
 * The overloads that take a Connection run inside the caller's transaction, so a change and its
 * notification are saved together (NFR-06). The status changes only happen if the complaint still
 * has the status the caller saw ({@code expected}); a false result means someone else got there first.
 */
public interface ComplaintRepository {

    /** Inserts the complaint (status SUBMITTED unless set otherwise) and fills in id and createdAt. */
    Complaint save(Complaint complaint);

    /** Same, but inside the given transaction. The connection is NOT closed or committed here. */
    Complaint save(Connection connection, Complaint complaint);

    Optional<Complaint> findById(int complaintId);

    /** A worker's own complaints, newest first (FR-31). */
    List<Complaint> findByWorker(int workerId);

    /** Complaints about one platform, newest first (a representative's list, FR-32). */
    List<Complaint> findByPlatform(int platformId);

    /** Every complaint, newest first (an advocate's list, FR-33). */
    List<Complaint> findAll();

    /** The complaints grouped into one common issue, newest first. */
    List<Complaint> findByCluster(int clusterId);

    /** SUBMITTED -> ESCALATED: sets escalated_at and the advocate who handled it (FR-33). */
    boolean markEscalated(Connection connection, int complaintId, ComplaintStatus expected, int advocateId);

    /**
     * Any open status -> RESOLVED: sets resolved_at. A representative passes their own id as
     * {@code representativeId}, an advocate passes theirs as {@code advocateId}; the other is null
     * and the stored value stays as it was.
     */
    boolean markResolved(Connection connection, int complaintId, ComplaintStatus expected,
                         Integer representativeId, Integer advocateId);

    /** Puts the complaint into a cluster, or takes it out again with a null cluster id (FR-33). */
    boolean assignCluster(Connection connection, int complaintId, Integer clusterId);

    /** Stores a representative's answer and fills in its id and time (FR-32). */
    ComplaintResponse saveResponse(Connection connection, ComplaintResponse response);

    /** The answers to one complaint, oldest first. */
    List<ComplaintResponse> findResponses(int complaintId);
}
