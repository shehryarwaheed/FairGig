package com.fairgig.service.complaint;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintTag;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.util.List;

/**
 * The advocate's side of complaints (FR-33). Screens must use the proxied instance.
 * The State pattern decides which status changes are possible; the Proxy limits this to advocates.
 */
public interface AdvocateComplaintService {

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
