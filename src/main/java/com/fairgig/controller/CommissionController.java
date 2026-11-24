package com.fairgig.controller;

import com.fairgig.model.CommissionRate;
import com.fairgig.service.commission.CommissionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** What the commission rate screens call (FR-26, FR-27). Failures become clear messages, never crashes. */
public class CommissionController {

    private final CommissionService commission;

    public CommissionController(CommissionService commission) {
        this.commission = commission;
    }

    public Outcome<CommissionRate> recordRate(int representativeId, int platformId,
                                              BigDecimal ratePercent, LocalDate effectiveDate) {
        return Outcome.attempt(() -> commission.recordRate(representativeId, platformId, ratePercent, effectiveDate));
    }

    public Outcome<List<CommissionRate>> history(int platformId) {
        return Outcome.attempt(() -> commission.history(platformId));
    }
}
