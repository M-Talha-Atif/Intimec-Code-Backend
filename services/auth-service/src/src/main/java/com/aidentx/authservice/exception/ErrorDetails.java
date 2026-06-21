package com.aidentx.authservice.exception;

import java.time.Instant;

/**
 * Represents a structured error response to be sent to clients.
 * <p>
 * Contains the following fields:
 * <ul>
 *   <li>timestamp: The time the error occurred (ISO-8601 format).</li>
 *   <li>status: The HTTP status code associated with the error.</li>
 *   <li>error: A short, human-readable error title (e.g., "Not Found").</li>
 *   <li>message: A detailed error message for the client.</li>
 *   <li>path: (Optional) The request path that caused the error.</li>
 * </ul>
 * This class is typically used in global exception handlers to provide consistent error responses.
 * </p>
 *
 * Example usage:
 * <pre>
 *   new ErrorDetails(Instant.now(), 404, "Not Found", "Resource does not exist", "/api/resource/1");
 * </pre>
 *
 * @author aidentx Development Team
 */
public class ErrorDetails {

    private final String timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;

    /**
     * Constructs a new ErrorDetails object with all fields.
     *
     * @param timestamp the time the error occurred
     * @param status the HTTP status code
     * @param error a short error title
     * @param message a detailed error message
     * @param path the request path that caused the error (optional)
     */
    public ErrorDetails(Instant timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp.toString();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * @return the timestamp of the error (ISO-8601 string)
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * @return the HTTP status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * @return the error title
     */
    public String getError() {
        return error;
    }

    /**
     * @return the detailed error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the request path that caused the error (may be null)
     */
    public String getPath() {
        return path;
    }
}
