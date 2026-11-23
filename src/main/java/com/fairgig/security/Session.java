package com.fairgig.security;

import com.fairgig.model.User;
import java.util.Optional;

/**
 * Remembers who is logged in while the app runs. Set by login, cleared by logout.
 * The Proxy reads it to decide what the current user is allowed to do (NFR-03).
 */
public final class Session {

    private static volatile User currentUser;

    private Session() {}

    public static Optional<User> currentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void start(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
