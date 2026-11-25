package com.fairgig.controller;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCluster;
import com.fairgig.model.ComplaintTag;
import com.fairgig.service.complaint.AdvocateComplaintService;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.IssueSummary;
import java.util.List;

/** What the advocate's complaint screens call (FR-33). Failures become clear messages, never crashes. */
public class AdvocateComplaintController {

    private final AdvocateComplaintService complaints;

    public AdvocateComplaintController(AdvocateComplaintService complaints) {
        this.complaints = complaints;
    }

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
