package com.aidentx.iam.entitlement_management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's {@code @Scheduled} annotation processing.
 *
 * <p>Required for {@link com.aidentx.iam.entitlement_management.scheduler.CronSchedulerPoller}
 * to function. The poller runs every 60 seconds to check for CRON rules
 * that are due for evaluation.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}