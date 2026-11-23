package com.fairgig.controller;

import com.fairgig.model.Platform;
import com.fairgig.model.User;
import com.fairgig.service.auth.AuthService;
import com.fairgig.service.auth.RegistrationRequest;
import com.fairgig.service.platform.PlatformService;
import java.util.List;
import java.util.Optional;

/** What the login, register and profile screens call. Every method returns an {@link Outcome}. */
public class AuthController {

    private final AuthService auth;
    private final PlatformService platforms;

    public AuthController(AuthService auth, PlatformService platforms) {
        this.auth = auth;
        this.platforms = platforms;
    }

    public Outcome<User> register(RegistrationRequest request) {
        return Outcome.attempt(() -> auth.register(request));
    }

    public Outcome<User> login(String email, String password) {
        return Outcome.attempt(() -> auth.login(email, password));
    }

    public void logout() {
        Outcome.run(auth::logout);
    }

    public Optional<User> currentUser() {
        return auth.currentUser();
    }

    /** For the registration screen: the platforms a representative can be assigned to (FR-02). */
    public Outcome<List<Platform>> platformsForRegistration() {
        return Outcome.attempt(platforms::listActivePlatforms);
    }

    public Outcome<User> viewProfile(int userId) {
        return Outcome.attempt(() -> auth.viewProfile(userId));
    }

    public Outcome<User> updateProfile(int userId, String name, String email, String phone, String city, String cityZone) {
        return Outcome.attempt(() -> auth.updateProfile(userId, name, email, phone, city, cityZone));
    }

    public Outcome<Void> changePassword(int userId, String currentPassword, String newPassword) {
        return Outcome.run(() -> auth.changePassword(userId, currentPassword, newPassword));
    }
}
