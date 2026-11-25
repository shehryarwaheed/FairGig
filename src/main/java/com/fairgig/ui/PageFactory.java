package com.fairgig.ui;

import com.fairgig.ui.verifier.VerifierCommissionPage;
import com.fairgig.ui.verifier.VerifierHistoryPage;
import com.fairgig.ui.verifier.VerifierHomePage;
import com.fairgig.ui.verifier.VerifierPages;
import com.fairgig.ui.verifier.VerifierPendingPage;
import com.fairgig.ui.worker.WorkerComplaintsPage;
import com.fairgig.ui.worker.WorkerCsvPage;
import com.fairgig.ui.worker.WorkerEarningsPage;
import com.fairgig.ui.worker.WorkerEvidencePage;
import com.fairgig.ui.worker.WorkerHistoryPage;
import com.fairgig.ui.worker.WorkerHomePage;
import com.fairgig.ui.worker.WorkerPages;
import com.fairgig.ui.worker.WorkerPlatformsPage;
import javafx.scene.Node;

/**
 * Decides which page to show for a tab. Pages that are built are created here; tabs of features
 * that other team members are still building fall back to a friendly placeholder.
 *
 * <p><b>Teammates:</b> to plug in your page, add a {@code case} for your tab title below.
 */
public final class PageFactory {

    private PageFactory() {}

    public static Node create(PageContext ctx, NavItem item) {
        // pages every role has (FR-04, FR-42)
        if (item == NavItem.NOTIFICATIONS) {
            return new NotificationsPage(ctx).build();
        }
        if (item == NavItem.PROFILE) {
            return new ProfilePage(ctx).build();
        }

        switch (ctx.role()) {
            case WORKER -> {
                switch (item.title()) {
                    case WorkerPages.HOME -> {
                        return new WorkerHomePage(ctx).build();
                    }
                    case WorkerPages.PLATFORMS -> {
                        return new WorkerPlatformsPage(ctx).build();
                    }
                    case WorkerPages.EARNINGS -> {
                        return new WorkerEarningsPage(ctx).build();
                    }
                    case WorkerPages.COMPLAINTS -> {
                        return new WorkerComplaintsPage(ctx).build();
                    }
                    case WorkerPages.HISTORY -> {
                        return new WorkerHistoryPage(ctx).build();
                    }
                    case WorkerPages.IMPORT_CSV -> {
                        return new WorkerCsvPage(ctx).build();
                    }
                    case WorkerPages.EVIDENCE -> {
                        return new WorkerEvidencePage(ctx).build();
                    }
                    default -> { /* teammates' tabs: placeholder below */ }
                }
            }
            case REPRESENTATIVE -> {
                switch (item.title()) {
                    case VerifierPages.HOME -> {
                        return new VerifierHomePage(ctx).build();
                    }
                    case VerifierPages.PENDING_REVIEWS -> {
                        return new VerifierPendingPage(ctx).build();
                    }
                    case VerifierPages.REVIEW_HISTORY -> {
                        return new VerifierHistoryPage(ctx).build();
                    }
                    case VerifierPages.COMMISSION_RATES -> {
                        return new VerifierCommissionPage(ctx).build();
                    }
                    default -> { /* complaints: placeholder for now */ }
                }
            }
            case ADVOCATE -> { /* complaints, moderation, analytics: placeholders for now */ }
            default -> { }
        }
        return PlaceholderPages.create(ctx.role(), item);
    }
}
