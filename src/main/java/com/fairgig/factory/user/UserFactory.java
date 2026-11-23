package com.fairgig.factory.user;

import com.fairgig.model.Role;
import com.fairgig.model.User;

/**
 * FACTORY METHOD pattern: each role has its own factory that knows how to build the right
 * kind of user (Worker, PlatformRepresentative or Advocate). The caller only asks for
 * "a user of this role" and never uses "new Worker()" directly.
 */
public interface UserFactory {

    /** The role this factory builds. */
    Role role();

    /** Builds a new, not yet saved user from the given data. */
    User create(NewUserData data);
}
