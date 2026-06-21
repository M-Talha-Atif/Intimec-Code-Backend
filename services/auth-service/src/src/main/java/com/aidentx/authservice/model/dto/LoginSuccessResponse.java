package com.aidentx.authservice.model.dto;

import java.time.Instant;

/**
 * Generic response wrapper used to represent a successful login operation.
 *
 * @param <T> the type of the data returned (e.g., token details, user info, etc.)
 */
public class LoginSuccessResponse<T> {

    /**
     * The timestamp indicating when the response was created.
     */
    private Instant timestamp;

    /**
     * The HTTP status code (e.g., 200 for success).
     */
    private int status;

    /**
     * The actual response data (such as token or user info).
     */
    private T data;

    /**
     * Constructs a LoginSuccessResponse with the specified status and data.
     * Automatically sets the timestamp to the current time.
     *
     * @param status the HTTP status code
     * @param data   the response payload
     */
    public LoginSuccessResponse(int status, T data) {
        this.timestamp = Instant.now();
        this.status = status;
        this.data = data;
    }

    /**
     * Returns the timestamp when this response was created.
     *
     * @return the timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the response data.
     *
     * @return the response payload
     */
    public T getData() {
        return data;
    }
}
