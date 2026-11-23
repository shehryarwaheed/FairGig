package com.fairgig.factory.user;

/**
 * Everything a factory needs to build a user. Role-specific fields may be null when they
 * do not apply (e.g. platformId is only used for representatives).
 * The password hash is hidden from toString() so it can never leak into logs.
 */
public record NewUserData(
        String name,
        String email,
        String passwordHash,
        String phone,
        String city,
        String cityZone,
        Integer platformId) {

    @Override
    public String toString() {
        return "NewUserData{name=" + name + ", email=" + email + "}";
    }
}
