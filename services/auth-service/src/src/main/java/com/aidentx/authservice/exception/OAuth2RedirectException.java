package com.aidentx.authservice.exception;

/**
 * Exception thrown to indicate an OAuth2 authentication failure that requires
 * redirecting the user to a specific URL, typically a front-end error page.
 */
public class OAuth2RedirectException extends RuntimeException {
    /**
     * The URL to which the user should be redirected after the exception.
     */
    private final String redirectUrl;
    /**
     * The error message describing the reason for the redirect.
     */
    private final String message;

    /**
     * Constructs a new OAuth2RedirectException with the specified redirect URL and message.
     * @param redirectUrl the URL to redirect the user to
     * @param message the error message
     */
    public OAuth2RedirectException(String redirectUrl, String message) {
        super(message);
        this.redirectUrl = redirectUrl;
        this.message = message;
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
    @Override
    public String getMessage() {
        return message;
    }
}
