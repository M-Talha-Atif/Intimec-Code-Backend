package com.aidentx.iam.events;

import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing identity lifecycle events to Kafka.
 *
 * <p>This service handles publishing events when identities are created, updated, or deleted.
 * These events are consumed by the Auth service to maintain synchronized user accounts.</p>
 *
 * <p>Events are published to the "identity-lifecycle-events" topic with the identity ID as the key
 * to ensure ordering per identity.</p>
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class IdentityEventPublisher {

    private static final String IDENTITY_EVENTS_TOPIC = "identity-lifecycle-events";
    private static final String EVENT_TYPE_CREATED = "identity.created";
    private static final String EVENT_TYPE_UPDATED = "identity.updated";
    private static final String EVENT_TYPE_DELETED = "identity.deleted";

    private final KafkaTemplate<String, IdentityLifecycleEvent> kafkaTemplate;
    private final IUnifiedLogger logger;

    @Autowired
    public IdentityEventPublisher(
            @Qualifier("lifecycleEventKafkaTemplate") KafkaTemplate<String, IdentityLifecycleEvent> kafkaTemplate,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = loggerFactory.getLogger("IdentityEventPublisher", "IDENTITY_EVENTS");
    }

    /**
     * Publishes an identity.created event when a new identity is created.
     *
     * @param identity the created identity (Employee, NonEmployee, or NonHumanIdentity)
     * @param createdBy username of the creator
     */
    public void publishIdentityCreatedEvent(Identity identity, String createdBy) {
        publishIdentityCreatedEvent(identity, createdBy, "identity-creation-api");
    }

    /**
     * Publishes an identity.created event with an explicit source identifier.
     * Used by non-API creation paths (e.g., application reconciliation) to maintain
     * traceability while still triggering user provisioning in the auth-service.
     *
     * @param identity  the created identity
     * @param createdBy username or system identifier that triggered the creation
     * @param source    originating subsystem (e.g., "application-reconciliation")
     */
    public void publishIdentityCreatedEvent(Identity identity, String createdBy, String source) {
        try {
            IdentityLifecycleEvent event = buildLifecycleEvent(identity, EVENT_TYPE_CREATED);

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("createdBy", createdBy);
            metadata.put("source", source);
            metadata.put("version", "1.0");
            event.setMetadata(metadata);

            // Send event (key is identityId to ensure ordering per identity)
            kafkaTemplate.send(IDENTITY_EVENTS_TOPIC, identity.getId().toString(), event);

            logger.info()
                    .message("Published identity.created event to Kafka")
                    .detail("identityId", identity.getId().toString())
                    .detail("identityType", identity.getIdentityType().name())
                    .detail("tenantId", identity.getTenantId())
                    .detail("topic", IDENTITY_EVENTS_TOPIC)
                    .detail("eventId", event.getEventId().toString())
                    .log();

        } catch (Exception e) {
            // Log error but don't fail identity creation
            logger.error(e)
                    .message("Failed to publish identity.created event")
                    .detail("identityId", identity.getId().toString())
                    .detail("identityType", identity.getIdentityType().name())
                    .detail("tenantId", identity.getTenantId())
                    .detail("error", e.getMessage())
                    .log();
        }
    }

    /**
     * Publishes an identity.updated event when an identity is modified.
     *
     * @param identity the updated identity
     * @param updatedBy username of who updated the identity
     */
    public void publishIdentityUpdatedEvent(Identity identity, String updatedBy) {
        try {
            IdentityLifecycleEvent event = buildLifecycleEvent(identity, EVENT_TYPE_UPDATED);

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("updatedBy", updatedBy);
            metadata.put("source", "identity-update-api");
            metadata.put("version", "1.0");
            event.setMetadata(metadata);

            kafkaTemplate.send(IDENTITY_EVENTS_TOPIC, identity.getId().toString(), event);

            logger.info()
                    .message("Published identity.updated event to Kafka")
                    .detail("identityId", identity.getId().toString())
                    .detail("identityType", identity.getIdentityType().name())
                    .detail("tenantId", identity.getTenantId())
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to publish identity.updated event")
                    .detail("identityId", identity.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
        }
    }

    /**
     * Publishes an identity.deleted event when an identity is removed.
     *
     * @param identity the deleted identity
     * @param deletedBy username of who deleted the identity
     */
    public void publishIdentityDeletedEvent(Identity identity, String deletedBy) {
        try {
            IdentityLifecycleEvent event = buildLifecycleEvent(identity, EVENT_TYPE_DELETED);

            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("deletedBy", deletedBy);
            metadata.put("source", "identity-deletion-api");
            metadata.put("version", "1.0");
            event.setMetadata(metadata);

            kafkaTemplate.send(IDENTITY_EVENTS_TOPIC, identity.getId().toString(), event);

            logger.info()
                    .message("Published identity.deleted event to Kafka")
                    .detail("identityId", identity.getId().toString())
                    .detail("identityType", identity.getIdentityType().name())
                    .detail("tenantId", identity.getTenantId())
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to publish identity.deleted event")
                    .detail("identityId", identity.getId().toString())
                    .detail("error", e.getMessage())
                    .log();
        }
    }

    /**
     * Builds an IdentityLifecycleEvent from an Identity entity.
     * Extracts common fields and type-specific data.
     *
     * @param identity the identity entity
     * @param eventType the event type (created/updated/deleted)
     * @return populated IdentityLifecycleEvent
     */
    private IdentityLifecycleEvent buildLifecycleEvent(Identity identity, String eventType) {
        IdentityLifecycleEvent event = new IdentityLifecycleEvent();
        event.setEventType(eventType);
        event.setTenantId(identity.getTenantId());
        event.setIdentityId(identity.getId());
        event.setIdentityType(identity.getIdentityType().name());
        event.setStatus(identity.getStatus().name());

        // Extract human-specific fields if applicable
        if (identity instanceof HumanIdentity) {
            HumanIdentity humanIdentity = (HumanIdentity) identity;
            event.setUsername(humanIdentity.getUsername());
            event.setEmail(humanIdentity.getEmail());
            event.setFirstName(humanIdentity.getFirstName());
            event.setLastName(humanIdentity.getLastName());
            event.setDisplayName(humanIdentity.getDisplayName());
        }

        // Add type-specific additional data
        Map<String, Object> additionalData = new HashMap<>();

        if (identity instanceof Employee) {
            Employee employee = (Employee) identity;
            additionalData.put("employeeId", employee.getEmployeeId());
            additionalData.put("jobTitle", employee.getJobTitle());
            additionalData.put("department", employee.getDepartment());
            additionalData.put("riskLevel", employee.getRiskLevel());
            additionalData.put("hireDate", employee.getHireDate());
            if (employee.getManager() != null) {
                additionalData.put("managerId", employee.getManager().getId().toString());
            }
        } else if (identity instanceof NonEmployee) {
            NonEmployee nonEmployee = (NonEmployee) identity;
            additionalData.put("contractId", nonEmployee.getContractId());
            additionalData.put("vendorName", nonEmployee.getVendorName());
            additionalData.put("riskLevel", nonEmployee.getRiskLevel());
            additionalData.put("contractEndDate", nonEmployee.getContractEndDate());
            if (nonEmployee.getSponsor() != null) {
                additionalData.put("sponsorId", nonEmployee.getSponsor().getId().toString());
            }
        } else if (identity instanceof NonHumanIdentity) {
            NonHumanIdentity nonHuman = (NonHumanIdentity) identity;
            additionalData.put("serviceName", nonHuman.getServiceName());
            additionalData.put("description", nonHuman.getDescription());
            additionalData.put("ownerTeam", nonHuman.getOwnerTeam());
            additionalData.put("riskLevel", nonHuman.getRiskLevel());
            // For non-human identities, use serviceName as username
            event.setUsername(nonHuman.getServiceName());
        }

        event.setAdditionalData(additionalData);
        return event;
    }
}
