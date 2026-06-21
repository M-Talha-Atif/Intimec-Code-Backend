package com.aidentx.access_portal_service.security;


/**
 * Thread-local holder for user context in workflow service.
 * Used by gRPC client interceptor to propagate user information.
 */
public class UserContextHolder {
    
    private static final ThreadLocal<UserContext> contextHolder = new ThreadLocal<>();
    
    public static void setContext(UserContext context) {
        contextHolder.set(context);
    }
    
    public static UserContext getContext() {
        return contextHolder.get();
    }
    
    public static void clearContext() {
        contextHolder.remove();
    }
}
