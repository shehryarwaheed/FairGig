package com.fairgig.factory.user;

import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.util.ValidationException;

/** Builds {@link PlatformRepresentative} accounts. A representative must have an assigned platform (FR-02). */
public class RepresentativeFactory implements UserFactory {

    @Override
    public Role role() {
        return Role.VERIFIER;
    }

    @Override
    public User create(NewUserData data) {
        if (data.platformId() == null) {
            throw new ValidationException("Please select your assigned platform.");
        }
        PlatformRepresentative rep = new PlatformRepresentative();
        rep.setName(data.name());
        rep.setEmail(data.email());
        rep.setPasswordHash(data.passwordHash());
        rep.setPlatformId(data.platformId());
        return rep;
    }
}
