package com.fairgig.factory.user;

import com.fairgig.model.Advocate;
import com.fairgig.model.Role;
import com.fairgig.model.User;

/** Builds {@link Advocate} accounts. */
public class AdvocateFactory implements UserFactory {

    @Override
    public Role role() {
        return Role.ADVOCATE;
    }

    @Override
    public User create(NewUserData data) {
        Advocate advocate = new Advocate();
        advocate.setName(data.name());
        advocate.setEmail(data.email());
        advocate.setPasswordHash(data.passwordHash());
        return advocate;
    }
}
