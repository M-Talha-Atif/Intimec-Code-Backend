package com.aidentx.authservice.model.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Entity representing a refresh token used to renew access tokens without requiring user login.
 * <p>
 * This entity uses a composite primary key consisting of the user and the token string.
 * This ensures that a user can only have unique tokens, and avoids the need for a separate ID field.
 * The refresh token is used in authentication flows to issue new access tokens after the original
 * access token expires, without requiring the user to re-authenticate.
 * </p>
 *
 * <p>Typical usage involves creating a <code>RefreshToken</code> when a user logs in or requests a new token,
 * and validating it during token refresh operations.</p>
 *
 * @author aidentx
 * @since 1.0
 */
@Entity
@Table(name = "refresh_tokens")
@IdClass(RefreshTokenId.class)
public class RefreshToken implements Serializable {

    /**
     * The user associated with this refresh token.
     * Part of the composite primary key.
     */
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The refresh token string.
     * Part of the composite primary key.
     */
    @Id
    @Column(nullable = false)
    private String token;

    /**
     * The timestamp when this refresh token will expire.
     */
    @Column(nullable = false)
    private Instant expiryDate;

    /**
     * The timestamp when this refresh token was created.
     * Set only once during creation.
     */
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * The timestamp when this refresh token was last updated.
     * Automatically updated via {@link #onUpdate()}.
     */
    private Instant updatedAt;

    /**
     * JPA lifecycle callback to update the updatedAt timestamp before update.
     * This method is automatically called by JPA before the entity is updated.
     */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and setters

    /**
     * Gets the user associated with this refresh token.
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user associated with this refresh token.
     *
     * @param user the user
     */
    public void setUser(User user) {
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
     * Gets the timestamp when this refresh token will expire.
     *
     * @return the expiry date
     */
    public Instant getExpiryDate() {
        return expiryDate;
    }

    /**
     * Sets the timestamp when this refresh token will expire.
     *
     * @param expiryDate the expiry date
     */
    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    /**
     * Gets the timestamp when this refresh token was created.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Gets the timestamp when this refresh token was last updated.
     *
     * @return the last updated timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the timestamp when this refresh token was last updated.
     *
     * @param updatedAt the last updated timestamp
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
