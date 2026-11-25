package com.fairgig.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.Region;

/** Button variants shared by the pages (the style itself lives in the CSS as ".btn-small"). */
public final class Buttons {

    private Buttons() {}

    /** A compact secondary button for rows of actions, tags and quick dates. */
    public static Button small(String text) {
        Button button = Widgets.secondaryButton(text);
        button.getStyleClass().add("btn-small");
        button.setMaxWidth(Region.USE_PREF_SIZE);
        return button;
    }
}
