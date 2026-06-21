package com.aidentx.authservice.model.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Composite primary key class for the {@link RefreshToken} entity.
 * <p>
 * Combines user ID and token string to uniquely identify a refresh token.
 * This class must match the primary key fields and types in the {@code RefreshToken} entity.
 * It is used by JPA to manage entities with composite keys.
 * </p>
 *
 * <p>Typical usage is internal to JPA/Hibernate and not needed directly in business logic.</p>
 *
 * @author aidentx
 * @since 1.0
 */
public class RefreshTokenId implements Serializable {

    /**
     * The UUID of the user associated with the refresh token.
     * Must match the type of User's primary key (UUID assumed).
     */
    private UUID user;
    /**
     * The refresh token string.
     */
    private String token;

    /**
     * Default constructor for frameworks that require it.
     */
    public RefreshTokenId() {
    }

    /**
     * Constructs a RefreshTokenId with the specified user and token.
     *
     * @param user the UUID of the user
     * @param token the refresh token string
     */
    public RefreshTokenId(UUID user, String token) {
        this.user = user;
        this.token = token;
    }

    /**
     * Gets the UUID of the user associated with the refresh token.
     *
     * @return the user UUID
     */
    public UUID getUser() {
        return user;
    }

    /**
     * Sets the UUID of the user associated with the refresh token.
     *
     * @param user the user UUID
     */
    public void setUser(UUID user) {
        this.user = user;
    }

    /**
     * Gets the refresh token string.
     *
     * @return the token string
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the refresh token string.
     *
     * @param token the token string
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Checks equality based on user and token fields.
     *
     * @param o the object to compare
     * @return true if both user and token are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshTokenId)) return false;
        RefreshTokenId that = (RefreshTokenId) o;
        return Objects.equals(user, that.user) &&
                Objects.equals(token, that.token);
    }

    /**
     * Computes the hash code based on user and token fields.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(user, token);
    }
}