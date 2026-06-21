package com.aidentx.applicationpermissions.shared.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Utility class to access Spring beans from static context.
 * This is useful when you need to access Spring-managed beans from entities
 * or other non-Spring managed components.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Component
public class ApplicationContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    /**
     * Set the ApplicationContext that this object runs in.
     * This method is called by Spring framework.
     *
     * @param applicationContext the ApplicationContext object to be used by this object
     * @throws BeansException if thrown by application context methods
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextHolder.applicationContext = applicationContext;
    }

    /**
     * Get the ApplicationContext.
     *
     * @return the ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Get a Spring bean by type.
     *
     * @param beanClass the class of the bean to retrieve
     * @param <T>       the type of the bean
     * @return the bean instance
     * @throws RuntimeException if the bean is not found or ApplicationContext is not set
     */
    public static <T> T getBean(Class<T> beanClass) {
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext is not set. Make sure ApplicationContextHolder is properly configured.");
        }
        return applicationContext.getBean(beanClass);
    }

    /**
     * Get a Spring bean by name.
     *
     * @param beanName the name of the bean to retrieve
     * @return the bean instance
     * @throws RuntimeException if the bean is not found or ApplicationContext is not set
     */
    public static Object getBean(String beanName) {
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext is not set. Make sure ApplicationContextHolder is properly configured.");
        }
        return applicationContext.getBean(beanName);
    }

    /**
     * Get a Spring bean by name and type.
     *
     * @param beanName  the name of the bean to retrieve
     * @param beanClass the class of the bean to retrieve
     * @param <T>       the type of the bean
     * @return the bean instance
     * @throws RuntimeException if the bean is not found or ApplicationContext is not set
     */
    public static <T> T getBean(String beanName, Class<T> beanClass) {
        if (applicationContext == null) {
            throw new RuntimeException("ApplicationContext is not set. Make sure ApplicationContextHolder is properly configured.");
        }
        return applicationContext.getBean(beanName, beanClass);
    }

    /**
     * Check if ApplicationContext is set and available.
     *
     * @return true if ApplicationContext is available, false otherwise
     */
    public static boolean isApplicationContextAvailable() {
        return applicationContext != null;
    }
}