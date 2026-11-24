package com.fairgig.service.earnings;

import com.fairgig.model.EarningsRecord;
import com.fairgig.model.VerificationReview;
import com.fairgig.model.VerificationStatus;
import com.fairgig.repository.EarningsFilter;
import com.fairgig.repository.EarningsRepository;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.ReviewRepository;
import com.fairgig.util.ValidationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Real implementation of {@link EarningsHistoryService}. Wrap it with AccessProxy before giving it to screens. */
public class DefaultEarningsHistoryService implements EarningsHistoryService {

    private final EarningsRepository earnings;
    private final ReviewRepository reviews;
    private final PlatformRepository platforms;

    public DefaultEarningsHistoryService(EarningsRepository earnings, ReviewRepository reviews,
                                         PlatformRepository platforms) {
        this.earnings = earnings;
        this.reviews = reviews;
        this.platforms = platforms;
    }

    @Override
    public List<EarningStatusView> myEarnings(int workerId, EarningsFilter filter) {
        EarningsFilter f = filter == null ? EarningsFilter.none() : filter;
        if (f.from() != null && f.to() != null && f.from().isAfter(f.to())) {
            throw new ValidationException("The start date is after the end date. Please swap them.");
        }

        Map<Integer, String> platformNames = new HashMap<>();
        List<EarningStatusView> rows = new ArrayList<>();
        for (EarningsRecord record : earnings.findByWorker(workerId, f)) {
            rows.add(new EarningStatusView(record,
                platformNames.computeIfAbsent(record.getPlatformId(), this::platformName),
                latestReviewFor(record)));
        }
        return rows;
    }

    /**
     * Only earnings that were decided have a review. Looking it up only for Verified, Flagged and
     * Unverifiable rows (never for Pending ones) keeps the screen fast with thousands of records (NFR-07).
     */
    private VerificationReview latestReviewFor(EarningsRecord record) {
        if (record.getStatus() == VerificationStatus.PENDING) {
            return null;
        }
        return reviews.findByRecord(record.getRecordId()).stream().findFirst().orElse(null);   // newest first
    }

    private String platformName(int platformId) {
        return platforms.findById(platformId).map(p -> p.getName()).orElse("Unknown platform");
    }
}
