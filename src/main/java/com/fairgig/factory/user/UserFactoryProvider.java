package com.fairgig.factory.user;

import com.fairgig.model.Role;

/** Picks the factory that matches a role. */
public final class UserFactoryProvider {

    private static final UserFactory WORKER = new WorkerFactory();
    private static final UserFactory REPRESENTATIVE = new RepresentativeFactory();
    private static final UserFactory ADVOCATE = new AdvocateFactory();

    private UserFactoryProvider() {}

    public static UserFactory forRole(Role role) {
        return switch (role) {
            case WORKER -> WORKER;
            case VERIFIER -> REPRESENTATIVE;
            case ADVOCATE -> ADVOCATE;
        };
    }
}
