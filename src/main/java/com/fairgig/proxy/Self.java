package com.fairgig.proxy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an int/Integer parameter that holds a USER id. The proxy only lets the call through
 * when it is the logged-in user's own id (you can only see and change your own account / data).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Self {
}
