package com.aidentx.authservice.model.entity;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entity representing a password reset token for a user.
 * <p>
 * Each user can have at most one active password reset token at a time.
 * The token is used to verify password reset requests and has an expiration time.
 * This entity is mapped to the <code>password_reset_token</code> table in the database.
 * </p>
 *
 * <p>Typical usage involves creating a <code>PasswordResetToken</code> when a user requests a password reset,
 * associating it with the user, and setting an expiration time. The token is then validated during the
 * password reset process.</p>
 *
 * @author aidentx
 * @since 1.0
 */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    /**
     * Surrogate primary key for the password reset token.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user associated with this password reset token. Unique per user.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * The unique token string used for password reset verification.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * The expiration timestamp for the password reset token.
     */
    @Column(nullable = false)
    private Instant expiresAt;

    /**
     * Default constructor for frameworks that require it.
     */
    public PasswordResetToken() {
    }

    /**
     * Constructs a PasswordResetToken with the specified user, token, and expiration time.
     *
     * @param user the user associated with this token
     * @param token the unique token string
     * @param expiresAt the expiration timestamp
     */
    public PasswordResetToken(User user, String token, Instant expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    /**
     * Gets the surrogate primary key for the password reset token.
     *
     * @return the token ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the surrogate primary key for the password reset token.
     *
     * @param id the token ID
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the user associated with this password reset token.
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user associated with this password reset token.
     *
     * @param user the user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the unique token string used for password reset verification.
     *
     * @return the token string
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the unique token string used for password reset verification.
     *
     * @param token the token string
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Gets the expiration timestamp for the password reset token.
     *
     * @return the expiration timestamp
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Sets the expiration timestamp for the password reset token.
     *
     * @param expiresAt the expiration timestamp
     */
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
