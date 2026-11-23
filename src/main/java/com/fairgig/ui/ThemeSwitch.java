package com.fairgig.ui;

import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Light / dark toggle: a recessed track with an inflated marble knob. */
public class ThemeSwitch extends StackPane {

    private static final double TRACK_W = 68;
    private static final double TRACK_H = 36;
    private static final double KNOB = 28;
    private static final double TRAVEL = TRACK_W - KNOB - 8;

    private final StackPane knob = new StackPane();

    public ThemeSwitch() {
        Region track = new Region();
        track.getStyleClass().add("switch-track");
        track.setPrefSize(TRACK_W, TRACK_H);
        track.setMinSize(TRACK_W, TRACK_H);
        track.setMaxSize(TRACK_W, TRACK_H);

        knob.getStyleClass().add("switch-knob");
        knob.setPrefSize(KNOB, KNOB);
        knob.setMinSize(KNOB, KNOB);
        knob.setMaxSize(KNOB, KNOB);
        knob.setAlignment(Pos.CENTER);

        setAlignment(Pos.CENTER_LEFT);
        setMaxSize(TRACK_W, TRACK_H);
        getChildren().addAll(track, knob);
        StackPane.setMargin(knob, new javafx.geometry.Insets(0, 0, 0, 4));

        syncKnob(false);
        setOnMouseClicked(e -> AppTheme.darkProperty().set(!AppTheme.darkProperty().get()));
        AppTheme.darkProperty().addListener((obs, was, isDark) -> syncKnob(true));
    }

    private void syncKnob(boolean animate) {
        boolean dark = AppTheme.darkProperty().get();
        knob.getChildren().setAll(dark ? Icons.of("moon", 16, "icon-blue") : Icons.of("sun", 16, "icon-amber"));
        double target = dark ? TRAVEL : 0;
        if (animate) {
            TranslateTransition t = new TranslateTransition(Duration.millis(260), knob);
            t.setToX(target);
            t.setInterpolator(Interpolator.EASE_BOTH);
            t.play();
        } else {
            knob.setTranslateX(target);
        }
    }
}
