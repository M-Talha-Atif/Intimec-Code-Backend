package com.aidentx.authservice.model.dto;

import com.aidentx.authservice.service.Auth.AppConstants;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object (DTO) for account lock policy requests.
 * <p>
 * This class is used for creating and updating account lock policies via REST endpoints.
 * Contains all necessary fields for account lockout configuration.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
public class AccountLockPolicyRequest {

    /**
     * Maximum number of failed login attempts before account is locked.
     */
    @NotNull(message = AppConstants.VALIDATION_MAX_FAILED_LOGIN_ATTEMPTS_REQUIRED)
    @Min(value = 1, message = AppConstants.VALIDATION_MAX_FAILED_LOGIN_ATTEMPTS_MIN_VALUE)
    private Integer maxFailedLoginAttempts;

    /**
     * Duration in minutes for which the account remains locked.
     * This will be converted to seconds internally.
     */
    @NotNull(message = AppConstants.VALIDATION_LOCKOUT_DURATION_MINUTES_REQUIRED)
    @Min(value = 1, message = AppConstants.VALIDATION_LOCKOUT_DURATION_MINUTES_MIN_VALUE)
    private Integer lockoutDurationMinutes;

    /**
     * Delay in seconds to increase lockout duration after each subsequent failure.
     * Set to 0 to disable progressive delay, or specify seconds to add per failure.
     */
    @NotNull(message = AppConstants.VALIDATION_INCREASE_DELAY_AFTER_FAILURE_SECONDS_REQUIRED)
    @Min(value = 0, message = AppConstants.VALIDATION_INCREASE_DELAY_AFTER_FAILURE_SECONDS_MIN_VALUE)
    private Integer increaseDelayAfterFailureSeconds;

    /**
     * Default constructor.
     */
    public AccountLockPolicyRequest() {
    }

    // Getters and Setters

    public Integer getMaxFailedLoginAttempts() {
        return maxFailedLoginAttempts;
    }

    public void setMaxFailedLoginAttempts(Integer maxFailedLoginAttempts) {
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
    }

    public Integer getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(Integer lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    public Integer getIncreaseDelayAfterFailureSeconds() {
        return increaseDelayAfterFailureSeconds;
    }

    public void setIncreaseDelayAfterFailureSeconds(Integer increaseDelayAfterFailureSeconds) {
        this.increaseDelayAfterFailureSeconds = increaseDelayAfterFailureSeconds;
    }

    @Override
    public String toString() {
        return "AccountLockPolicyRequest{" +
                "maxFailedLoginAttempts=" + maxFailedLoginAttempts +
                ", lockoutDurationMinutes=" + lockoutDurationMinutes +
                ", increaseDelayAfterFailureSeconds=" + increaseDelayAfterFailureSeconds +
                '}';
    }
}
