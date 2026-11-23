package com.fairgig.proxy;

import com.fairgig.model.Role;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Says who may call a service method. Put it on the METHODS OF THE SERVICE INTERFACE;
 * the {@link AccessProxy} reads it before every call (FR-05, NFR-03).
 *
 * <pre>
 *   &#64;Access(anyone = true)                 // guests too (login, register)
 *   &#64;Access                                // any logged-in user
 *   &#64;Access(roles = Role.WORKER)           // workers only
 *   &#64;Access(roles = {Role.VERIFIER, Role.ADVOCATE})
 * </pre>
 *
 * A method with NO annotation is denied to everybody (secure by default).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Access {

    /** Roles that may call the method. Empty = any logged-in user. */
    Role[] roles() default {};

    /** true = no login needed. */
    boolean anyone() default false;
}
