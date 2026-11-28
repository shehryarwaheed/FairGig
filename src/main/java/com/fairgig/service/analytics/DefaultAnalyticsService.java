package com.fairgig.service.analytics;

import com.fairgig.model.CommissionRate;
import com.fairgig.model.Complaint;
import com.fairgig.model.ComplaintCategory;
import com.fairgig.model.EarningsRecord;
import com.fairgig.model.Platform;
import com.fairgig.model.RateChange;
import com.fairgig.model.Worker;
import com.fairgig.repository.CityStatsRepository;
import com.fairgig.repository.CommissionRateRepository;
import com.fairgig.repository.ComplaintRepository;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.strategy.analytics.CityMedianComparisonStrategy;
import com.fairgig.strategy.analytics.EarningsSummaryStrategy;
import com.fairgig.strategy.analytics.PlatformTrendStrategy;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Real implementation of {@link AnalyticsService}. */
public class DefaultAnalyticsService implements AnalyticsService {

    private final EarningsRepository earnings;
    private final UserRepository users;
    private final PlatformRepository platforms;
    private final CityStatsRepository cityStats;
    private final CommissionRateRepository commissionRates;
    private final ComplaintRepository complaints;

    public DefaultAnalyticsService(EarningsRepository earnings, UserRepository users, PlatformRepository platforms,
                                   CityStatsRepository cityStats, CommissionRateRepository commissionRates,
                                   ComplaintRepository complaints) {
        this.earnings = earnings;
        this.users = users;
        this.platforms = platforms;
        this.cityStats = cityStats;
        this.commissionRates = commissionRates;
        this.complaints = complaints;
    }

    @Override
    public WorkerAnalytics myAnalytics(int workerId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("The start date must not be after the end date.");
        }
        Worker worker = users.findWorkerById(workerId)
            .orElseThrow(() -> new NotFoundException("This worker account no longer exists."));
        List<EarningsRecord> records = earnings.findByWorker(workerId, new EarningsFilter(null, from, to, null));
        Map<Integer, String> names = platforms.findAll().stream()
            .collect(Collectors.toMap(Platform::getPlatformId, Platform::getName));
        return new WorkerAnalytics(
            new EarningsSummaryStrategy().analyze(records),
            new PlatformTrendStrategy().analyze(records),
            new CityMedianComparisonStrategy(cityStats.workerTotalsInCity(worker.getCity(), from, to)).analyze(records),
            names);
    }

    @Override
    public AdvocateAnalytics advocateOverview() {
        Map<String, List<RateChange>> trends = new LinkedHashMap<>();
        for (Platform platform : platforms.findAll()) {
            List<CommissionRate> rates = new ArrayList<>(commissionRates.findByPlatform(platform.getPlatformId()));
            if (rates.isEmpty()) {
                continue;
            }
            rates.sort(Comparator.comparing(CommissionRate::getEffectiveDate).thenComparing(CommissionRate::getRateId));
            List<RateChange> changes = new ArrayList<>();
            BigDecimal previous = null;
            for (CommissionRate rate : rates) {
                changes.add(new RateChange(rate, previous));
                previous = rate.getRatePercent();
            }
            trends.put(platform.getName(), changes);
        }
        Map<ComplaintCategory, Long> counted = complaints.findAll().stream()
            .collect(Collectors.groupingBy(Complaint::getCategory, Collectors.counting()));
        Map<ComplaintCategory, Long> mostCommonFirst = new LinkedHashMap<>();
        counted.entrySet().stream()
            .sorted(Map.Entry.<ComplaintCategory, Long>comparingByValue().reversed())
            .forEach(e -> mostCommonFirst.put(e.getKey(), e.getValue()));
        return new AdvocateAnalytics(trends, cityStats.incomeByZone(), mostCommonFirst);
    }
}
