package com.aidentx.core.onboarding.model.enums;

/**
 * Represents the short-term, dynamic operational status of an Application,
 * reflecting its current background activity.
 */
public enum OperationalStatus {
    /**
     * The application is not performing any background tasks. This is the normal
     * state.
     */
    IDLE,
    /**
     * An aggregation (data synchronization) job is currently running for this
     * application.
     */
    AGGREGATING,
    /**
     * A provisioning job is currently running for this application.
     */
    PROVISIONING,
    /**
     * The last background job (e.g., aggregation) failed.
     * This status requires administrative attention.
     */
    ERROR
}