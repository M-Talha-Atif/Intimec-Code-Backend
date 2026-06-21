package com.aidentx.core.onboarding.controller;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aidentx.core.onboarding.mapper.ApplicationMapper;
import com.aidentx.core.onboarding.model.dto.ApplicationResponseDTO;
import com.aidentx.core.onboarding.model.dto.ApplicationSchemaDefinitionDTO;
import com.aidentx.core.onboarding.model.dto.ApplicationSchemaObjectTypeDTO;
import com.aidentx.core.onboarding.model.entity.Application;
import com.aidentx.core.onboarding.model.enums.ManagedAccountStatus;
import com.aidentx.core.onboarding.repository.ManagedAccountRepository;
import com.aidentx.core.onboarding.service.ApplicationsOnboardingService;
import com.aidentx.core.onboarding.service.FileStorageService;
import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.CorrelationRuleDTO;
import com.aidentx.core.onboarding.config.constants.ApplicationBulkConstants;
import com.aidentx.core.onboarding.model.dto.ApplicationBulkDtos;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.aidentx.core.onboarding.audit.IgaApplicationAuditEvent;
import com.aidentx.core.onboarding.audit.IgaApplicationAuditPublisher;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationsOnboardingController {

    private final ApplicationsOnboardingService applicationService;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private final IUnifiedLogger logger;
    private final ApplicationMapper mapper;
    private final ManagedAccountRepository managedAccountRepository;
    private final IgaApplicationAuditPublisher auditPublisher;

    public ApplicationsOnboardingController(ApplicationsOnboardingService applicationService,
                                            ObjectMapper objectMapper, FileStorageService fileStorageService,
                                            UnifiedLoggerFactory loggerFactory, ApplicationMapper mapper,
                                            ManagedAccountRepository managedAccountRepository,
                                            IgaApplicationAuditPublisher auditPublisher) {
        this.applicationService = applicationService;
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
        this.logger = loggerFactory.getLogger(ApplicationsOnboardingController.class);
        this.mapper = mapper;
        this.managedAccountRepository = managedAccountRepository;
        this.auditPublisher = auditPublisher;

        // Configure ObjectMapper to be lenient with missing fields
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false);
    }

    /**
     * Bulk fetch applications (id + name only).
     *
     * <p>This endpoint is designed for high-performance internal service calls
     * (e.g., entitlement service) to resolve resource IDs into application names.
     *
     * <p>Example:
     * <pre>
     * POST /api/v1/applications/bulk
     * {
     *   "ids": ["uuid1", "uuid2"]
     * }
     * </pre>
     *
     * @param tenantId tenant identifier
     * @param request  bulk request containing application IDs
     * @return list of application id-name pairs
     */
    @PostMapping(ApplicationBulkConstants.BULK_FETCH_PATH)
    public ResponseEntity<ApplicationBulkDtos.BulkResponse> getApplicationsBulk(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody ApplicationBulkDtos.BulkRequest request) {

        List<UUID> ids = request.ids();

        logger.debug().message("Received bulk application fetch request")
                .detail(ApplicationBulkConstants.LOG_RESOURCE_IDS, ids)
                .log();

        List<ApplicationBulkDtos.ApplicationNameDto> applications =
                applicationService.getApplicationsByIds(tenantId, ids);

        return ResponseEntity.ok(new ApplicationBulkDtos.BulkResponse(applications));
    }


    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> createApplication(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @RequestPart("details") String detailsJson,
            @RequestPart("connector") String connectorJson,
            @RequestPart(value = "schema", required = false) String schemaJson,
            @RequestPart(value = "correlation", required = false) String correlationJson,
            @RequestPart(value = "attributeMappings", required = false) String attributeMappingsJson,
            @RequestPart(value = "access", required = false) String accessJson,
            @RequestPart(value = "synchronizationPolicy", required = false) String synchronizationPolicyJson,
            @RequestPart(value = "icon", required = false) MultipartFile icon) {
        try {
            OnboardingRequestDTO.DetailsDTO details = objectMapper.readValue(detailsJson,
                    OnboardingRequestDTO.DetailsDTO.class);
            Map<String, Object> connector = objectMapper.readValue(connectorJson, new TypeReference<>() {
            });

            OnboardingRequestDTO.SchemaDTO schema;
            if (schemaJson != null && !schemaJson.isEmpty()) {
                schema = objectMapper.readValue(schemaJson, OnboardingRequestDTO.SchemaDTO.class);
            } else {
                schema = new OnboardingRequestDTO.SchemaDTO(List.of());
            }

            // Parse correlation policy
            OnboardingRequestDTO.CorrelationDTO correlation = (correlationJson != null && !correlationJson.isEmpty())
                    ? objectMapper.readValue(correlationJson, OnboardingRequestDTO.CorrelationDTO.class)
                    : null;

            // Parse attribute mappings policy
            OnboardingRequestDTO.AttributeMappingPolicyDTO attributeMappings = (attributeMappingsJson != null
                    && !attributeMappingsJson.isEmpty())
                    ? objectMapper.readValue(attributeMappingsJson,
                    OnboardingRequestDTO.AttributeMappingPolicyDTO.class)
                    : null;

            OnboardingRequestDTO.AccessDTO access = (accessJson != null && !accessJson.isEmpty())
                    ? objectMapper.readValue(accessJson, OnboardingRequestDTO.AccessDTO.class)
                    : null;

            // Parse synchronization policy
            OnboardingRequestDTO.SynchronizationPolicyDTO synchronizationPolicy = (synchronizationPolicyJson != null
                    && !synchronizationPolicyJson.isEmpty())
                    ? objectMapper.readValue(synchronizationPolicyJson,
                    OnboardingRequestDTO.SynchronizationPolicyDTO.class)
                    : null;

            OnboardingRequestDTO requestDto = new OnboardingRequestDTO(details, connector, schema, correlation,
                    attributeMappings, access, synchronizationPolicy, null);

            Application savedApp = applicationService.orchestrateOnboarding(tenantId, requestDto, icon);

            auditPublisher.publish(new IgaApplicationAuditEvent(
                    userId != null ? userId.toString() : "00000000-0000-0000-0000-000000000000",
                    resolveClientIp(),
                    "application_created",
                    savedApp.getId().toString(),
                    savedApp.getName(),
                    savedApp.getRiskLevel() != null ? savedApp.getRiskLevel() : "medium",
                    tenantId,
                    null,
                    buildApplicationState(savedApp),
                    Instant.now()));

            logger.info().message("Successfully created application, returning 201 CREATED.")
                    .detail("appId", savedApp.getId()).log();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(mapper.toResponse(savedApp, correlation, attributeMappings));

        } catch (IllegalArgumentException e) {
            // This will catch validation errors from both the controller and the service
            // layer.
            logger.warn().message("Validation failed during application onboarding.")
                    .detail("error", e.getMessage()).log();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            // This will catch unexpected errors, including gRPC communication failures.
            logger.error(e).message("A critical error occurred during application onboarding.").log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/{resourceId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> updateApplication(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @PathVariable UUID resourceId,
            @RequestPart("details") String detailsJson,
            @RequestPart("connector") String connectorJson,
            @RequestPart(value = "schema", required = false) String schemaJson,
            @RequestPart(value = "correlation", required = false) String correlationJson,
            @RequestPart(value = "attributeMappings", required = false) String attributeMappingsJson,
            @RequestPart(value = "access", required = false) String accessJson,
            @RequestPart(value = "synchronizationPolicy", required = false) String synchronizationPolicyJson,
            @RequestPart(value = "icon", required = false) MultipartFile icon) {
        try {
            // Parse JSON parts with null checks
            OnboardingRequestDTO.DetailsDTO details = null;
            if (detailsJson != null && !detailsJson.trim().isEmpty()) {
                details = objectMapper.readValue(detailsJson, OnboardingRequestDTO.DetailsDTO.class);
            }

            Map<String, Object> connector = null;
            if (connectorJson != null && !connectorJson.trim().isEmpty()) {
                connector = objectMapper.readValue(connectorJson, new TypeReference<Map<String, Object>>() {
                });
            }

            OnboardingRequestDTO.SchemaDTO schema = null;
            if (schemaJson != null && !schemaJson.trim().isEmpty()) {
                schema = objectMapper.readValue(schemaJson, OnboardingRequestDTO.SchemaDTO.class);
            } else {
                schema = new OnboardingRequestDTO.SchemaDTO(List.of());
            }

            // Parse correlation policy
            OnboardingRequestDTO.CorrelationDTO correlation = null;
            if (correlationJson != null && !correlationJson.trim().isEmpty()) {
                correlation = objectMapper.readValue(correlationJson, OnboardingRequestDTO.CorrelationDTO.class);
            }

            // Parse attribute mappings policy
            OnboardingRequestDTO.AttributeMappingPolicyDTO attributeMappings = null;
            if (attributeMappingsJson != null && !attributeMappingsJson.trim().isEmpty()) {
                attributeMappings = objectMapper.readValue(attributeMappingsJson,
                        OnboardingRequestDTO.AttributeMappingPolicyDTO.class);
            }

            OnboardingRequestDTO.AccessDTO access = accessJson != null && !accessJson.trim().isEmpty()
                    ? objectMapper.readValue(accessJson, OnboardingRequestDTO.AccessDTO.class)
                    : null;

            // Parse synchronization policy
            OnboardingRequestDTO.SynchronizationPolicyDTO synchronizationPolicy = null;
            if (synchronizationPolicyJson != null && !synchronizationPolicyJson.trim().isEmpty()) {
                synchronizationPolicy = objectMapper.readValue(synchronizationPolicyJson,
                        OnboardingRequestDTO.SynchronizationPolicyDTO.class);
            }

            // Validate required fields
            if (details == null) {
                throw new IllegalArgumentException("Details are required");
            }
            if (connector == null) {
                throw new IllegalArgumentException("Connector configuration is required");
            }

            // Snapshot state BEFORE update — must build JSON immediately, before orchestrateUpdate
            // mutates the JPA entity via the session cache.
            com.fasterxml.jackson.databind.JsonNode stateBeforeJson = null;
            try {
                Application stateBefore = applicationService.getApplicationEntity(tenantId, resourceId);
                stateBeforeJson = buildApplicationState(stateBefore);
            } catch (Exception e) {
                logger.warn().message("Could not capture stateBefore for audit log").detail("resourceId", resourceId).log();
            }

            OnboardingRequestDTO requestDto = new OnboardingRequestDTO(details, connector, schema, correlation,
                    attributeMappings, access, synchronizationPolicy, null);
            Application updatedApp = applicationService.orchestrateUpdate(tenantId, resourceId, requestDto, icon);

            auditPublisher.publish(new IgaApplicationAuditEvent(
                    userId != null ? userId.toString() : "00000000-0000-0000-0000-000000000000",
                    resolveClientIp(),
                    "application_updated",
                    updatedApp.getId().toString(),
                    updatedApp.getName(),
                    updatedApp.getRiskLevel() != null ? updatedApp.getRiskLevel() : "medium",
                    tenantId,
                    stateBeforeJson,
                    buildApplicationState(updatedApp),
                    Instant.now()));

            return ResponseEntity.ok(mapper.toResponse(updatedApp, correlation, attributeMappings));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error(e).message("Error during application update").log();
            return ResponseEntity.badRequest().body(Map.of("error", "An unexpected error occurred: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<Void> deleteApplication(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false) UUID userId,
            @PathVariable UUID resourceId) {
        Application before = null;
        try {
            before = applicationService.getApplicationEntity(tenantId, resourceId);
        } catch (Exception ignored) {
        }
        try {
            applicationService.deleteApplication(tenantId, resourceId);

            auditPublisher.publish(new IgaApplicationAuditEvent(
                    userId != null ? userId.toString() : "00000000-0000-0000-0000-000000000000",
                    resolveClientIp(),
                    "application_deleted",
                    resourceId.toString(),
                    before != null ? before.getName() : resourceId.toString(),
                    before != null && before.getRiskLevel() != null ? before.getRiskLevel() : "high",
                    tenantId,
                    before != null ? buildApplicationState(before) : null,
                    null,
                    Instant.now()));

            logger.info().message("Successfully deleted application")
                    .detail("resourceId", resourceId).log();
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            logger.warn().message("Application not found for deletion")
                    .detail("resourceId", resourceId)
                    .detail("tenantId", tenantId).log();
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error(e).message("Error during application deletion")
                    .detail("resourceId", resourceId).log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<ApplicationResponseDTO> getApplicationById(@RequestHeader("X-Tenant-ID") String tenantId,
                                                                     @PathVariable UUID resourceId) {
        try {
            Application app = applicationService.getApplicationEntity(tenantId, resourceId);

            // Fetch policies from identity-service
            com.aidentx.iam.contracts.grpc.IdentityMappingPolicy policy = applicationService.getPolicy(tenantId,
                    resourceId, app.getIdentityType());

            OnboardingRequestDTO.CorrelationDTO correlation = null;
            OnboardingRequestDTO.AttributeMappingPolicyDTO attributeMappings = null;
            OnboardingRequestDTO.SynchronizationPolicyDTO synchronizationPolicy = null;

            if (policy != null) {
                correlation = convertCorrelationFromProto(policy, resourceId);
                attributeMappings = convertAttributeMappingsFromProto(policy, resourceId);
                synchronizationPolicy = convertSynchronizationPolicyFromProto(policy);
            }

            return ResponseEntity.ok(mapper.toResponse(app, correlation, attributeMappings, synchronizationPolicy));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error(e).message("Error fetching application").detail("resourceId", resourceId).log();
            return ResponseEntity.badRequest().build();
        }
    }

    // -------------------------------------------------------------------------
    // Audit helpers
    // -------------------------------------------------------------------------

    private com.fasterxml.jackson.databind.JsonNode buildApplicationState(Application app) {
        ObjectNode node = objectMapper.createObjectNode();
        if (app.getId() != null) node.put("id", app.getId().toString());
        if (app.getName() != null) node.put("name", app.getName());
        if (app.getDescription() != null) node.put("description", app.getDescription());
        if (app.getRiskLevel() != null) node.put("riskLevel", app.getRiskLevel());
        if (app.getLifecycleStatus() != null) node.put("lifecycleStatus", app.getLifecycleStatus().name());
        return node;
    }

    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                String forwarded = req.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isEmpty()) {
                    return forwarded.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    private OnboardingRequestDTO.CorrelationDTO convertCorrelationFromProto(
            com.aidentx.iam.contracts.grpc.IdentityMappingPolicy policy, UUID resourceId) {
        return policy.getCorrelationRulesList().stream()
                .filter(rule -> rule.getResourceId().equals(resourceId.toString()))
                .findFirst()
                .map(rule -> {
                    try {
                        String mode = rule.getMode();
                        OnboardingRequestDTO.RuleGroupDTO ruleGroup = null;
                        OnboardingRequestDTO.ScriptDTO script = null;

                        String json = com.google.protobuf.util.JsonFormat.printer().print(rule.getRuleGroup());

                        if ("script".equalsIgnoreCase(mode)) {
                            // Extract script data from the struct
                            Map<String, Object> map = objectMapper.readValue(json,
                                    new TypeReference<Map<String, Object>>() {
                                    });
                            String data = (String) map.get("script");
                            String lang = (String) map.get("lang");
                            script = new OnboardingRequestDTO.ScriptDTO(data, lang);
                        } else {
                            ruleGroup = objectMapper.readValue(json, OnboardingRequestDTO.RuleGroupDTO.class);
                        }

                        return new OnboardingRequestDTO.CorrelationDTO(mode, ruleGroup, script);
                    } catch (Exception e) {
                        logger.warn().message("Failed to convert correlation rule").detail("error", e.getMessage())
                                .log();
                        return null;
                    }
                })
                .orElse(null);
    }

    private OnboardingRequestDTO.AttributeMappingPolicyDTO convertAttributeMappingsFromProto(
            com.aidentx.iam.contracts.grpc.IdentityMappingPolicy policy, UUID resourceId) {
        List<OnboardingRequestDTO.OnboardingAttributeMappingDTO> mappings = policy.getAttributeMappingsList().stream()
                .filter(mapping -> mapping.getSourceResourceId().equals(resourceId.toString()))
                .map(mapping -> {
                    try {
                        OnboardingRequestDTO.ScriptDTO script = null;
                        if (mapping.hasScript()) {
                            String json = com.google.protobuf.util.JsonFormat.printer().print(mapping.getScript());
                            Map<String, Object> map = objectMapper.readValue(json,
                                    new TypeReference<Map<String, Object>>() {
                                    });
                            String data = (String) map.get("data");
                            String lang = (String) map.get("lang");
                            script = new OnboardingRequestDTO.ScriptDTO(data, lang);
                        }
                        return new OnboardingRequestDTO.OnboardingAttributeMappingDTO(
                                mapping.getSourceAttributeName(),
                                mapping.getTargetAttributeName(),
                                mapping.getMode(),
                                script);
                    } catch (Exception e) {
                        logger.warn().message("Failed to convert attribute mapping").detail("error", e.getMessage())
                                .log();
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        if (mappings.isEmpty()) {
            return null;
        }
        return new OnboardingRequestDTO.AttributeMappingPolicyDTO(mappings);
    }

    private OnboardingRequestDTO.SynchronizationPolicyDTO convertSynchronizationPolicyFromProto(
            com.aidentx.iam.contracts.grpc.IdentityMappingPolicy policy) {
        List<OnboardingRequestDTO.SynchronizationReactionDTO> reactions = policy.getSynchronizationReactionsList()
                .stream()
                .map(reaction -> new OnboardingRequestDTO.SynchronizationReactionDTO(
                        reaction.getSituation(),
                        reaction.getAction()))
                .toList();

        if (reactions.isEmpty()) {
            return null;
        }
        return new OnboardingRequestDTO.SynchronizationPolicyDTO(reactions);
    }

    @GetMapping
    public ResponseEntity<Page<ApplicationResponseDTO>> getAllApplications(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        Page<Application> applications = applicationService.getAllApplications(tenantId, pageable);
        Page<ApplicationResponseDTO> response = applications.map(app -> {
            ApplicationResponseDTO base = mapper.toResponse(app);
            long totalAccounts = managedAccountRepository.countByTenantIdAndResourceId(tenantId, app.getId());
            long orphanCount = managedAccountRepository.countByTenantIdAndResourceIdAndStatus(
                    tenantId, app.getId(), ManagedAccountStatus.ORPHANED);
            return new ApplicationResponseDTO(
                    base.id(), base.details(), base.connector(), base.schema(),
                    base.correlation(), base.attributeMappings(), base.access(),
                    base.synchronizationPolicy(), base.createdAt(), base.updatedAt(),
                    totalAccounts, orphanCount);
        });
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{resourceId}/schema")
    public ResponseEntity<List<ApplicationSchemaObjectTypeDTO>> getApplicationSchemaObjectTypes(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID resourceId) {
        try {
            List<ApplicationSchemaObjectTypeDTO> objectTypes = applicationService
                    .getApplicationSchemaObjectTypes(resourceId);
            return ResponseEntity.ok(objectTypes);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{resourceId}/schema/{objectType}")
    public ResponseEntity<List<ApplicationSchemaDefinitionDTO>> getApplicationSchemaDefinitions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID resourceId,
            @PathVariable String objectType) {
        try {
            List<ApplicationSchemaDefinitionDTO> definitions = applicationService
                    .getApplicationSchemaDefinitions(resourceId, objectType);
            return ResponseEntity.ok(definitions);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Serves an application icon file.
     * This endpoint retrieves the icon from the secure storage location and streams
     * it to the client.
     *
     * @param filename The name of the icon file (e.g., "uuid.png").
     * @return A ResponseEntity containing the file resource.
     */
    @GetMapping("/icons/{filename:.+}")
    public ResponseEntity<Resource> serveIcon(@PathVariable String filename) {
        try {
            Resource file = fileStorageService.loadIconAsResource(filename);
            MediaType mediaType = MediaTypeFactory.getMediaType(file).orElse(MediaType.APPLICATION_OCTET_STREAM);
            return ResponseEntity.ok().contentType(mediaType).body(file);
        } catch (FileNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


    /**
     * Response DTO for application onboarding that includes governance data with
     * IDs
     */
    public static class OnboardingResponseDTO {
        private final Application application;
        private final List<CorrelationRuleDTO> correlationRules;
        private final List<AttributeMappingDTO> attributeMappings;

        public OnboardingResponseDTO(Application application, List<CorrelationRuleDTO> correlationRules,
                                     List<AttributeMappingDTO> attributeMappings) {
            this.application = application;
            this.correlationRules = correlationRules;
            this.attributeMappings = attributeMappings;
        }

        public Application getApplication() {
            return application;
        }

        public List<CorrelationRuleDTO> getCorrelationRules() {
            return correlationRules;
        }

        public List<AttributeMappingDTO> getAttributeMappings() {
            return attributeMappings;
        }
    }
}