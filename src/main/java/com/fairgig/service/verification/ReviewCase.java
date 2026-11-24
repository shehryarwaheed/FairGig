package com.fairgig.service.verification;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Evidence;
import java.util.List;

/**
 * Everything a representative needs to judge one earning (FR-19): the submitted figures and the
 * evidence files next to them. Use EvidenceService.readEvidence to open a file.
 */
public record ReviewCase(EarningsRecord record, String workerName, String platformName, List<Evidence> evidence) {
}
