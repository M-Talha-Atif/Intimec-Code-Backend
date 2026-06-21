package com.aidentx.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for password expiration notification scheduling.
 * Allows customization of cron job timing and behavior through application properties.
 *
 * @author Aidentx
 * @since 1.0
 */
@ConfigurationProperties(prefix = "aidentx.auth.password-expiration-notification")
public class PasswordExpirationNotificationConfig {

    /**
     * Cron expression for password expiration notification job.
     * Default: "0 0 9 * * *" (daily at 9:00 AM)
     */
    private String cronExpression = "0 0 9 * * *";

    /**
     * Whether the password expiration notification cron job is enabled.
     * Default: true
     */
    private boolean enabled = true;

    /**
     * Maximum number of users to process per cron job run.
     * Helps prevent performance issues with large user bases.
     * Default: 1000
     */
    private int maxUsersPerRun = 1000;

    /**
     * Batch size for processing users in groups.
     * Default: 50
     */
    private int batchSize = 50;

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

    public int getMaxUsersPerRun() {
        return maxUsersPerRun;
    }

    public void setMaxUsersPerRun(int maxUsersPerRun) {
        this.maxUsersPerRun = maxUsersPerRun;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
