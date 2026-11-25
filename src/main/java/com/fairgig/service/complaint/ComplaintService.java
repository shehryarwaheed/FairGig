package com.fairgig.service.complaint;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintTag;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.OwnPlatform;
import com.fairgig.proxy.Self;
import java.util.List;

/**
 * Complaints for all three roles (FR-30 to FR-33). Screens must use the proxied instance:
 * {@code AccessProxy.wrap(ComplaintService.class, new DefaultComplaintService(...))}.
 * The State pattern decides which status changes are possible; the Proxy decides who may ask for them.
 */
public interface ComplaintService {

    // ---------------------------------------------------------------- worker (FR-30, FR-31)

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

    // ---------------------------------------------------------------- representative (FR-32)

    /** The complaints about the representative's own platform, newest first. */
    @Access(roles = Role.VERIFIER)
    List<ComplaintView> platformComplaints(@OwnPlatform int platformId);

    /**
     * Answers a complaint of the representative's platform and notifies the worker (FR-41).
     * The status stays SUBMITTED: an answer is not the end of the complaint.
     */
    @Access(roles = Role.VERIFIER)
    ComplaintResponse respond(@Self int representativeId, int complaintId, String message);

    /**
     * Resolves a complaint of the representative's platform and notifies the worker (FR-41).
     * A complaint that an advocate has escalated is resolved by an advocate, not here.
     */
    @Access(roles = Role.VERIFIER)
    Complaint resolve(@Self int representativeId, int complaintId);

    // ---------------------------------------------------------------- advocate (FR-33)

    /** Every complaint, newest first, with its tags and the common issue it belongs to. */
    @Access(roles = Role.ADVOCATE)
    List<ComplaintView> allComplaints();

    /** SUBMITTED -> ESCALATED. Notifies the worker. */
    @Access(roles = Role.ADVOCATE)
    Complaint escalate(@Self int advocateId, int complaintId);

    /** Resolves any open complaint. Notifies the worker. */
    @Access(roles = Role.ADVOCATE)
    Complaint resolveAsAdvocate(@Self int advocateId, int complaintId);

    /**
     * Puts a tag on a complaint. The tag is created if it does not exist yet. Names are kept in
     * lower case with dashes, for example "payment-delay".
     */
    @Access(roles = Role.ADVOCATE)
    ComplaintTag tag(@Self int advocateId, int complaintId, String tagName);

    /** Takes a tag off a complaint. */
    @Access(roles = Role.ADVOCATE)
    void removeTag(@Self int advocateId, int complaintId, int tagId);

    /** All tags, to pick one from. */
    @Access(roles = Role.ADVOCATE)
    List<ComplaintTag> allTags();

    /** Creates a common issue and groups the given complaints under it, in one transaction. */
    @Access(roles = Role.ADVOCATE)
    ComplaintCluster createIssue(@Self int advocateId, List<Integer> complaintIds, String title, String description);

    /** Moves a complaint into an existing common issue, or out of it when {@code clusterId} is null. */
    @Access(roles = Role.ADVOCATE)
    void assignToIssue(@Self int advocateId, int complaintId, Integer clusterId);

    /** The common issues with the number of complaints in each, newest first. */
    @Access(roles = Role.ADVOCATE)
    List<IssueSummary> issues();
}
