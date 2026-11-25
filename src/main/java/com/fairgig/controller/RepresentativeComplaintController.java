package com.fairgig.controller;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.RepresentativeComplaintService;
import java.util.List;

/** What the representative's complaint screens call (FR-32). Failures become clear messages, never crashes. */
public class RepresentativeComplaintController {

    private final RepresentativeComplaintService complaints;

    public RepresentativeComplaintController(RepresentativeComplaintService complaints) {
        this.complaints = complaints;
    }

    public Outcome<List<ComplaintView>> platformComplaints(int platformId) {
        return Outcome.attempt(() -> complaints.platformComplaints(platformId));
    }

    public Outcome<ComplaintResponse> respond(int representativeId, int complaintId, String message) {
        return Outcome.attempt(() -> complaints.respond(representativeId, complaintId, message));
    }

    public Outcome<Complaint> resolve(int representativeId, int complaintId) {
        return Outcome.attempt(() -> complaints.resolve(representativeId, complaintId));
    }
}
