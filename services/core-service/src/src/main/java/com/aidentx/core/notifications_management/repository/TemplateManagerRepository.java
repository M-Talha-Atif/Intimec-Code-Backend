package com.aidentx.core.notifications_management.repository;

import com.aidentx.core.notifications_management.enums.LocaleType;
import com.aidentx.core.notifications_management.enums.OperationType;
import com.aidentx.core.notifications_management.model.entity.TemplateManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link TemplateManager} entities.
 * <p>
 * This repository provides CRUD operations and custom query methods for template managers,
 * including filtering by tenant, trigger type, language, and enabled status.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Repository
public interface TemplateManagerRepository extends JpaRepository<TemplateManager, UUID> {
    
    /**
     * Retrieves all templates for a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return a list of templates for the specified tenant
     */
    List<TemplateManager> findByTenantId(String tenantId);
    
    /**
     * Retrieves templates triggered by a specific operation type.
     * 
     * @param triggerType the operation type that triggers the template
     * @return a list of templates for the specified trigger type
     */
    List<TemplateManager> findByTriggerType(OperationType triggerType);
    
    /**
     * Retrieves templates for a specific language/locale.
     * 
     * @param language the language/locale to filter by
     * @return a list of templates for the specified language
     */
    List<TemplateManager> findByLanguage(LocaleType language);
    
    /**
     * Retrieves enabled or disabled templates.
     * 
     * @param isEnabled the enabled status to filter by
     * @return a list of templates with the specified enabled status
     */
    List<TemplateManager> findByIsEnabled(Boolean isEnabled);
    
    /**
     * Retrieves templates for a specific tenant and trigger type.
     * 
     * @param tenantId the tenant identifier
     * @param triggerType the operation type that triggers the template
     * @return a list of templates matching both criteria
     */
    List<TemplateManager> findByTenantIdAndTriggerType(String tenantId, OperationType triggerType);
    
    /**
     * Retrieves templates for a specific tenant, trigger type, and language.
     * 
     * @param tenantId the tenant identifier
     * @param triggerType the operation type that triggers the template
     * @param language the language/locale
     * @return an optional containing the template if found
     */
    Optional<TemplateManager> findByTenantIdAndTriggerTypeAndLanguage(String tenantId, OperationType triggerType, LocaleType language);
    
    /**
     * Retrieves enabled templates for a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @param isEnabled the enabled status
     * @return a list of enabled templates for the specified tenant
     */
    List<TemplateManager> findByTenantIdAndIsEnabled(String tenantId, Boolean isEnabled);
    
    /**
     * Retrieves enabled templates for a specific trigger type.
     * 
     * @param triggerType the operation type that triggers the template
     * @param isEnabled the enabled status
     * @return a list of enabled templates for the specified trigger type
     */
    List<TemplateManager> findByTriggerTypeAndIsEnabled(OperationType triggerType, Boolean isEnabled);
    
    /**
     * Retrieves enabled templates for a specific tenant and trigger type.
     * This method provides tenant-aware template lookup for multi-tenant notification processing.
     * 
     * @param tenantId the tenant identifier
     * @param triggerType the operation type that triggers the template
     * @param isEnabled the enabled status
     * @return a list of enabled templates for the specified tenant and trigger type
     */
    List<TemplateManager> findByTenantIdAndTriggerTypeAndIsEnabled(String tenantId, OperationType triggerType, Boolean isEnabled);
    
    /**
     * Searches for templates by name containing specific text.
     * 
     * @param searchText the text to search for in template names
     * @return a list of templates with matching names
     */
    @Query("SELECT tm FROM TemplateManager tm WHERE tm.templateName LIKE %:searchText%")
    List<TemplateManager> findByTemplateNameContaining(@Param("searchText") String searchText);
    
    /**
     * Counts templates by tenant and enabled status.
     * 
     * @param tenantId the tenant identifier
     * @param isEnabled the enabled status
     * @return the count of templates matching the criteria
     */
    Long countByTenantIdAndIsEnabled(String tenantId, Boolean isEnabled);
    
    /**
     * Checks if a template exists for a specific tenant, trigger type, and language.
     * 
     * @param tenantId the tenant identifier
     * @param triggerType the operation type that triggers the template
     * @param language the language/locale
     * @return true if template exists, false otherwise
     */
    boolean existsByTenantIdAndTriggerTypeAndLanguage(String tenantId, OperationType triggerType, LocaleType language);

    /**
     * Retrieves a template by tenant ID, template name, trigger type, and language.
     * 
     * @param tenantId the tenant identifier
     * @param templateName the template name
     * @param triggerType the operation type that triggers the template
     * @param language the language/locale
     * @return an optional containing the template if found
     */
    Optional<TemplateManager> findByTenantIdAndTemplateNameAndTriggerTypeAndLanguage(
            String tenantId, String templateName, OperationType triggerType, LocaleType language);
}
