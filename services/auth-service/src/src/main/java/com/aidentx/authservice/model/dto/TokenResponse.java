package com.aidentx.authservice.model.dto;

/**
 * Data Transfer Object (DTO) representing JWT tokens and their expiration times.
 * <p>
 * Typically returned upon successful authentication to allow the client
 * to authenticate subsequent requests and refresh access tokens when needed.
 */
public class TokenResponse {

    /**
     * The access JWT token string.
     */
    private String token;

    /**
     * The number of seconds until the access token expires.
     */
    private long expiresIn;

    /**
     * The refresh token string, used to obtain new access tokens without
     * requiring the user to re-authenticate.
     */
    private String refreshToken;

    /**
     * The session ID (UUID) for tracking this specific session.
     * Used for session activity tracking and multi-device session management.
     */
    private String sessionId;

    /**
     * Constructs a new TokenResponse with the provided access token, expiration time,
     * and refresh token.
     *
     * @param token        the access JWT token
     * @param expiresIn    the access token's validity duration in seconds
     * @param refreshToken the refresh token string
     */
    public TokenResponse(String token, long expiresIn, String refreshToken) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
    }

    /**
     * Constructs a new TokenResponse with session ID.
     *
     * @param token        the access JWT token
     * @param expiresIn    the access token's validity duration in seconds
     * @param refreshToken the refresh token string
     * @param sessionId    the session ID for this login session
     */
    public TokenResponse(String token, long expiresIn, String refreshToken, String sessionId) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.sessionId = sessionId;
    }

    /**
     * Returns the access JWT token.
     *
     * @return the access token string
     */
    public String getToken() {
        return token;
    }

    /**
     * Returns the number of seconds until the access token expires.
     *
     * @return the expiration time in seconds
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Returns the refresh token string.
     *
     * @return the refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Returns the session ID.
     *
     * @return the session ID (UUID as string)
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the session ID.
     *
     * @param sessionId the session ID to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
