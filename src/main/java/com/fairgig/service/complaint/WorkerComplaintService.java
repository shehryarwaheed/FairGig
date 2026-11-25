package com.fairgig.service.complaint;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.util.List;

/**
 * The worker's side of complaints (FR-30, FR-31). Screens must use the proxied instance:
 * {@code AccessProxy.wrap(WorkerComplaintService.class, new DefaultWorkerComplaintService(...))}.
 */
public interface WorkerComplaintService {

    /**
     * Files a complaint about a platform the worker is registered on, optionally linked to one of
     * their own earnings of that platform. The platform's representatives are notified, in the
     * same transaction (FR-41). The status starts as SUBMITTED.
     *
     * @throws com.fairgig.util.ValidationException listing every problem
     */
    @Access(roles = Role.WORKER)
    Complaint fileComplaint(@Self int workerId, int platformId, ComplaintCategory category,
                            String description, Integer recordId);

    /** The worker's own complaints with their status and the answers, newest first (FR-31). */
    @Access(roles = Role.WORKER)
    List<ComplaintView> myComplaints(@Self int workerId);
}
