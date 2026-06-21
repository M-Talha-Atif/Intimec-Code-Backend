package com.aidentx.authservice.model.entity;

import com.aidentx.authservice.enums.Role;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing the association between a user and a role.
 * <p>
 * This class maps to the <code>users_role</code> table and links a user to a specific role
 * (e.g., ADMIN, USER, etc.). Each record represents a single role assignment for a user.
 * </p>
 *
 * <p>Typical usage involves assigning, updating, or querying user roles for authorization
 * and access control within the application.</p>
 */
@Entity
@Table(name = "users_role")
public class UserRole {

    /**
     * Unique identifier for the user-role association.
     */
    @Id
    private UUID id;

    /**
     * The user associated with this role.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The role assigned to the user.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    /**
     * No-args constructor (required by JPA).
     */
    public UserRole() {
    }

    /**
     * Constructs a UserRole with the specified ID, user, and role.
     *
     * @param id unique identifier for the association
     * @param user the user associated with this role
     * @param role the role assigned to the user
     */
    public UserRole(UUID id, User user, Role role) {
        this.id = id;
        this.user = user;
        this.role = role;
    }

    // Getters and setters

    /**
     * Gets the unique identifier for the user-role association.
     *
     * @return the association ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the user-role association.
     *
     * @param id the association ID
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the user associated with this role.
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user associated with this role.
     *
     * @param user the user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the role assigned to the user.
     *
     * @return the role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Sets the role assigned to the user.
     *
     * @param role the role
     */
    public void setRole(Role role) {
        this.role = role;
    }
}