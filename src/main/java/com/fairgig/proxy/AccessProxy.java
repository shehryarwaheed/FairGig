package com.fairgig.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * PROXY pattern (NFR-03, FR-05, FR-06): wraps a service so that EVERY call is checked on the
 * server side before it reaches the real service. The screens only ever get the proxy.
 *
 * <pre>
 *   AuthService auth = AccessProxy.wrap(AuthService.class, new DefaultAuthService(...));
 * </pre>
 *
 * Rules (read from the annotations on the interface):
 * <ul>
 *   <li>{@link Access} on a method: who may call it. No annotation = denied (secure by default).</li>
 *   <li>{@link Self} on a parameter: must be the logged-in user's own id.</li>
 *   <li>{@link OwnPlatform} on a parameter: representatives may only use their own platform.</li>
 * </ul>
 * Errors thrown by the real service pass through unchanged.
 */
public final class AccessProxy {

    private AccessProxy() {}

    public static <T> T wrap(Class<T> serviceInterface, T target) {
        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException(serviceInterface.getName() + " must be an interface.");
        }
        if (!java.lang.reflect.Modifier.isPublic(serviceInterface.getModifiers())) {
            throw new IllegalArgumentException(serviceInterface.getName() + " must be a public interface.");
        }
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(target, args);       // toString / equals / hashCode
            }
            check(method, args == null ? new Object[0] : args);
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();                       // show the real error, not a wrapper
            }
        };
        Object proxy = Proxy.newProxyInstance(
            serviceInterface.getClassLoader(), new Class<?>[] {serviceInterface}, handler);
        return serviceInterface.cast(proxy);
    }

    private static void check(Method method, Object[] args) {
        Access access = method.getAnnotation(Access.class);
        if (access == null) {
            throw new com.fairgig.util.AccessDeniedException("This action is not available.");
        }
        if (access.anyone()) {
            return;
        }
        AccessPolicy.requireRole(access.roles());

        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                Integer value = args[i] instanceof Integer number ? number : null;
                if (annotation instanceof Self) {
                    AccessPolicy.requireSelf(value);
                } else if (annotation instanceof OwnPlatform) {
                    AccessPolicy.requirePlatform(value);
                }
            }
        }
    }
}
