package com.fairgig.service.analytics;

import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.time.LocalDate;

/** Analytics for workers (FR-23 to FR-25) and advocates (FR-39, FR-40). */
public interface AnalyticsService {

    /** Null dates mean "no limit". */
    @Access(roles = Role.WORKER)
    WorkerAnalytics myAnalytics(@Self int workerId, LocalDate from, LocalDate to);

    @Access(roles = Role.ADVOCATE)
    AdvocateAnalytics advocateOverview();
}
