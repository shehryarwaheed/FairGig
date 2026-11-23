package com.fairgig.ui;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Builds the empty placeholder page for tabs whose feature is not built yet (teammates replace these). */
public final class PlaceholderPages {

    private PlaceholderPages() {}

    public static Node create(UiRole role, NavItem item) {
        VBox page = new VBox(22);
        page.setPadding(new Insets(6, 8, 28, 4));

        List<Node> animated = new ArrayList<>();

        VBox header = new VBox(4);
        if (item.home()) {
            String first = role.userName().split(" ")[0];
            header.getChildren().addAll(
                Widgets.label("Welcome back, " + first, "h-title"),
                Widgets.label(item.subtitle(), "h-sub"));
        } else {
            header.getChildren().addAll(
                Widgets.label(item.title(), "h-title"),
                Widgets.label(item.subtitle(), "h-sub"));
        }
        animated.add(header);

        if (item.home()) {
            HBox tiles = new HBox(16);
            for (TileSpec spec : role.tiles()) {
                VBox tile = Widgets.statTile(spec.icon(), spec.tint(), spec.label(), value(spec), spec.note());
                HBox.setHgrow(tile, Priority.ALWAYS);
                tile.setMaxWidth(Double.MAX_VALUE);
                tiles.getChildren().add(tile);
            }
            animated.add(tiles);
        } else {
            animated.add(toolbar());
        }

        Node art = Illustrations.emptyState(item.icon(), "Nothing here yet",
            "This tab is a placeholder. Its content will be added in a later step.");
        VBox hero = Widgets.card(0, art);
        hero.setAlignment(Pos.CENTER);
        hero.setMinHeight(360);
        VBox.setVgrow(hero, Priority.ALWAYS);
        animated.add(hero);

        page.getChildren().addAll(animated);
        Anim.stagger(animated, 18, 380, 70);

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("transparent-scroll");
        return scroll;
    }

    private static ObservableValue<String> value(TileSpec spec) {
        return switch (spec.kind()) {
            case MONEY -> CurrencyState.money("—");
            case PERCENT -> new SimpleStringProperty("— %");
            case COUNT -> new SimpleStringProperty("—");
        };
    }

    /** Decorative toolbar made of skeleton bars: search + filters + primary action. */
    private static Node toolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        HBox search = Widgets.field("search", "Search will live here", false);
        search.setDisable(true);
        search.setPrefWidth(300);
        search.setMaxWidth(300);
        bar.getChildren().addAll(
            search,
            Widgets.skeleton(86, 40, "skeleton-pill", 0.0),
            Widgets.skeleton(86, 40, "skeleton-pill", 0.25),
            Widgets.spacer(),
            Widgets.skeleton(130, 44, "skeleton-accent", 0.5));
        return new StackPane(bar);
    }
}
