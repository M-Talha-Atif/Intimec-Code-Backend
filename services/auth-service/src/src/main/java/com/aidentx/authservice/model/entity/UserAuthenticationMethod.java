package com.aidentx.authservice.model.entity;

import com.aidentx.authservice.enums.AuthenticationMethod;
import jakarta.persistence.*;

/**
 * Entity representing an authentication method used by a user.
 *
 * <p>This entity links a user to a specific authentication method (e.g., LDAP, OAUTH2, SAML),
 * allowing the system to track which methods the user has used at least once.</p>
 *
 * <p>Each (user, authMethod) pair is unique.</p>
 */
@Entity
@Table(name = "users_auth_methods", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "auth_method"})
})
public class UserAuthenticationMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_method", nullable = false)
    private AuthenticationMethod authMethod;


    public UserAuthenticationMethod() {}

    public UserAuthenticationMethod(User user, AuthenticationMethod authMethod) {
        this.user = user;
        this.authMethod = authMethod;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public AuthenticationMethod getAuthMethod() {
        return authMethod;
    }

    public void setAuthMethod(AuthenticationMethod authMethod) {
        this.authMethod = authMethod;
    }
}
