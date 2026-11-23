package com.fairgig.ui;

import java.io.InputStream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;

/** Light / dark theme + font setup. */
public final class AppTheme {

    private static final BooleanProperty DARK = new SimpleBooleanProperty(false);

    private AppTheme() {}

    public static BooleanProperty darkProperty() {
        return DARK;
    }

    /**
     * Tries to load Plus Jakarta Sans from /fonts (optional). Falls back to Segoe UI,
     * so the prototype works even when the font files are not added.
     */
    public static String loadFonts() {
        String family = "Segoe UI";
        String[] files = {"PlusJakartaSans-Regular.ttf", "PlusJakartaSans-Bold.ttf", "PlusJakartaSans-ExtraBold.ttf"};
        for (String file : files) {
            try (InputStream in = AppTheme.class.getResourceAsStream("/fonts/" + file)) {
                if (in != null) {
                    Font loaded = Font.loadFont(in, 14);
                    if (loaded != null) {
                        family = loaded.getFamily();
                    }
                }
            } catch (Exception ignored) {
                // optional fonts: ignore problems and keep the fallback
            }
        }
        return family;
    }

    /** Adds the stylesheet and keeps the ".dark" class of the scene root in sync. */
    public static void install(Scene scene, Parent root, String fontFamily) {
        scene.getStylesheets().add(AppTheme.class.getResource("/css/fairgig.css").toExternalForm());
        root.setStyle("-fx-font-family: '" + fontFamily + "';");
        apply(root, DARK.get());
        DARK.addListener((obs, was, isDark) -> apply(root, isDark));
    }

    private static void apply(Parent root, boolean dark) {
        root.getStyleClass().remove("dark");
        if (dark) {
            root.getStyleClass().add("dark");
        }
    }
}
