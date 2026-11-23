package com.fairgig.ui;

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

    public void showShell() {
        show(new AppShell(this::showLogin));
    }

    private void show(Node screen) {
        root.getChildren().setAll(screen);
        Anim.fadeSlideIn(screen, 12, 380, 0);
    }
}
