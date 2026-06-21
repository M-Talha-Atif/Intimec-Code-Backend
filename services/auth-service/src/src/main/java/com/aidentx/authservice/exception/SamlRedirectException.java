package com.aidentx.authservice.exception;

import com.aidentx.authservice.config.AppConstants;

/**
 * Exception thrown to indicate a SAML authentication failure that requires
 * redirecting the user to a specific URL, typically a front-end error page.
 */
public class SamlRedirectException extends RuntimeException {
    /**
     * The URL to which the user should be redirected after the exception.
     */
    private final String redirectUrl;
    /**
     * The error message describing the reason for the redirect.
     */
    private final String errorMessage;

    /**
     * Constructs a new SamlRedirectException with the specified redirect URL and error message.
     * If the error message is null, a default message is used.
     * @param redirectUrl the URL to redirect the user to
     * @param errorMessage the error message (may be null)
     */
    public SamlRedirectException(String redirectUrl, String errorMessage) {
        super(errorMessage != null ? errorMessage : AppConstants.DEFAULT_SAML_AUTH_ERROR_MESSAGE);
        this.redirectUrl = redirectUrl;
        this.errorMessage = errorMessage;
    }


    /**
     * Returns the redirect URL associated with this exception.
     * @return the redirect URL
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * Returns the error message associated with this exception.
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }
}
