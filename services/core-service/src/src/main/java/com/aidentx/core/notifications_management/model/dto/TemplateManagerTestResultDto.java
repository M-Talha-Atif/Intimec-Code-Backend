package com.aidentx.core.notifications_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO for template manager test results.
 * <p>
 * This DTO contains the results of testing a template manager,
 * including the processed content and test status.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-09-29
 */
public class TemplateManagerTestResultDto {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("processedSubject")
    private String processedSubject;

    @JsonProperty("processedBody")
    private String processedBody;

    @JsonProperty("testEmailAddress")
    private String testEmailAddress;

    @JsonProperty("templateVariables")
    private Map<String, Object> templateVariables;

    @JsonProperty("emailConfiguration")
    private String emailConfiguration;

    /**
     * Default constructor.
     */
    public TemplateManagerTestResultDto() {}

    /**
     * Constructor for successful test result.
     *
     * @param success whether the test was successful
     * @param message the test result message
     * @param processedSubject the processed subject
     * @param processedBody the processed body
     * @param testEmailAddress the test email address
     * @param templateVariables the template variables used
     * @param emailConfiguration the email configuration used
     */
    public TemplateManagerTestResultDto(boolean success, String message, String processedSubject,
                                       String processedBody, String testEmailAddress,
                                       Map<String, Object> templateVariables, String emailConfiguration) {
        this.success = success;
        this.message = message;
        this.processedSubject = processedSubject;
        this.processedBody = processedBody;
        this.testEmailAddress = testEmailAddress;
        this.templateVariables = templateVariables;
        this.emailConfiguration = emailConfiguration;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getProcessedSubject() {
        return processedSubject;
    }

    public void setProcessedSubject(String processedSubject) {
        this.processedSubject = processedSubject;
    }

    public String getProcessedBody() {
        return processedBody;
    }

    public void setProcessedBody(String processedBody) {
        this.processedBody = processedBody;
    }

    public String getTestEmailAddress() {
        return testEmailAddress;
    }

    public void setTestEmailAddress(String testEmailAddress) {
        this.testEmailAddress = testEmailAddress;
    }

    public Map<String, Object> getTemplateVariables() {
        return templateVariables;
    }

    public void setTemplateVariables(Map<String, Object> templateVariables) {
        this.templateVariables = templateVariables;
    }

    public String getEmailConfiguration() {
        return emailConfiguration;
    }

    public void setEmailConfiguration(String emailConfiguration) {
        this.emailConfiguration = emailConfiguration;
    }

    @Override
    public String toString() {
        return "TemplateManagerTestResultDto{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", processedSubject='" + processedSubject + '\'' +
                ", processedBody='" + processedBody + '\'' +
                ", testEmailAddress='" + testEmailAddress + '\'' +
                ", templateVariables=" + templateVariables +
                ", emailConfiguration='" + emailConfiguration + '\'' +
                '}';
    }
}