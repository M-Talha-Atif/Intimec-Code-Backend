package com.aidentx.core.onboarding.service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.aidentx.core.onboarding.model.dto.ApplicationBulkDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.aidentx.core.onboarding.model.dto.ApplicationSchemaDefinitionDTO;
import com.aidentx.core.onboarding.model.dto.ApplicationSchemaObjectTypeDTO;
import com.aidentx.core.onboarding.model.entity.Application;
import com.aidentx.core.onboarding.model.entity.ApplicationSchemaAttribute;
import com.aidentx.core.onboarding.repository.ApplicationRepository;
import com.aidentx.dal.protos.identity_schema.IdentitySchemaServiceGrpc;
import com.aidentx.iam.contracts.dto.ApplicationCardDTO;
import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import com.aidentx.iam.events.ApplicationRegisteredEvent;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.JsonFormat;

import com.aidentx.core.onboarding.client.IdentityPurgeClient;
import com.aidentx.core.onboarding.client.TaskManagementServiceClient;
import com.aidentx.core.onboarding.repository.ManagedAccountRepository;
import jakarta.persistence.EntityNotFoundException;

@Service
public class ApplicationsOnboardingService {

    private final IUnifiedLogger logger;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final FileStorageService fileStorageService;
    private final PolicyOrchestrationService policyOrchestrationService;
    private final TaskManagementServiceClient taskManagementServiceClient;
    private final IdentityPurgeClient identityServiceClient;
    private final ManagedAccountRepository managedAccountRepository;

