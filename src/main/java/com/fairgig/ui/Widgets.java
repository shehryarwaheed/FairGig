package com.fairgig.ui;

import java.util.Locale;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Reusable clay-styled building blocks. */
public final class Widgets {

    private Widgets() {}

    public static Label label(String text, String... styleClasses) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    /** Small UPPERCASE micro-label. */
    public static Label caps(String text) {
        return label(text.toUpperCase(Locale.ROOT), "caps");
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Button primaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-primary");
        b.setMaxWidth(Double.MAX_VALUE);
        Anim.pressable(b);
        return b;
    }

    public static Button secondaryButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn-secondary");
        b.setMaxWidth(Double.MAX_VALUE);
        Anim.pressable(b);
        return b;
    }

    /** Sunken input with a leading icon. Password fields get a show/hide eye. */
    public static HBox field(String icon, String prompt, boolean password) {
        HBox box = new HBox(10);
        box.getStyleClass().add("clay-field");
        box.setAlignment(Pos.CENTER_LEFT);

        StackPane iconChip = new StackPane(Icons.of(icon, 18, "icon-accent"));
        iconChip.getStyleClass().add("field-icon-chip");
        box.getChildren().add(iconChip);

        if (!password) {
            TextField tf = new TextField();
            tf.setPromptText(prompt);
            HBox.setHgrow(tf, Priority.ALWAYS);
            trackFocus(tf, box);
            box.getChildren().add(tf);
            return box;
        }

        PasswordField hidden = new PasswordField();
        TextField shown = new TextField();
        hidden.setPromptText(prompt);
        shown.setPromptText(prompt);
        shown.textProperty().bindBidirectional(hidden.textProperty());
        shown.setVisible(false);
        shown.setManaged(false);
        trackFocus(hidden, box);
        trackFocus(shown, box);
        StackPane holder = new StackPane(hidden, shown);
        HBox.setHgrow(holder, Priority.ALWAYS);

        Region eye = Icons.of("eye", 20);
        eye.setStyle("-fx-cursor: hand;");
        eye.setOnMouseClicked(e -> {
            boolean show = !shown.isVisible();
            shown.setVisible(show);
            shown.setManaged(show);
            hidden.setVisible(!show);
            hidden.setManaged(!show);
        });
        box.getChildren().addAll(holder, eye);
        return box;
    }

    private static void trackFocus(TextInputControl input, HBox box) {
        input.focusedProperty().addListener((obs, was, focused) -> {
            box.getStyleClass().remove("focused");
            if (focused) {
                box.getStyleClass().add("focused");
            }
        });
    }

    /** A decorative placeholder bar that softly shimmers. */
    public static Region skeleton(double width, double height, String styleClass, double shimmerDelay) {
        Region r = new Region();
        r.getStyleClass().add(styleClass);
        r.setPrefSize(width, height);
        r.setMinSize(width, height);
        r.setMaxHeight(height);
        Anim.shimmer(r, shimmerDelay);
        return r;
    }

    /** Dashboard tile: micro-label, tinted icon chip, big figure and a recessed meter. */
    public static VBox statTile(String icon, String tint, String label, ObservableValue<String> value, String note) {
        VBox tile = new VBox(12);
        tile.getStyleClass().add("clay-card");
        tile.setPadding(new Insets(18, 20, 20, 20));
        tile.setMinWidth(190);

        StackPane chip = new StackPane(Icons.of(icon, 20, "icon-" + tint));
        chip.getStyleClass().addAll("tile-chip", "tint-" + tint);

        HBox top = new HBox(caps(label), spacer(), chip);
        top.setAlignment(Pos.CENTER_LEFT);

        Label figure = label("", "big-value");
        figure.textProperty().bind(value);

        StackPane meter = new StackPane();
        meter.getStyleClass().add("meter-track");
        meter.setPrefHeight(12);
        meter.setMinHeight(12);
        meter.setMaxHeight(12);
        Region fill = new Region();
        fill.getStyleClass().add("meter-fill");
        fill.maxWidthProperty().bind(meter.widthProperty().multiply(0.32));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        meter.getChildren().add(fill);

        tile.getChildren().addAll(top, figure, meter, label(note, "h-sub"));
        Anim.pressable(tile);
        return tile;
    }

    /** Finds the text box inside a container built by {@link #field}. */
    public static TextInputControl input(Node root) {
        if (root instanceof TextInputControl text) {
            return text;
        }
        if (root instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                TextInputControl found = input(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** Like {@link #statTile} but without the decorative meter: for real numbers. */
    public static VBox infoTile(String icon, String tint, String label, ObservableValue<String> value, String note) {
        VBox tile = new VBox(10);
        tile.getStyleClass().add("clay-card");
        tile.setPadding(new Insets(18, 20, 18, 20));
        tile.setMinWidth(190);

        StackPane chip = new StackPane(Icons.of(icon, 20, "icon-" + tint));
        chip.getStyleClass().addAll("tile-chip", "tint-" + tint);
        HBox top = new HBox(caps(label), spacer(), chip);
        top.setAlignment(Pos.CENTER_LEFT);

        Label figure = label("", "big-value");
        figure.textProperty().bind(value);
        tile.getChildren().addAll(top, figure, label(note, "h-sub"));
        return tile;
    }

    /** Wraps any node in a clay card. */
    public static VBox card(double spacing, Node... children) {
        VBox card = new VBox(spacing, children);
        card.getStyleClass().add("clay-card");
        card.setPadding(new Insets(22));
        return card;
    }
}
