package com.fairgig.service.earnings;

import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import com.fairgig.repository.EarningsFilter;
import java.util.List;

/**
 * The worker's earnings history with verification status and filters (FR-15, FR-16).
 * Screens must use the proxied instance. A worker only ever sees their own earnings.
 */
public interface EarningsHistoryService {

    /**
     * The worker's earnings, newest shift first, narrowed by platform, date range and status
     * (use {@link EarningsFilter#none()} for everything).
     *
     * @throws com.fairgig.util.ValidationException if the start date is after the end date
     */
    @Access(roles = Role.WORKER)
    List<EarningStatusView> myEarnings(@Self int workerId, EarningsFilter filter);
}
