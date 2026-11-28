package com.fairgig.ui.worker;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Platform;
import com.fairgig.model.PostCategory;
import com.fairgig.service.community.PostView;
import com.fairgig.ui.Banner;
import com.fairgig.ui.ChipGroup;
import com.fairgig.ui.CommunityBoard;
import com.fairgig.ui.FormField;
import com.fairgig.ui.PageContext;
import com.fairgig.ui.Pages;
import com.fairgig.ui.Widgets;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** "Community" tab (FR-34, FR-35): post anonymously, like once, comment and reply. */
public class WorkerCommunityPage {

    private final PageContext ctx;

    public WorkerCommunityPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        Banner banner = new Banner();
        ChipGroup<PostCategory> category = new ChipGroup<>(PostCategory::label);
        category.setItems(List.of(PostCategory.values()));
        ChipGroup<Platform> platform = new ChipGroup<>(Platform::getName);
        Outcome<List<Platform>> platforms = ctx.app().worker().availablePlatforms();
        if (platforms.isOk()) {
            platform.setItems(platforms.value());
        }
        FormField title = new FormField("Title", "edit", "A short title", false);
        TextArea content = new TextArea();
        content.setPromptText("What would you like to share? Your name is never shown.");
        content.setWrapText(true);
        content.setPrefRowCount(4);

        CommunityBoard board = new CommunityBoard(ctx, false);
        Button publish = Widgets.primaryButton("Post anonymously");
        publish.setMaxWidth(Region.USE_PREF_SIZE);
        publish.setOnAction(e -> {
            Platform chosen = platform.selected();
            Outcome<PostView> result = ctx.app().community().createPost(ctx.userId(), category.selected(),
                title.text(), content.getText(), chosen == null ? null : chosen.getPlatformId());
            if (result.isOk()) {
                title.setText("");
                content.clear();
                board.reload();
                banner.showSuccess("Posted. Others see it under the name " + result.value().alias() + ".");
            } else {
                banner.showError(result.problems());
            }
        });

        VBox page = Pages.page();
        page.getChildren().addAll(
            Pages.header("Community", "Anonymous posts, comments and likes from fellow workers."),
            Pages.section("Write a post", Widgets.label("Category", "field-label"), category,
                Widgets.label("Platform (optional)", "field-label"), platform, title, content, banner, publish),
            Pages.section("Board", board));
        return Pages.scroll(page);
    }
}
