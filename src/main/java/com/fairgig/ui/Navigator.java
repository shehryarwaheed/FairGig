package com.fairgig.ui;

import com.fairgig.controller.AppContext;
import com.fairgig.model.User;
import com.fairgig.ui.auth.LoginView;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/** Swaps the top-level screens (login <-> app shell) inside the scene root. */
public class Navigator {

    private final StackPane root;

    public Navigator(StackPane root) {
        this.root = root;
    }

    public void showLogin() {
        show(new LoginView(this::showShell));
    }

    /** Opens the app for the user who just logged in. Logging out goes back to the login screen. */
    public void showShell(User user) {
        show(new AppShell(user, () -> {
            AppContext.get().auth().logout();
            showLogin();
        }));
    }

    private void show(Node screen) {
        root.getChildren().setAll(screen);
        Anim.fadeSlideIn(screen, 12, 380, 0);
    }
}
