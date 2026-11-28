package com.fairgig.ui.advocate;

import com.fairgig.ui.CommunityBoard;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** "Moderation" tab (FR-36): remove inappropriate posts with a reason. */
public class AdvocateModerationPage {

    private final PageContext ctx;

    public AdvocateModerationPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();
        page.getChildren().addAll(
            Pages.header("Moderation", "Remove inappropriate community posts with a reason."),
            new CommunityBoard(ctx, true));
        return Pages.scroll(page);
    }
}
