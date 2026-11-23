package com.fairgig.app;

import com.fairgig.ui.AppTheme;
import com.fairgig.ui.Navigator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Application bootstrap (package "app").
 * Prototype stage: opens the clickable UI shell (login + empty tabs for every role).
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        String fontFamily = AppTheme.loadFonts();

        StackPane root = new StackPane();
        Scene scene = new Scene(root, 1320, 820);
        AppTheme.install(scene, root, fontFamily);

        new Navigator(root).showLogin();

        stage.setTitle("FairGig - UI Prototype");
        stage.setMinWidth(1180);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
