package com.aidentx.core.notifications_management.repository;

import com.aidentx.core.notifications_management.enums.AuthMethod;
import com.aidentx.core.notifications_management.enums.EncryptionType;
import com.aidentx.core.notifications_management.model.entity.EmailConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link EmailConfiguration} entities.
 * <p>
 * This repository provides CRUD operations and custom query methods for email configurations,
 * including filtering by tenant, authentication method, and encryption type.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Repository
public interface EmailConfigurationRepository extends JpaRepository<EmailConfiguration, UUID> {
    
    /**
     * Retrieves email configuration for a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return an optional containing the email configuration if found
     */
    Optional<EmailConfiguration> findByTenantId(String tenantId);
    
    /**
     * Retrieves all email configurations using a specific authentication method.
     * 
     * @param authMethod the authentication method to filter by
     * @return a list of email configurations using the specified auth method
     */
    List<EmailConfiguration> findByAuthMethod(AuthMethod authMethod);
    
    /**
     * Retrieves all email configurations using a specific encryption type.
     * 
     * @param encryptionType the encryption type to filter by
     * @return a list of email configurations using the specified encryption
     */
    List<EmailConfiguration> findByEncryptionType(EncryptionType encryptionType);
    
    /**
     * Retrieves email configurations by SMTP hostname.
     * 
     * @param smtpHostname the SMTP hostname to search for
     * @return a list of email configurations with the specified hostname
     */
    List<EmailConfiguration> findBySmtpHostname(String smtpHostname);
    
    /**
     * Retrieves email configurations by sender email address.
     * 
     * @param emailAddress the email address to search for
     * @return a list of email configurations with the specified email address
     */
    List<EmailConfiguration> findByEmailAddress(String emailAddress);
    
    /**
     * Checks if an email configuration exists for a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return true if configuration exists, false otherwise
     */
    boolean existsByTenantId(String tenantId);
    
    /**
     * Retrieves email configurations by SMTP port number.
     * 
     * @param smtpPort the SMTP port number
     * @return a list of email configurations using the specified port
     */
    List<EmailConfiguration> findBySmtpPort(Long smtpPort);
    
    /**
     * Retrieves email configurations by authentication method and encryption type.
     * 
     * @param authMethod the authentication method
     * @param encryptionType the encryption type
     * @return a list of email configurations matching both criteria
     */
    @Query("SELECT ec FROM EmailConfiguration ec WHERE ec.authMethod = :authMethod AND ec.encryptionType = :encryptionType")
    List<EmailConfiguration> findByAuthMethodAndEncryptionType(@Param("authMethod") AuthMethod authMethod, 
                                                               @Param("encryptionType") EncryptionType encryptionType);
}
