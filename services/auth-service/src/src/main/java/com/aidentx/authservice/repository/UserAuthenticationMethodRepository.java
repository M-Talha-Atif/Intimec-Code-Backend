package com.aidentx.authservice.repository;

import com.aidentx.authservice.enums.AuthenticationMethod;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.entity.UserAuthenticationMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAuthenticationMethodRepository extends JpaRepository<UserAuthenticationMethod, Long> {

    /**
     * Find a UserAuthenticationMethod by user and authentication method.
     *
     * @param user the user
     * @param authMethod the authentication method
     * @return Optional containing the matching record if found
     */
    Optional<UserAuthenticationMethod> findByUserAndAuthMethod(User user, AuthenticationMethod authMethod);

    /**
     * Find all users who have used a specific authentication method.
     *
     * @param method the authentication method
     * @return list of UserAuthenticationMethod entries
     */
    List<UserAuthenticationMethod> findByAuthMethod(AuthenticationMethod method);

    /**
     * Count how many users have used a specific authentication method.
     *
     * @param method the authentication method
     * @return number of users
     */
    long countByAuthMethod(AuthenticationMethod method);
}
