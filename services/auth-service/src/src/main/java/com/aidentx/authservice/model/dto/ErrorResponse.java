package com.aidentx.authservice.model.dto;

import java.util.List;

/**
 * Represents a structured error response that contains a list of individual error messages.
 * Typically used in API responses to return multiple error details to the client.
 */
public class ErrorResponse {

    /**
     * List of error messages associated with the request.
     */
    private List<ErrorMessage> errors;

    /**
     * Constructs a new ErrorResponse with the provided list of error messages.
     *
     * @param errors the list of error messages to include in the response
     */
    public ErrorResponse(List<ErrorMessage> errors) {
        this.errors = errors;
    }

    /**
     * Returns the list of error messages.
     *
     * @return a list of {@link ErrorMessage} objects
     */
    public List<ErrorMessage> getErrors() {
        return errors;
    }

    /**
     * Sets the list of error messages.
     *
     * @param errors a list of {@link ErrorMessage} objects
     */
    public void setErrors(List<ErrorMessage> errors) {
        this.errors = errors;
    }

    /**
     * Represents an individual error message.
     * Used as part of the {@link ErrorResponse}.
     */
    public static class ErrorMessage {

        /**
         * The actual error message to be presented to the client.
         */
        private String msg;

        /**
         * Constructs a new ErrorMessage with the given message string.
         *
         * @param msg the error message content
         */
        public ErrorMessage(String msg) {
            this.msg = msg;
        }

        /**
         * Returns the error message string.
         *
         * @return the error message
         */
        public String getMsg() {
            return msg;
        }

        /**
         * Sets the error message string.
         *
         * @param msg the error message to set
         */
        public void setMsg(String msg) {
            this.msg = msg;
        }
    }
}
