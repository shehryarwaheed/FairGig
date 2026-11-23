package com.fairgig.ui;

import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/** Small, soft animations used across the prototype. */
public final class Anim {

    private static final String SCALE_KEY = "fg.scaleAnim";

    private Anim() {}

    /** Fades a node in while it slides up from {@code fromY} pixels below. */
    public static void fadeSlideIn(Node node, double fromY, double ms, double delayMs) {
        node.setOpacity(0);
        node.setTranslateY(fromY);
        FadeTransition fade = new FadeTransition(Duration.millis(ms), node);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(ms), node);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition both = new ParallelTransition(fade, slide);
        both.setDelay(Duration.millis(delayMs));
        both.play();
    }

    /** Fade-slide for a list of nodes, one after the other. */
    public static void stagger(List<? extends Node> nodes, double fromY, double ms, double stepMs) {
        for (int i = 0; i < nodes.size(); i++) {
            fadeSlideIn(nodes.get(i), fromY, ms, i * stepMs);
        }
    }

    /** Clay "press" feel: grows slightly on hover, squishes while pressed. */
    public static void pressable(Node node) {
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> scaleTo(node, 1.03));
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> scaleTo(node, 1.0));
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> scaleTo(node, 0.96));
        node.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> scaleTo(node, node.isHover() ? 1.03 : 1.0));
    }

    private static void scaleTo(Node node, double target) {
        Object previous = node.getProperties().get(SCALE_KEY);
        if (previous instanceof ScaleTransition old) {
            old.stop();
        }
        ScaleTransition st = new ScaleTransition(Duration.millis(130), node);
        st.setToX(target);
        st.setToY(target);
        st.setInterpolator(Interpolator.EASE_OUT);
        node.getProperties().put(SCALE_KEY, st);
        st.play();
    }

    /** Gentle endless floating (used for illustration pieces). */
    public static void floatLoop(Node node, double dy, double seconds, double delaySeconds) {
        TranslateTransition t = new TranslateTransition(Duration.seconds(seconds), node);
        t.setByY(dy);
        t.setAutoReverse(true);
        t.setCycleCount(Timeline.INDEFINITE);
        t.setInterpolator(Interpolator.EASE_BOTH);
        t.setDelay(Duration.seconds(delaySeconds));
        t.play();
    }

    /** Slow "breathing" scale (sparkles, halos). */
    public static void breathe(Node node, double toScale, double seconds, double delaySeconds) {
        ScaleTransition s = new ScaleTransition(Duration.seconds(seconds), node);
        s.setToX(toScale);
        s.setToY(toScale);
        s.setAutoReverse(true);
        s.setCycleCount(Timeline.INDEFINITE);
        s.setInterpolator(Interpolator.EASE_BOTH);
        s.setDelay(Duration.seconds(delaySeconds));
        s.play();
    }

    /** Soft shimmer for skeleton placeholders. */
    public static void shimmer(Node node, double delaySeconds) {
        FadeTransition f = new FadeTransition(Duration.seconds(1.3), node);
        f.setFromValue(0.55);
        f.setToValue(1.0);
        f.setAutoReverse(true);
        f.setCycleCount(Timeline.INDEFINITE);
        f.setDelay(Duration.seconds(delaySeconds));
        f.play();
    }
}
