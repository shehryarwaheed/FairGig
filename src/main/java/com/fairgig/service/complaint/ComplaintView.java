package com.fairgig.service.complaint;

import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintResponse;
import com.fairgig.model.ComplaintTag;
import java.util.List;

/**
 * One complaint as a screen needs it: with names instead of ids and its answers.
 * {@code tags} and {@code issueTitle} are only filled for advocates (they are the advocate's own
 * working labels); for workers and representatives the tags are empty and the title is null.
 */
public record ComplaintView(Complaint complaint, String workerName, String platformName,
                            List<ComplaintResponse> responses, List<ComplaintTag> tags, String issueTitle) {
}
