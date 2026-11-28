package com.fairgig.service.community;

import com.fairgig.model.Role;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.math.BigDecimal;
import java.util.List;

/** Sharing platform rates and seeing the anonymized averages (FR-37, FR-38). */
public interface RateIntelligenceService {

    /** Incentive and deductions are optional. City and zone come from the worker's profile. */
    @Access(roles = Role.WORKER)
    void share(@Self int workerId, int platformId, BigDecimal commissionPercent, BigDecimal incentive,
               BigDecimal deductions);

    @Access(roles = {Role.WORKER, Role.ADVOCATE})
    List<PlatformRateSummary> aggregated();
}
