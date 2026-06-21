package com.aidentx.core.notifications_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * DTO for testing template managers with dynamic content.
 * <p>
 * This simplified DTO allows users to test template processing by providing 
 * just the template content and test email address. The tenant is automatically 
 * extracted from JWT.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-09-29
 */
public class TemplateManagerTestDto {

    @JsonProperty("templateSubject")
    private String templateSubject;

    @NotBlank(message = "Email body is required")
    @JsonProperty("emailBody")
    private String emailBody;

    @NotBlank(message = "Test email address is required")
    @Email(message = "Must be a valid email address")
    @JsonProperty("testEmailAddress")
    private String testEmailAddress;

    // Optional: Template variables for testing (can be null or empty for testing purposes)
    @JsonProperty("templateVariables")
    private Map<String, Object> templateVariables;

    /**
     * Default constructor.
     */
    public TemplateManagerTestDto() {}

    /**
     * Constructor with essential fields.
     *
     * @param templateSubject the subject line
     * @param emailBody the email body content
     * @param testEmailAddress the test email address
     * @param templateVariables the template variables
     */
    public TemplateManagerTestDto(String templateSubject, String emailBody, String testEmailAddress,
                                 Map<String, Object> templateVariables) {
        this.templateSubject = templateSubject;
        this.emailBody = emailBody;
        this.testEmailAddress = testEmailAddress;
        this.templateVariables = templateVariables;
    }

    // Getters and Setters

    public String getTemplateSubject() {
        return templateSubject;
    }

    public void setTemplateSubject(String templateSubject) {
        this.templateSubject = templateSubject;
    }

    public String getEmailBody() {
        return emailBody;
    }

    public void setEmailBody(String emailBody) {
        this.emailBody = emailBody;
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

    @Override
    public String toString() {
        return "TemplateManagerTestDto{" +
                "templateSubject='" + templateSubject + '\'' +
                ", emailBody='" + emailBody + '\'' +
                ", testEmailAddress='" + testEmailAddress + '\'' +
                ", templateVariables=" + templateVariables +
                '}';
    }
}