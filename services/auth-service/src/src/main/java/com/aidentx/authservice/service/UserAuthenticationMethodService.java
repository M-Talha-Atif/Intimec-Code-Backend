package com.aidentx.authservice.service;

import com.aidentx.authservice.enums.AuthenticationMethod;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.entity.UserAuthenticationMethod;
import com.aidentx.authservice.repository.UserAuthenticationMethodRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service class responsible for managing the association between users and
 * their authentication methods.
 * 
 * <p>This service allows tracking which authentication methods a user has
 * used to log in, such as LDAP, OAuth2, or SAML. It helps in maintaining
 * usage records and generating statistics on authentication method adoption.</p>
 * 
 * <p>Typical use cases:
 * <ul>
 *   <li>Record when a user authenticates using a specific method.</li>
 *   <li>Retrieve counts of users by authentication method for reporting.</li>
 * </ul>
 * </p>
 */
@Service
public class UserAuthenticationMethodService {

    private final UserAuthenticationMethodRepository userAuthMethodRepository;

    /**
     * Constructs the service with the given repository.
     *
     * @param userAuthMethodRepository repository for UserAuthenticationMethod entities
     */
    public UserAuthenticationMethodService(UserAuthenticationMethodRepository userAuthMethodRepository) {
        this.userAuthMethodRepository = userAuthMethodRepository;
    }

    /**
     * Tracks that a user has used a specific authentication method.
     * If the user-method record does not exist, it creates a new one.
     *
     * @param user the user who authenticated
     * @param method the authentication method used
     */
    public void trackUserMethod(User user, AuthenticationMethod method) {
        Optional<UserAuthenticationMethod> existing = userAuthMethodRepository.findByUserAndAuthMethod(user, method);

        if (existing.isEmpty()) {
            UserAuthenticationMethod newRecord = new UserAuthenticationMethod(user, method);
            userAuthMethodRepository.save(newRecord);
        }
    }

    /**
     * Counts how many users have used the specified authentication method.
     *
     * @param method the authentication method to count users for
     * @return the number of users who have used the method
     */
    public long countUsersByMethod(AuthenticationMethod method) {
        return userAuthMethodRepository.countByAuthMethod(method);
    }
}
