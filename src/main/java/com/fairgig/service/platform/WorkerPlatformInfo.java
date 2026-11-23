package com.fairgig.service.platform;

import com.fairgig.model.Platform;
import java.time.LocalDate;

/**
 * One line of "my platforms": the platform itself plus the worker's own details for it.
 *
 * @param platform      the platform
 * @param accountHandle the worker's account name on that platform (may be null)
 * @param joinedDate    when the worker joined it (may be null)
 */
public record WorkerPlatformInfo(Platform platform, String accountHandle, LocalDate joinedDate) {
}
