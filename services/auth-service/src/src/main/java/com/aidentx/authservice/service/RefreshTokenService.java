package com.aidentx.authservice.service;

import com.aidentx.authservice.model.entity.RefreshToken;
import com.aidentx.authservice.model.entity.User;

/**
 * Interface for refresh token operations such as creation, validation, and revocation.
 */
public interface RefreshTokenService {

    /**
     * Creates and persists a new refresh token for the specified user using the provided token string.
     *
     * @param user  the user to associate with the new refresh token
     * @param token the already generated refresh token string
     * @return the saved {@link RefreshToken} entity
     */
    RefreshToken createRefreshToken(User user, String token);


    /**
     * Validates a refresh token, checking if it exists, is not revoked, and has not expired.
     *
     * @param token the refresh token string
     * @return the valid RefreshToken entity
     */
    RefreshToken validateRefreshToken(String token);

    /**
     * Marks the refresh token as revoked in the database.
     *
     * @param token the refresh token string
     */
    void revokeRefreshToken(String token);

    /**
     * Deletes a specific refresh token from the database.
     *
     * @param token the refresh token string to delete
     */
    void deleteByToken(String token);

    /**
     * Deletes all refresh tokens associated with a given user.
     *
     * @param user the user whose tokens should be deleted
     */
    void deleteAllUserTokens(User user);
}