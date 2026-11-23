package com.fairgig.ui;

import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** A labelled clay input with a place for its error message underneath. */
public class FormField extends VBox {

    private final HBox box;
    private final TextInputControl input;
    private final Label error = Widgets.label("", "error-text");

    public FormField(String label, String icon, String prompt, boolean password) {
        super(6);
        box = Widgets.field(icon, prompt, password);
        input = Widgets.input(box);
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);
        getChildren().addAll(Widgets.label(label, "field-label"), box, error);
    }

    public String text() {
        return input.getText() == null ? "" : input.getText();
    }

    public void setText(String text) {
        input.setText(text);
    }

    public TextInputControl input() {
        return input;
    }

    /** Shows the message under the field; null or empty clears it. */
    public void setError(String message) {
        boolean has = message != null && !message.isBlank();
        error.setText(has ? message : "");
        error.setVisible(has);
        error.setManaged(has);
    }

    public String error() {
        return error.getText();
    }

    /** Runs the action when the user leaves the field. */
    public void onLeave(Runnable action) {
        input.focusedProperty().addListener((obs, was, now) -> {
            if (!now) {
                action.run();
            }
        });
    }

    /** Runs the action whenever the text changes. */
    public void onChange(Runnable action) {
        input.textProperty().addListener((obs, was, now) -> action.run());
    }
}
