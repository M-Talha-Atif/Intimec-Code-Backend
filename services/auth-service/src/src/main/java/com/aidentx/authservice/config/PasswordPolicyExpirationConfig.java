package com.aidentx.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for password policy expiration scheduling.
 * Allows customization of cron job timing through application properties.
 *
 * @author Aidentx
 * @since 1.0
 */
@ConfigurationProperties(prefix = "aidentx.auth.password-policy.expiration")
public class PasswordPolicyExpirationConfig {

    /**
     * Cron expression for password policy expiration extension job.
     * Default: "0 0 2 * * *" (daily at 2:00 AM)
     */
    private String cronExpression = "0 0 2 * * *";

    /**
     * Whether the automatic policy extension is enabled.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Maximum number of policies to process in a single job run.
     * This helps prevent memory issues with large numbers of expired policies.
     * Default: 1000
     */
    private int maxPoliciesPerRun = 1000;

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxPoliciesPerRun() {
        return maxPoliciesPerRun;
    }

    public void setMaxPoliciesPerRun(int maxPoliciesPerRun) {
        this.maxPoliciesPerRun = maxPoliciesPerRun;
    }
}