    public ApplicationsOnboardingService(
            UnifiedLoggerFactory loggerFactory,
            ApplicationRepository applicationRepository,
            ObjectMapper objectMapper,
            JsonFormat.Parser jsonParser,
            @Qualifier("applicationLifecycleEventsKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            FileStorageService fileStorageService,
            IdentitySchemaServiceGrpc.IdentitySchemaServiceBlockingStub identitySchemaClient,
            PolicyOrchestrationService policyOrchestrationService,
            TaskManagementServiceClient taskManagementServiceClient,
            IdentityPurgeClient identityServiceClient,
            ManagedAccountRepository managedAccountRepository) {
        this.logger = loggerFactory.getLogger(ApplicationsOnboardingService.class);
        this.applicationRepository = applicationRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.fileStorageService = fileStorageService;
        this.policyOrchestrationService = policyOrchestrationService;
        this.taskManagementServiceClient = taskManagementServiceClient;
        this.identityServiceClient = identityServiceClient;
        this.managedAccountRepository = managedAccountRepository;
    }

    @Transactional
    public Application orchestrateOnboarding(String tenantId, OnboardingRequestDTO request, MultipartFile icon)
            throws IOException {
        logger.info().message("Starting application onboarding with multipart data.")
                .detail("appName", request.details().name()).log();

        Application app = convertDtoToEntity(tenantId, request);

        // Save the application first to get a stable UUID
        Application savedApp = applicationRepository.save(app);
        logger.info().message("Saved application entity.").detail("resourceId", savedApp.getId()).log();

        // If an icon was provided, store it using the app's UUID and update the entity
        if (icon != null && !icon.isEmpty()) {
            String iconPath = fileStorageService.storeIcon(icon, savedApp.getId());
            savedApp.setIconPath(iconPath);
            savedApp = applicationRepository.save(savedApp); // Save again to persist the icon path
        }

        try {
            // Forward per-identity-type policies to Identity Service
            // Each identity type gets its own policy storage
            policyOrchestrationService.forwardPoliciesToIdentityService(tenantId, savedApp, request);

        } catch (Exception e) {
            logger.error(e).message("Onboarding failed due to policy forwarding error. Rolling back transaction.")
                    .detail("appId", savedApp.getId()).log();
            // Re-throw as a runtime exception to trigger the @Transactional rollback
            throw new RuntimeException("Onboarding failed: " + e.getMessage(), e);
        }

        publishRegistrationEvent(savedApp);
        return savedApp;
    }

    @Transactional
    public Application orchestrateUpdate(String tenantId, UUID resourceId, OnboardingRequestDTO request,
                                         MultipartFile icon) throws IOException {
        logger.info().message("Starting application update with multipart data.")
                .detail("resourceId", resourceId).log();
        Application existingApp = applicationRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Application with ID " + resourceId + " not found."));

        String oldIdentityType = existingApp.getIdentityType();

        // If a new icon is provided, delete the old one first
        if (icon != null && !icon.isEmpty() && existingApp.getIconPath() != null) {
            fileStorageService.deleteIcon(existingApp.getIconPath());
        }

        updateEntityFromDto(existingApp, tenantId, request);

        // If an icon was provided, store it and update the path
        if (icon != null && !icon.isEmpty()) {
            String iconPath = fileStorageService.storeIcon(icon, existingApp.getId());
            existingApp.setIconPath(iconPath);
        }

        Application updatedApp = applicationRepository.save(existingApp);

        // If the identity type has changed, we must delete the old policies to avoid
        // duplication
        if (oldIdentityType != null && !oldIdentityType.equals(updatedApp.getIdentityType())) {
            logger.info().message("Identity type changed. Deleting old policies.")
                    .detail("oldType", oldIdentityType)
                    .detail("newType", updatedApp.getIdentityType())
                    .log();
            policyOrchestrationService.deletePolicies(tenantId, resourceId);
        }

        try {
            // Forward updated per-identity-type policies to Identity Service
            policyOrchestrationService.forwardPoliciesToIdentityService(tenantId, updatedApp, request);
        } catch (Exception e) {
            logger.error(e).message("Update failed due to policy forwarding error. Rolling back transaction.")
                    .detail("appId", updatedApp.getId()).log();
            throw new RuntimeException("Update failed: " + e.getMessage(), e);
        }

        if (!updatedApp.isAuthoritative()) {
            publishRegistrationEvent(updatedApp);
        }

        logger.info().message("Successfully updated application.").detail("resourceId", updatedApp.getId())
                .log();
        return updatedApp;
    }

    private Application convertDtoToEntity(String tenantId, OnboardingRequestDTO request) {
        Application app = new Application();
        updateEntityFromDto(app, tenantId, request);
        return app;
    }

    private void updateEntityFromDto(Application app, String tenantId, OnboardingRequestDTO request) {
        OnboardingRequestDTO.DetailsDTO details = request.details();

        // Map simple fields from 'details'
        app.setTenantId(tenantId);
        app.setName(details.name());
        app.setDescription(details.description());
        app.setAuthoritative(details.isAuthoritative());
        app.setIdentityType(details.identityType());
        app.setRiskLevel(details.riskLevel());

        // Set schemaId if provided
        if (details.schemaId() != null && !details.schemaId().isBlank()) {
            try {
                app.setAssociatedSchemaId(UUID.fromString(details.schemaId()));
            } catch (IllegalArgumentException e) {
                logger.warn().message("Invalid schemaId format, ignoring")
                        .detail("schemaId", details.schemaId())
                        .log();
            }
        }

        // Map complex objects and arrays to JsonNode fields
        app.setTechnicalOwners(objectMapper.valueToTree(details.owner()));
        app.setBusinessOwners(objectMapper.valueToTree(details.businessOwner()));
        app.setCategories(objectMapper.valueToTree(details.category()));
        app.setComplianceLabels(objectMapper.valueToTree(details.complianceLabels()));
        app.setScopes(objectMapper.valueToTree(details.scopes()));
        app.setDepartment(objectMapper.valueToTree(details.department()));

        // Handle connector configuration - add connectorId from details if provided
        Map<String, Object> connectorConfig = request.connector();
        if (connectorConfig == null) {
            connectorConfig = new java.util.HashMap<>();
        }
        if (details.connectorId() != null && !details.connectorId().isBlank()) {
            connectorConfig.put("connectorId", details.connectorId());
        }
        app.setConnectorConfiguration(objectMapper.valueToTree(connectorConfig));

        // Map top-level JSON objects
        app.setAccessPolicy(objectMapper.valueToTree(request.access()));

        // Reconcile the schema attributes collection
        app.getSchemaAttributes().clear();
        if (request.schema() != null && request.schema().definitions() != null) {
            List<ApplicationSchemaAttribute> schemaAttributes = new ArrayList<>();
            for (OnboardingRequestDTO.SchemaDefinitionDTO def : request.schema().definitions()) {
                if (def.attributes() != null) {
                    for (OnboardingRequestDTO.SchemaAttributeDTO attrDto : def.attributes()) {
                        ApplicationSchemaAttribute attr = new ApplicationSchemaAttribute();
                        attr.setApplication(app);
                        attr.setName(attrDto.name());
                        attr.setType(attrDto.type());
                        attr.setCategory(attrDto.category());
                        attr.setDescription(attrDto.description());
                        attr.setObjectType(def.objectClass());
                        attr.setObjectTypeKind(def.type().name());
                        attr.setFlags(objectMapper.valueToTree(attrDto.flags()));

                        if (def.displayAttribute() != null && def.displayAttribute().equals(attrDto.name())) {
                            attr.setDisplayAttribute(true);
                        } else {
                            attr.setDisplayAttribute(false);
                        }

                        if (def.descriptionAttribute() != null && def.descriptionAttribute().equals(attrDto.name())) {
                            attr.setDescriptionAttribute(true);
                        } else {
                            attr.setDescriptionAttribute(false);
                        }

                        schemaAttributes.add(attr);
                    }
                }
            }
            app.getSchemaAttributes().addAll(schemaAttributes);
        }
    }

    private void publishRegistrationEvent(Application app) {
        // Publish event asynchronously without blocking the response
        // This is for future event consumers and should not impact onboarding
        // performance
        CompletableFuture.runAsync(() -> {
            ApplicationRegisteredEvent event = new ApplicationRegisteredEvent(
                    app.getTenantId(),
                    app.getId(),
                    app.getName(),
                    app.getAssociatedSchemaId());
            try {
                kafkaTemplate.send("application-registered-events", app.getId().toString(), event).get();
                logger.info().message("Published ApplicationRegisteredEvent.").detail("resourceId", app.getId())
                        .log();
            } catch (Exception e) {
                // Log the failure but don't throw - Kafka issues shouldn't break onboarding
                // since no active consumers depend on this event currently
                logger.warn().message(
                                "Failed to publish ApplicationRegisteredEvent - will retry in background if needed.")
                        .detail("resourceId", app.getId()).failureReason(e.getLocalizedMessage()).log();
            }
        });
    }

    public Application getById(UUID resourceId) {
        return applicationRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + resourceId));
    }

    @Transactional(readOnly = true)
    public Page<Application> getAllApplications(String tenantId, Pageable pageable) {
        return applicationRepository.findByTenantId(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ApplicationCardDTO> getAllApplicationsForTenant(String tenantId) {
        logger.info().message("Fetching all applications for tenant.").detail("tenantId", tenantId).log();
        return applicationRepository.findByTenantId(tenantId).stream()
                .map(app -> {
                    String iconPath = null;
                    if (app.getIconPath() != null && !app.getIconPath().isBlank()) {
                        // Construct the fully qualified URL for the icon
                        String filename = Paths.get(app.getIconPath()).getFileName().toString();
                        iconPath = "/api/v1/applications/icons/" + filename;
                    } else {
                        // If no application icon, try to get connector icon
                        String connectorId = null;
                        if (app.getConnectorConfiguration() != null
                                && app.getConnectorConfiguration().has("connectorId")) {
                            connectorId = app.getConnectorConfiguration().get("connectorId")
                                    .asText();
                        }
                        if (connectorId != null) {
                            iconPath = "/api/v1/connectors/" + connectorId + "/icon";
                        }
                    }

                    return new ApplicationCardDTO(
                            app.getId(),
                            app.getName(),
                            app.isAuthoritative(),
                            app.getLifecycleStatus().name(),
                            app.getOperationalStatus().name(),
                            iconPath);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationSchemaAttribute> getApplicationSchemaAttributes(UUID resourceId) {
        logger.info().message("Fetching schema attributes for application.").detail("resourceId", resourceId)
                .log();
        Application app = applicationRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + resourceId));
        // Eagerly fetch the attributes to avoid lazy loading issues
        app.getSchemaAttributes().size();
        return app.getSchemaAttributes();
    }

    @Transactional(readOnly = true)
    public List<ApplicationSchemaObjectTypeDTO> getApplicationSchemaObjectTypes(UUID resourceId) {
        Application app = applicationRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + resourceId));

        Map<String, String> objectTypeDisplayMap = new HashMap<>();
        Map<String, String> objectTypeDescriptionMap = new HashMap<>();

        for (ApplicationSchemaAttribute attr : app.getSchemaAttributes()) {
            if (!objectTypeDisplayMap.containsKey(attr.getObjectType())) {
                objectTypeDisplayMap.put(attr.getObjectType(), null);
            }
            if (!objectTypeDescriptionMap.containsKey(attr.getObjectType())) {
                objectTypeDescriptionMap.put(attr.getObjectType(), null);
            }
            if (attr.isDisplayAttribute()) {
                objectTypeDisplayMap.put(attr.getObjectType(), attr.getName());
            }
            if (attr.isDescriptionAttribute()) {
                objectTypeDescriptionMap.put(attr.getObjectType(), attr.getName());
            }
        }

        return objectTypeDisplayMap.entrySet().stream()
                .map(entry -> new ApplicationSchemaObjectTypeDTO(
                        entry.getKey(),
                        entry.getValue(),
                        objectTypeDescriptionMap.get(entry.getKey())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationSchemaDefinitionDTO> getApplicationSchemaDefinitions(UUID resourceId, String objectType) {
        logger.info().message("Fetching schema definitions for application.")
                .detail("resourceId", resourceId)
                .detail("objectType", objectType)
                .log();

        Application app = applicationRepository.findById(resourceId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found: " + resourceId));

        return app.getSchemaAttributes().stream()
                .filter(attr -> attr.getObjectType().equals(objectType))
                .map(ApplicationSchemaDefinitionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Deletes an application and its associated policies from the Identity Service.
     * This is a transactional operation that will rollback if policy deletion
     * fails.
     *
     * @param tenantId   The tenant ID
     * @param resourceId The application UUID to delete
     * @throws EntityNotFoundException if the application doesn't exist
     * @throws RuntimeException        if policy deletion fails
     */
    @Transactional
    public void deleteApplication(String tenantId, UUID resourceId) {
        logger.info().message("Starting application deletion")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId).log();

        // Verify the application exists and belongs to the tenant
        Application application = applicationRepository.findByIdAndTenantId(resourceId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Application not found with ID: " + resourceId + " for tenant: " + tenantId));

        try {
            // Purge all identity-service data first (shadows, entitlements, assignments, rules, shards)
            identityServiceClient.purgeApplicationData(resourceId, tenantId);

            // Cascade-delete all tasks (and their runs) for this application
            taskManagementServiceClient.deleteAllTasksForApplication(resourceId, tenantId);

            // Delete governance policies from Identity Service
            policyOrchestrationService.deletePolicies(tenantId, resourceId);

            // Delete all managed accounts for this application
            managedAccountRepository.deleteAllByResourceIdAndTenantId(resourceId, tenantId);

            // Delete the icon file if it exists
            if (application.getIconPath() != null && !application.getIconPath().isEmpty()) {
                try {
                    fileStorageService.deleteIcon(application.getIconPath());
                    logger.info().message("Deleted application icon")
                            .detail("iconPath", application.getIconPath()).log();
                } catch (Exception e) {
                    logger.warn().message("Failed to delete icon file, continuing with application deletion")
                            .detail("iconPath", application.getIconPath())
                            .detail("error", e.getMessage()).log();
                }
            }

            // Delete the application from the database
            applicationRepository.delete(application);

            logger.info().message("Successfully deleted application")
                    .detail("resourceId", resourceId)
                    .detail("appName", application.getName()).log();

        } catch (Exception e) {
            logger.error(e).message("Application deletion failed. Rolling back transaction.")
                    .detail("resourceId", resourceId).log();
            throw new RuntimeException("Application deletion failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Application getApplicationEntity(String tenantId, UUID resourceId) {
        return applicationRepository.findByIdAndTenantId(resourceId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Application not found"));
    }

    public com.aidentx.iam.contracts.grpc.IdentityMappingPolicy getPolicy(String tenantId, UUID resourceId,
                                                                          String identityType) {
        return policyOrchestrationService.getPolicy(tenantId, resourceId, identityType);
    }

    /**
     * Returns lightweight application details (id + name) for given IDs.
     *
     * <p>This method is optimized for bulk cross-service resolution use cases
     * (e.g., entitlement service enrichment). Only minimal fields are fetched
     * to reduce payload size and DB overhead.
     *
     * @param tenantId tenant identifier
     * @param ids      list of application IDs
     * @return list of application name projections
     */
    @Transactional(readOnly = true)
    public List<ApplicationBulkDtos.ApplicationNameDto> getApplicationsByIds(
            String tenantId,
            List<UUID> ids) {

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        logger.debug().message("Fetching applications in bulk")
                .detail("tenantId", tenantId)
                .detail("count", ids.size())
                .log();

        List<Application> apps = applicationRepository.findByIdInAndTenantId(ids, tenantId);

        List<ApplicationBulkDtos.ApplicationNameDto> result = apps.stream()
                .map(app -> new ApplicationBulkDtos.ApplicationNameDto(
                        app.getId(),
                        app.getName()
                ))
                .toList();

        logger.info().message("Bulk application fetch completed")
                .detail("resultCount", result.size())
                .log();

        return result;
    }
}