package com.fairgig.service.auth;

import com.fairgig.model.Role;

/**
 * What the registration screen sends (FR-01, FR-02).
 * Worker fields (phone, city, cityZone) and platformId (representatives) may be null when they do not apply.
 * The password is hidden from toString() so it can never reach a log.
 */
public record RegistrationRequest(
        String name,
        String email,
        String password,
        Role role,
        String phone,
        String city,
        String cityZone,
        Integer platformId) {

    @Override
    public String toString() {
        return "RegistrationRequest{name=" + name + ", email=" + email + ", role=" + role + "}";
    }
}
