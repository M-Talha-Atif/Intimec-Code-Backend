package com.aidentx.configuration.audit;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect for cleaning up thread-local user context after gRPC method execution.
 * This prevents memory leaks by ensuring thread-local storage is cleared.
 */
@Aspect
@Component
public class UserContextCleanupAspect {

    /**
     * Clears the user context after any gRPC service method execution.
     * This runs after all methods in the ConfigurationGrpcEndpoint class.
     */
    @After("execution(* com.aidentx.configuration.grpc.ConfigurationGrpcEndpoint.*(..))")
    public void cleanupUserContext() {
        UserContextHolder.clear();
    }
}
