package com.aidentx.core.onboarding.model.enums;

/**
 * Represents the long-term, administrative lifecycle status of an Application.
 */
public enum LifecycleStatus {
    /**
     * The application configuration has been started but is not yet ready for use.
     * Schedulers and correlation engines will ignore it.
     */
    DRAFT,
    /**
     * The application is fully configured and in active use.
     * It is a candidate for scheduled aggregation and provisioning.
     */
    ACTIVE,
    /**
     * The application is being phased out but is still functional.
     * This serves as a warning to administrators.
     */
    DEPRECATED,
    /**
     * The application has been disabled and is no longer in use.
     * All automated processes will ignore it.
     */
    INACTIVE
}