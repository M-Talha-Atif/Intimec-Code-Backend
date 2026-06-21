package com.aidentx.authservice.repository;

import com.aidentx.authservice.model.entity.PasswordResetToken;
import com.aidentx.authservice.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing {@link PasswordResetToken} entities.
 * <p>
 * Provides methods to find tokens by user or by token string.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    /**
     * Finds a password reset token by its token string.
     *
     * @param token the token string
     * @return an Optional containing the PasswordResetToken if found, or empty otherwise
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Finds a password reset token by the associated user.
     *
     * @param user the user
     * @return an Optional containing the PasswordResetToken if found, or empty otherwise
     */
    Optional<PasswordResetToken> findByUser(User user);
}
