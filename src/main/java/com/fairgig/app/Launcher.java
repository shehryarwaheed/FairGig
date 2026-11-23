package com.fairgig.app;

/**
 * Entry point for the IntelliJ Run button.
 * It does NOT extend Application, which avoids the
 * "JavaFX runtime components are missing" error when JavaFX comes from Maven.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        Main.main(args);
    }
}
