package com.aidentx.workflowsservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.quartz.CronScheduleBuilder;

/**
 * Validator for cron expressions using Quartz
 */
public class CronExpressionValidator implements ConstraintValidator<ValidCronExpression, String> {

    private boolean required;

    @Override
    public void initialize(ValidCronExpression constraintAnnotation) {
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(String cronExpression, ConstraintValidatorContext context) {
        // Allow null/blank if not required
        if (cronExpression == null || cronExpression.isBlank()) {
            return !required;
        }

        try {
            CronScheduleBuilder.cronSchedule(cronExpression);
            return true;
        } catch (Exception e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Invalid cron expression: " + e.getMessage()
            ).addConstraintViolation();
            return false;
        }
    }
}
