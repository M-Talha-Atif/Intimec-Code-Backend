package com.aidentx.iam.security;

/**
 * A ThreadLocal holder for the current user's context.
 * This makes the user's identity available throughout the request processing
 * thread.
 */
public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    public static void setContext(UserContext userContext) {
        CONTEXT.set(userContext);
    }

    public static UserContext getContext() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}