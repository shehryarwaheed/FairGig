package com.fairgig.model;

/** A worker-rights advocate (tables "users" + "advocates"). Has no extra fields of its own. */
public class Advocate extends User {

    public Advocate() {
        super(Role.ADVOCATE);
    }
}
