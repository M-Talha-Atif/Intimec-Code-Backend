package com.aidentx.authservice.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing password history for users.
 * <p>
 * This class maps to the <code>password_history</code> table and stores hashed passwords
 * that users have previously used. This is used to enforce password history policies
 * that prevent users from reusing recent passwords.
 * </p>
 * 
 * <p><strong>Security Note:</strong> Passwords are stored as hashed values using the same
 * hashing algorithm used for current passwords. This ensures that even if the database
 * is compromised, the actual passwords remain protected.</p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Entity
@Table(name = "password_history", indexes = {
    @Index(name = "idx_password_history_user_id", columnList = "user_id"),
    @Index(name = "idx_password_history_created_at", columnList = "created_at")
})
public class PasswordHistory {

    /**
     * Unique identifier for the password history entry (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * The user ID this password history belongs to.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * The hashed password that was previously used by the user.
     * <p>
     * <strong>Security Note:</strong> This field stores the password hash, NOT the plain text password.
     * The same hashing algorithm used for current passwords should be used here.
     * </p>
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Timestamp when this password was set (epoch time).
     * This helps with ordering password history and cleanup of old entries.
     */
    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    /**
     * Tenant ID this password history belongs to.
     * Used for multi-tenant isolation.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Default constructor required by JPA.
     */
    public PasswordHistory() {
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Constructor with all required fields.
     *
     * @param userId       the user ID
     * @param passwordHash the hashed password
     * @param tenantId     the tenant ID
     */
    public PasswordHistory(UUID userId, String passwordHash, String tenantId) {
        this();
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.tenantId = tenantId;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return "PasswordHistory{" +
                "id=" + id +
                ", userId=" + userId +
                ", passwordHash='[HIDDEN]'" + // Never log the actual hash for security
                ", createdAt=" + createdAt +
                ", tenantId='" + tenantId + '\'' +
                '}';
    }
}
