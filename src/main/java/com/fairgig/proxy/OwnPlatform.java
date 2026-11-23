package com.fairgig.proxy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an int/Integer parameter that holds a PLATFORM id. When the caller is a platform
 * representative, the proxy only allows their own assigned platform (FR-06).
 * Workers and advocates are not restricted by this annotation (use {@link Access} for roles).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface OwnPlatform {
}
