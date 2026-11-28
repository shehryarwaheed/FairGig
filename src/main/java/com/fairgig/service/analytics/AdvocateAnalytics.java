package com.fairgig.service.analytics;

import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.RateChange;
import com.fairgig.repository.CityStatsRepository.ZoneIncome;
import java.util.List;
import java.util.Map;

/**
 * Everything the advocate analyses (FR-39, FR-40).
 *
 * @param commissionTrends   platform name -> its rate history, oldest first
 * @param incomeByZone       anonymous income per city zone
 * @param complaintCounts    category -> number of complaints, most common first
 */
public record AdvocateAnalytics(Map<String, List<RateChange>> commissionTrends,
                                List<ZoneIncome> incomeByZone,
                                Map<ComplaintCategory, Long> complaintCounts) {
}
