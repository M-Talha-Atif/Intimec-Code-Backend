package com.aidentx.authservice.model.dto;

/**
 * Generic response Data Transfer Object (DTO) for status and message.
 * <p>
 * This class is typically used to return a simple status code and message in API responses,
 * such as indicating the success or failure of an operation (e.g., password update, deletion, etc.).
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     StatusMessageResponse response = new StatusMessageResponse(200, "Password updated successfully.");
 * </pre>
 *
 * @author aidentx
 * @since 1.0
 */
public class StatusMessageResponse {
    /**
     * The status code representing the result of the operation (e.g., HTTP status code).
     */
    private int status;
    /**
     * The message providing additional information about the operation result.
     */
    private String message;

    /**
     * Default constructor for frameworks that require it.
     */
    public StatusMessageResponse() {
    }

    /**
     * Constructs a StatusMessageResponse with the specified status and message.
     *
     * @param status the status code representing the result
     * @param message the message providing additional information
     */
    public StatusMessageResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * Gets the status code representing the result of the operation.
     *
     * @return the status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the status code representing the result of the operation.
     *
     * @param status the status code
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Gets the message providing additional information about the operation result.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message providing additional information about the operation result.
     *
     * @param message the message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}
