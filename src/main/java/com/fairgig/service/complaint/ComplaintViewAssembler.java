package com.fairgig.service.complaint;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintTag;
import com.fairgig.repository.ComplaintRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Turns complaints into {@link ComplaintView}s (names instead of ids, plus the answers). One job, used by every role. */
final class ComplaintViewAssembler {

    private final ComplaintRepository complaints;
    private final ComplaintSupport support;

    ComplaintViewAssembler(ComplaintRepository complaints, ComplaintSupport support) {
        this.complaints = complaints;
        this.support = support;
    }

    /** For workers and representatives: no tags, no common issue. */
    List<ComplaintView> views(List<Complaint> list) {
        return views(list, Map.of(), Map.of());
    }

    /** For advocates: with each complaint's tags and the title of its common issue. */
    List<ComplaintView> views(List<Complaint> list, Map<Integer, List<ComplaintTag>> tagsByComplaint,
                              Map<Integer, String> issueTitles) {
        Map<Integer, String> workerNames = new HashMap<>();
        Map<Integer, String> platformNames = new HashMap<>();
        List<ComplaintView> views = new ArrayList<>();
        for (Complaint c : list) {
            views.add(new ComplaintView(c,
                workerNames.computeIfAbsent(c.getWorkerId(), support::userName),
                platformNames.computeIfAbsent(c.getPlatformId(), support::platformName),
                complaints.findResponses(c.getComplaintId()),
                tagsByComplaint.getOrDefault(c.getComplaintId(), List.of()),
                c.getClusterId() == null ? null : issueTitles.get(c.getClusterId())));
        }
        return views;
    }
}
