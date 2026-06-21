package com.aidentx.authservice.repository;

import com.aidentx.authservice.model.entity.RefreshToken;
import com.aidentx.authservice.model.entity.RefreshTokenId;
import com.aidentx.authservice.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for performing CRUD operations on refresh tokens.
 * Uses composite key (RefreshTokenId) consisting of user and token.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, RefreshTokenId> {

    /**
     * Finds a refresh token by its token string.
     *
     * @param token the token string
     * @return an Optional containing the refresh token if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Retrieves all refresh tokens associated with a given user.
     *
     * @param user the user entity
     * @return a list of refresh tokens for the user
     */
    List<RefreshToken> findAllByUser(User user);

    /**
     * Deletes all refresh tokens belonging to a given user.
     *
     * @param user the user entity
     */
    void deleteAllByUser(User user);

    /**
     * Deletes a refresh token by its token string.
     *
     * @param token the refresh token string
     * @return the number of rows deleted
     */
    long deleteByToken(String token);

    /**
     * Optional: Delete a refresh token by user and token composite key.
     *
     * @param user  the user entity
     * @param token the refresh token string
     * @return number of rows deleted
     */
    long deleteByUserAndToken(User user, String token);
}
