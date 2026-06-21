package com.aidentx.configuration.audit.aspect;

import com.aidentx.configuration.audit.UserContextHolder;
import com.aidentx.configuration.service.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect to automatically manage the UserContextHolder for all REST controller operations.
 * This centralizes the logic for setting and clearing the user context,
 * removing repetitive boilerplate from the controller layer.
 */
@Aspect
@Component
public class UserContextAspect {

    /**
     * Pointcut that matches the execution of any public method in any class
     * within the 'com.aidentx.configuration.controller' package.
     */
    @Pointcut("execution(public * com.aidentx.configuration.controller.*.*(..))")
    public void controllerMethods() {}

    /**
     * Advice that wraps around controller methods to manage the UserContextHolder.
     * It sets the user context before the method executes and guarantees it is cleared afterwards.
     *
     * @param joinPoint The proceeding join point for the intercepted method.
     * @return The result of the original controller method.
     * @throws Throwable if the intercepted method throws an exception.
     */
    @Around("controllerMethods()")
    public Object manageUserContext(ProceedingJoinPoint joinPoint) throws Throwable {
        // Attempt to get the current HTTP request.
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            // We only need to set the context for methods that will perform write operations.
            // For simplicity and robustness, we can set it for all, as read operations won't use it.
            // A more advanced implementation could use annotations to target only specific methods.
            try {
                UserContextHolder.setUserContextFromAuthentication(RequestUtils.getClientIP(request));
            } catch (IllegalStateException e) {
                // This will occur if a non-authenticated user tries to access a protected endpoint.
                // Spring Security will handle the 401 response, but we log the context failure.
                // We proceed without a context, letting the security chain do its job.
            }
        }

        try {
            // Proceed with the original method execution (e.g., the controller's GET, PUT, POST).
            return joinPoint.proceed();
        } finally {
            // CRITICAL: Always clear the context from the thread-local after the request is complete
            // to prevent memory leaks and context bleeding between requests on reused threads.
            UserContextHolder.clear();
        }
    }
}