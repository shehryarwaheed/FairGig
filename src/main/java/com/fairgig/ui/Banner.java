package com.fairgig.ui;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** A coloured message bar for success or error feedback. Hidden (and takes no space) until used. */
public class Banner extends HBox {

    private final Label text = new Label();

    public Banner() {
        super(10);
        setAlignment(Pos.CENTER_LEFT);
        text.setWrapText(true);
        text.setMaxWidth(Double.MAX_VALUE);
        getChildren().add(text);
        hide();
    }

    public void showError(String message) {
        show("banner-error", "close", message);
    }

    public void showError(List<String> messages) {
        showError(String.join("\n", messages));
    }

    public void showSuccess(String message) {
        show("banner-success", "check", message);
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
    }

    public String text() {
        return text.getText();
    }

    public boolean isError() {
        return isVisible() && getStyleClass().contains("banner-error");
    }

    private void show(String styleClass, String icon, String message) {
        getStyleClass().removeAll("banner", "banner-error", "banner-success");
        getStyleClass().addAll("banner", styleClass);
        getChildren().setAll(Icons.of(icon, 16), text);
        text.setText(message);
        setVisible(true);
        setManaged(true);
        Anim.fadeSlideIn(this, 6, 220, 0);
    }
}
