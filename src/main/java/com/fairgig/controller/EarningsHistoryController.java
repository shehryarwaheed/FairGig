package com.fairgig.controller;

import com.fairgig.repository.EarningsFilter;
import com.fairgig.service.earnings.EarningStatusView;
import com.fairgig.service.earnings.EarningsHistoryService;
import java.util.List;

/** What the worker's earnings history screen calls (FR-15, FR-16). Failures become clear messages. */
public class EarningsHistoryController {

    private final EarningsHistoryService history;

    public EarningsHistoryController(EarningsHistoryService history) {
        this.history = history;
    }

    public Outcome<List<EarningStatusView>> myEarnings(int workerId, EarningsFilter filter) {
        return Outcome.attempt(() -> history.myEarnings(workerId, filter));
    }
}
