package com.fairgig.controller;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintTag;
import com.fairgig.service.complaint.ComplaintService;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.IssueSummary;
import java.util.List;

/**
 * What the complaint screens of all three roles call (FR-30 to FR-33).
 * Every method returns an {@link Outcome}, so a failure shows a clear message instead of crashing.
 */
public class ComplaintController {

    private final ComplaintService complaints;

    public ComplaintController(ComplaintService complaints) {
        this.complaints = complaints;
    }

    // ---- worker

    public Outcome<Complaint> fileComplaint(int workerId, int platformId, ComplaintCategory category,
                                            String description, Integer recordId) {
        return Outcome.attempt(() -> complaints.fileComplaint(workerId, platformId, category, description, recordId));
    }

    public Outcome<List<ComplaintView>> myComplaints(int workerId) {
        return Outcome.attempt(() -> complaints.myComplaints(workerId));
    }

    // ---- representative

    public Outcome<List<ComplaintView>> platformComplaints(int platformId) {
        return Outcome.attempt(() -> complaints.platformComplaints(platformId));
    }

    public Outcome<ComplaintResponse> respond(int representativeId, int complaintId, String message) {
        return Outcome.attempt(() -> complaints.respond(representativeId, complaintId, message));
    }

    public Outcome<Complaint> resolve(int representativeId, int complaintId) {
        return Outcome.attempt(() -> complaints.resolve(representativeId, complaintId));
    }

    // ---- advocate

    public Outcome<List<ComplaintView>> allComplaints() {
        return Outcome.attempt(complaints::allComplaints);
    }

    public Outcome<Complaint> escalate(int advocateId, int complaintId) {
        return Outcome.attempt(() -> complaints.escalate(advocateId, complaintId));
    }

    public Outcome<Complaint> resolveAsAdvocate(int advocateId, int complaintId) {
        return Outcome.attempt(() -> complaints.resolveAsAdvocate(advocateId, complaintId));
    }

    public Outcome<ComplaintTag> tag(int advocateId, int complaintId, String tagName) {
        return Outcome.attempt(() -> complaints.tag(advocateId, complaintId, tagName));
    }

    public Outcome<Void> removeTag(int advocateId, int complaintId, int tagId) {
        return Outcome.run(() -> complaints.removeTag(advocateId, complaintId, tagId));
    }

    public Outcome<List<ComplaintTag>> allTags() {
        return Outcome.attempt(complaints::allTags);
    }

    public Outcome<ComplaintCluster> createIssue(int advocateId, List<Integer> complaintIds,
                                                 String title, String description) {
        return Outcome.attempt(() -> complaints.createIssue(advocateId, complaintIds, title, description));
    }

    public Outcome<Void> assignToIssue(int advocateId, int complaintId, Integer clusterId) {
        return Outcome.run(() -> complaints.assignToIssue(advocateId, complaintId, clusterId));
    }

    public Outcome<List<IssueSummary>> issues() {
        return Outcome.attempt(complaints::issues);
    }
}
