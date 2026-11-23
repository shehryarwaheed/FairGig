package com.fairgig.service.evidence;

import com.fairgig.model.Evidence;

/** An evidence row together with the file's bytes (what a viewer needs to show the picture or PDF). */
public record EvidenceFile(Evidence evidence, byte[] content) {
}
