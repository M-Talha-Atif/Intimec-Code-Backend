package com.aidentx.core.notifications_management.model.dto;

/**
 * Data Transfer Object for email template test results.
 * <p>
 * This DTO contains the results of testing email template processing
 * and sending, including success status and processed content.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
public class EmailTemplateTestResultDto {
    
    private boolean success;
    private String message;
    private String processedSubject;
    private String processedBody;
    private String templateName;
    private String smtpHost;
    private Integer smtpPort;
    private String fromEmail;
    private String toEmail;
    
    /**
     * Default constructor.
     */
    public EmailTemplateTestResultDto() {}
    
    /**
     * Constructor for successful test.
     * 
     * @param processedSubject the processed email subject
     * @param processedBody the processed email body
     * @param templateName the name of the template used
     * @param smtpHost the SMTP host used
     * @param smtpPort the SMTP port used
     * @param fromEmail the sender email address
     * @param toEmail the recipient email address
     */
    public EmailTemplateTestResultDto(String processedSubject, String processedBody, String templateName,
                                     String smtpHost, Integer smtpPort, String fromEmail, String toEmail) {
        this.success = true;
        this.message = "Email template test completed successfully";
        this.processedSubject = processedSubject;
        this.processedBody = processedBody;
        this.templateName = templateName;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.fromEmail = fromEmail;
        this.toEmail = toEmail;
    }
    
    /**
     * Constructor for failed test.
     * 
     * @param errorMessage the error message
     */
    public EmailTemplateTestResultDto(String errorMessage) {
        this.success = false;
        this.message = errorMessage;
    }
    
    /**
     * Gets the success status.
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * Sets the success status.
     * 
     * @param success the success status to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    /**
     * Gets the result message.
     * 
     * @return the message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Sets the result message.
     * 
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Gets the processed email subject.
     * 
     * @return the processed subject
     */
    public String getProcessedSubject() {
        return processedSubject;
    }
    
    /**
     * Sets the processed email subject.
     * 
     * @param processedSubject the processed subject to set
     */
    public void setProcessedSubject(String processedSubject) {
        this.processedSubject = processedSubject;
    }
    
    /**
     * Gets the processed email body.
     * 
     * @return the processed body
     */
    public String getProcessedBody() {
        return processedBody;
    }
    
    /**
     * Sets the processed email body.
     * 
     * @param processedBody the processed body to set
     */
    public void setProcessedBody(String processedBody) {
        this.processedBody = processedBody;
    }
    
    /**
     * Gets the template name.
     * 
     * @return the template name
     */
    public String getTemplateName() {
        return templateName;
    }
    
    /**
     * Sets the template name.
     * 
     * @param templateName the template name to set
     */
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    /**
     * Gets the SMTP host.
     * 
     * @return the SMTP host
     */
    public String getSmtpHost() {
        return smtpHost;
    }
    
    /**
     * Sets the SMTP host.
     * 
     * @param smtpHost the SMTP host to set
     */
    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }
    
    /**
     * Gets the SMTP port.
     * 
     * @return the SMTP port
     */
    public Integer getSmtpPort() {
        return smtpPort;
    }
    
    /**
     * Sets the SMTP port.
     * 
     * @param smtpPort the SMTP port to set
     */
    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }
    
    /**
     * Gets the sender email address.
     * 
     * @return the from email
     */
    public String getFromEmail() {
        return fromEmail;
    }
    
    /**
     * Sets the sender email address.
     * 
     * @param fromEmail the from email to set
     */
    public void setFromEmail(String fromEmail) {
        this.fromEmail = fromEmail;
    }
    
    /**
     * Gets the recipient email address.
     * 
     * @return the to email
     */
    public String getToEmail() {
        return toEmail;
    }
    
    /**
     * Sets the recipient email address.
     * 
     * @param toEmail the to email to set
     */
    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }
}
