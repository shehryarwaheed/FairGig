package com.fairgig.ui;

import com.fairgig.controller.Outcome;
import com.fairgig.model.Notification;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** The notification list of the logged-in user (FR-42): click one to mark it read. */
public class NotificationsPage {

    private final PageContext ctx;
    private final VBox list = new VBox(8);
    private final Banner banner = new Banner();
    private final Label summary = Widgets.label("", "h-sub");

    public NotificationsPage(PageContext ctx) {
        this.ctx = ctx;
    }

    public Node build() {
        VBox page = Pages.page();

        Button markAll = Widgets.secondaryButton("Mark all as read");
        markAll.setMaxWidth(Region.USE_PREF_SIZE);
        markAll.setOnAction(e -> {
            Outcome<Void> result = ctx.app().notifications().markAllRead(ctx.userId());
            if (result.isOk()) {
                refresh();
                ctx.nav().refreshNotifications();
            } else {
                banner.showError(result.message());
            }
        });

        VBox header = Pages.header(NavItem.NOTIFICATIONS.title(), NavItem.NOTIFICATIONS.subtitle());
        HBox top = new HBox(12, header, Widgets.spacer(), markAll);
        top.setAlignment(Pos.CENTER_LEFT);

        page.getChildren().addAll(top, banner, Pages.section(null, summary, list));
        refresh();
        return Pages.scroll(page);
    }

    private void refresh() {
        Outcome<List<Notification>> result = ctx.app().notifications().myNotifications(ctx.userId());
        list.getChildren().clear();
        if (!result.isOk()) {
            banner.showError(result.message());
            summary.setText("");
            return;
        }
        List<Notification> notes = result.value();
        long unread = notes.stream().filter(n -> !n.isRead()).count();
        summary.setText(notes.isEmpty() ? "" : unread + " unread of " + notes.size());
        if (notes.isEmpty()) {
            list.getChildren().add(Pages.empty("bell", "You are all caught up",
                "New updates about reviews, evidence and complaints will show up here."));
            return;
        }
        for (Notification n : notes) {
            list.getChildren().add(row(n));
        }
    }

    private Node row(Notification n) {
        Region dot = new Region();
        dot.getStyleClass().add("note-dot");
        if (n.isRead()) {
            dot.getStyleClass().add("read");
        }
        dot.setPrefSize(10, 10);
        dot.setMinSize(10, 10);
        dot.setMaxSize(10, 10);

        Label message = Widgets.label(n.getMessage(), "label");
        message.setWrapText(true);
        Label when = Widgets.label(Fmt.when(n.getCreatedAt()), "hint-text");
        VBox text = new VBox(3, message, when);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox row = new HBox(14, dot, text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("note-row");
        if (!n.isRead()) {
            row.getStyleClass().add("unread");
            row.setOnMouseClicked(e -> {
                Outcome<Void> result = ctx.app().notifications().markRead(ctx.userId(), n.getNotificationId());
                if (result.isOk()) {
                    refresh();
                    ctx.nav().refreshNotifications();
                } else {
                    banner.showError(result.message());
                }
            });
        }
        return row;
    }
}
