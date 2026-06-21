package com.aidentx.authservice.model.dto;

/**
 * Data Transfer Object representing a request to refresh an access token.
 * <p>
 * Contains the refresh token string issued to the client,
 * which will be validated and used to generate a new access token.
 * </p>
 */
public class RefreshTokenRequest {

    /**
     * The refresh token string provided by the client.
     */
    private String refreshToken;

    /**
     * Retrieves the refresh token string.
     *
     * @return the refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token string.
     *
     * @param refreshToken the refresh token to set
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
