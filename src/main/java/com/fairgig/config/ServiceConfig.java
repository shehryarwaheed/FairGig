
package com.fairgig.config;

import com.fairgig.repository.*;
import com.fairgig.service.analytics.*;
import com.fairgig.service.community.*;
import com.fairgig.service.report.*;
import com.fairgig.strategy.analytics.*;

import java.util.EnumMap;
import java.util.Map;

public final class ServiceConfig {

    public static Services create(DataSource ds) {
        // ---- repositories -------------------------------------------------
        PostRepository postRepo = new JdbcPostRepository(ds);
        CommentRepository commentRepo = new JdbcCommentRepository(ds);
        LikeRepository likeRepo = new JdbcLikeRepository(ds);
        EarningsRepository earningsRepo = new JdbcEarningsRepository(ds);

        // ---- analytics ----------------------------------------------------
        AnalyticsStrategy<EarningsSummary> summaryStrat = new EarningsSummaryStrategy(earningsRepo);
        AnalyticsStrategy<PlatformTrend> trendStrat = new PlatformTrendStrategy(earningsRepo);
        AnalyticsStrategy<CityMedianComparison> medianStrat = new CityMedianComparisonStrategy(earningsRepo);
        AnalyticsService analyticsService = new AnalyticsService(summaryStrat, trendStrat, medianStrat);

        // ---- reports -------------------------------------------------------
        Map<ReportService.Type, ReportFactory> factories = new EnumMap<>(ReportService.Type.class);
        factories.put(ReportService.Type.EARNINGS, new EarningsReportFactory(analyticsService));
        factories.put(ReportService.Type.INCOME_CERTIFICATE, new IncomeCertificateFactory(analyticsService));
        ReportService reportService = new ReportService(factories);

        // ---- community -----------------------------------------------------
        CommunityService communityService = new DefaultCommunityService(postRepo, commentRepo, likeRepo);
        AdvocateCommunityService advocateService = new AdvocateCommunityService(postRepo);

        // Return a holder object
        return new Services(analyticsService, reportService, communityService, advocateService);
    }


    public static record Services(
            AnalyticsService analytics,
            ReportService report,
            CommunityService community,
            AdvocateCommunityService advocate) {}
}