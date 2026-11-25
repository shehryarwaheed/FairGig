package com.fairgig.controller;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.service.complaint.ComplaintView;
import com.fairgig.service.complaint.WorkerComplaintService;
import java.util.List;

/** What the worker's complaint screen calls (FR-30, FR-31). Failures become clear messages, never crashes. */
public class WorkerComplaintController {

    private final WorkerComplaintService complaints;

    public WorkerComplaintController(WorkerComplaintService complaints) {
        this.complaints = complaints;
    }

    public Outcome<Complaint> fileComplaint(int workerId, int platformId, ComplaintCategory category,
                                            String description, Integer recordId) {
        return Outcome.attempt(() -> complaints.fileComplaint(workerId, platformId, category, description, recordId));
    }

    public Outcome<List<ComplaintView>> myComplaints(int workerId) {
        return Outcome.attempt(() -> complaints.myComplaints(workerId));
    }
}
