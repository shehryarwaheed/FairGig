package com.fairgig.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Small layout helpers shared by all pages. */
public final class Pages {

    private Pages() {}

    /** The vertical stack every page is built in. */
    public static VBox page() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(6, 10, 28, 4));
        return page;
    }

    /** Page title with a one-line description. */
    public static VBox header(String title, String subtitle) {
        return new VBox(4, Widgets.label(title, "h-title"), Widgets.label(subtitle, "h-sub"));
    }

    /** Wraps a page in a transparent scroll pane (the standard page container). */
    public static ScrollPane scroll(VBox page) {
        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("transparent-scroll");
        return scroll;
    }

    /** A clay card with a section title. */
    public static VBox section(String title, Node... content) {
        VBox card = new VBox(14);
        card.getStyleClass().add("clay-card");
        card.setPadding(new Insets(22));
        if (title != null) {
            card.getChildren().add(Widgets.label(title, "section-title"));
        }
        card.getChildren().addAll(content);
        return card;
    }

    /** The window a node lives in (needed by file choosers), or null if it is not shown yet. */
    public static Window window(Node node) {
        return node.getScene() == null ? null : node.getScene().getWindow();
    }

    /** A centred, friendly "nothing here" block. */
    public static VBox empty(String icon, String title, String text) {
        VBox box = new VBox(10, Illustrations.emptyState(icon, title, text));
        box.setAlignment(Pos.CENTER);
        return box;
    }
}
