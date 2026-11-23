package com.fairgig.factory.user;

import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.model.Worker;

/** Builds {@link Worker} accounts. */
public class WorkerFactory implements UserFactory {

    @Override
    public Role role() {
        return Role.WORKER;
    }

    @Override
    public User create(NewUserData data) {
        Worker worker = new Worker();
        worker.setName(data.name());
        worker.setEmail(data.email());
        worker.setPasswordHash(data.passwordHash());
        worker.setPhone(data.phone());
        worker.setCity(data.city());
        worker.setCityZone(data.cityZone());
        return worker;
    }
}
