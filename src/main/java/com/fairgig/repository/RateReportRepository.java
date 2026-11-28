package com.fairgig.repository;

import com.fairgig.model.RateReport;
import java.util.List;

/** Data access for rates shared by workers. */
public interface RateReportRepository {

    RateReport save(RateReport report);

    List<RateReport> findAll();
}
