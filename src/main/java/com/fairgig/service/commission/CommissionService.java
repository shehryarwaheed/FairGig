package com.fairgig.service.commission;

import com.fairgig.model.CommissionRate;
import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.OwnPlatform;
import com.fairgig.proxy.Self;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Commission rates of the platforms (FR-26, FR-27). Screens must use the proxied instance:
 * {@code AccessProxy.wrap(CommissionService.class, new DefaultCommissionService(...))}.
 */
public interface CommissionService {

    /**
     * A representative records their platform's commission rate with the day it starts (FR-27).
     * Representatives can only do this for their own platform (FR-06).
     *
     * @throws com.fairgig.util.ValidationException listing every problem (rate outside 0-100,
     *         bad date, a rate already recorded for that date)
     */
    @Access(roles = Role.VERIFIER)
    CommissionRate recordRate(@Self int representativeId, @OwnPlatform int platformId,
                              BigDecimal ratePercent, LocalDate effectiveDate);

    /**
     * The rate history of one platform, NEWEST first. Each rate knows the one before it, so
     * {@link CommissionRate#isSuddenChange()} can highlight big jumps (FR-26). Workers and
     * advocates may look at any platform; a representative only at their own.
     */
    @Access
    List<CommissionRate> history(@OwnPlatform int platformId);
}
