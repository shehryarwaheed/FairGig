package com.fairgig.service.complaint;

import com.fairgig.model.ComplaintCluster;

/** A common issue with the number of complaints grouped under it (FR-33). */
public record IssueSummary(ComplaintCluster cluster, int complaintCount) {
}
