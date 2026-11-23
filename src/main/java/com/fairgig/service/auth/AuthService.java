package com.fairgig.service.auth;

import com.fairgig.model.User;
import com.fairgig.proxy.Access;
import com.fairgig.proxy.Self;
import java.util.Optional;

/**
 * Accounts: register, log in, log out, view and update the profile (FR-01 to FR-04).
 * <p>
 * The annotations tell the Proxy who may call each method. Screens must use the proxied instance:
 * {@code AccessProxy.wrap(AuthService.class, new DefaultAuthService(...))}.
 * Problems are reported as ValidationException with messages the user can act on (NFR-10).
 */
public interface AuthService {

    /** Validates the request, creates the right kind of user through the factory and saves it. */
    @Access(anyone = true)
    User register(RegistrationRequest request);

    /** Checks the credentials and starts the session. */
    @Access(anyone = true)
    User login(String email, String password);

    @Access(anyone = true)
    void logout();

    @Access(anyone = true)
    Optional<User> currentUser();

    /** You can only view your own profile. */
    @Access
    User viewProfile(@Self int userId);

    /**
     * Updates your own profile. Phone, city and zone only apply to workers.
     * The role itself can never be changed here.
     */
    @Access
    User updateProfile(@Self int userId, String name, String email, String phone, String city, String cityZone);

    @Access
    void changePassword(@Self int userId, String currentPassword, String newPassword);
}
