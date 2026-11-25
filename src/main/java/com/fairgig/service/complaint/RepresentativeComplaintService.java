package com.fairgig.service.complaint;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.OwnPlatform;
import com.fairgig.proxy.Self;
import java.util.List;

/**
 * The representative's side of complaints (FR-32), always limited to their own platform (FR-06).
 * Screens must use the proxied instance.
 */
public interface RepresentativeComplaintService {

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
}
