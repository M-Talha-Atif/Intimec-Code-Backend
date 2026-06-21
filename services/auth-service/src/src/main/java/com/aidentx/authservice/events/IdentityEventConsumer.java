package com.aidentx.authservice.events;

import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.enums.UserStatus;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Kafka consumer for identity lifecycle events from the Identity service.
 *
 * <p>This service listens to the "identity-events" topic and creates/updates/deletes
 * corresponding user records in the Auth service database. This keeps authentication
 * accounts synchronized with the authoritative identity records.</p>
 *
 * <p>Key behaviors:</p>
 * <ul>
 *   <li>identity.created → Creates new user account (no password set initially)</li>
 *   <li>identity.updated → Updates user account fields</li>
 *   <li>identity.deleted → Soft deletes or marks user as INACTIVE</li>
 * </ul>
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class IdentityEventConsumer {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    @Autowired
    public IdentityEventConsumer(UserRepository userRepository,
                                 ObjectMapper objectMapper,
                                 UnifiedLoggerFactory loggerFactory) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger("IdentityEventConsumer", "AUTH_EVENTS");
    }

    /**
     * Consumes identity lifecycle events from Kafka.
     * Routes to appropriate handler based on event type.
     *
     * @param event the identity lifecycle event
     */
    @KafkaListener(
            topics = "identity-lifecycle-events",
            groupId = "auth-service-identity-sync",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeIdentityEvent(IdentityLifecycleEvent event) {
        try {
            logger.info()
                    .message("Received identity lifecycle event")
                    .detail("eventId", event.getEventId().toString())
                    .detail("eventType", event.getEventType())
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("identityType", event.getIdentityType())
                    .detail("tenantId", event.getTenantId())
                    .log();

            switch (event.getEventType()) {
                case "identity.created":
                    handleIdentityCreated(event);
                    break;
                case "identity.updated":
                    handleIdentityUpdated(event);
                    break;
                case "identity.deleted":
                    handleIdentityDeleted(event);
                    break;
                default:
                    logger.warn()
                            .message("Unknown event type received")
                            .detail("eventType", event.getEventType())
                            .detail("eventId", event.getEventId().toString())
                            .log();
            }

        } catch (Exception e) {
            // Log error and let Kafka handle retry logic
            logger.error(e)
                    .message("Failed to process identity lifecycle event")
                    .detail("eventId", event.getEventId().toString())
                    .detail("eventType", event.getEventType())
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("error", e.getMessage())
                    .log();
            throw new RuntimeException("Failed to process identity event", e);
        }
    }

    /**
     * Handles identity.created events by creating a new user account.
     * User is created without a password; they must use forgot password flow.
     *
     * @param event the identity created event
     */
    private void handleIdentityCreated(IdentityLifecycleEvent event) {
        // Check if user already exists (idempotency)
        Optional<User> existingUser = userRepository.findByIdentityId(event.getIdentityId());
        if (existingUser.isPresent()) {
            logger.warn()
                    .message("User already exists for identity, skipping creation")
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("userId", existingUser.get().getId().toString())
                    .log();
            return;
        }

        // Only create users for human identities (Employee, NonEmployee)
        // Non-human identities don't need user accounts
        if ("NON_HUMAN".equals(event.getIdentityType())) {
            logger.info()
                    .message("Skipping user creation for non-human identity")
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("serviceName", event.getUsername())
                    .log();
            return;
        }

        try {
            User newUser = new User();
            newUser.setId(UUID.randomUUID());
            newUser.setIdentityId(event.getIdentityId());
            newUser.setIdentityType(event.getIdentityType());
            newUser.setUsername(event.getUsername());
            newUser.setEmail(event.getEmail());
            newUser.setFirstName(event.getFirstName());
            newUser.setLastName(event.getLastName());
            newUser.setDisplayName(event.getDisplayName() != null
                    ? event.getDisplayName()
                    : event.getFirstName() + " " + event.getLastName());
            newUser.setTenantId(event.getTenantId());

            // Map identity status to user status
            newUser.setStatus(mapIdentityStatusToUserStatus(event.getStatus()));

            // Initialize security fields
            newUser.setPasswordHash(null); // No password set initially
            newUser.setFailedLoginAttempts(0);
            newUser.setMfaEnabled(false);
            newUser.setIsPasswordExpired(true); // Force password setup

            // Create empty profile data JSON
            JsonNode emptyProfile = objectMapper.createObjectNode();
            newUser.setProfileData(emptyProfile);

            // Set audit fields
            Instant now = Instant.now();
            newUser.setCreatedAt(now);
            newUser.setUpdatedAt(now);

            // Set createdBy from event metadata if available
            if (event.getMetadata() != null && event.getMetadata().containsKey("createdBy")) {
                // You might want to look up the user ID based on username
                // For now, we'll leave it null as it's from Identity service
                newUser.setCreatedBy(null);
            }

            userRepository.save(newUser);

            logger.info()
                    .message("Successfully created user from identity event")
                    .detail("userId", newUser.getId().toString())
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("username", newUser.getUsername())
                    .detail("email", newUser.getEmail())
                    .detail("tenantId", newUser.getTenantId())
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to create user from identity event")
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("username", event.getUsername())
                    .detail("error", e.getMessage())
                    .log();
            throw e;
        }
    }

    /**
     * Handles identity.updated events by updating the corresponding user.
     *
     * @param event the identity updated event
     */
    private void handleIdentityUpdated(IdentityLifecycleEvent event) {
        Optional<User> userOpt = userRepository.findByIdentityId(event.getIdentityId());

        if (!userOpt.isPresent()) {
            logger.warn()
                    .message("User not found for identity update, creating new user")
                    .detail("identityId", event.getIdentityId().toString())
                    .log();
            // Handle as creation if user doesn't exist
            handleIdentityCreated(event);
            return;
        }

        try {
            User user = userOpt.get();

            // Update fields from event
            user.setUsername(event.getUsername());
            user.setEmail(event.getEmail());
            user.setFirstName(event.getFirstName());
            user.setLastName(event.getLastName());
            user.setDisplayName(event.getDisplayName() != null
                    ? event.getDisplayName()
                    : event.getFirstName() + " " + event.getLastName());

            // Update status if changed
            UserStatus newStatus = mapIdentityStatusToUserStatus(event.getStatus());
            if (newStatus != user.getStatus()) {
                user.setStatus(newStatus);

                // If status changed to INACTIVE, lock the account
                if (newStatus == UserStatus.INACTIVE) {
                    user.setLockedUntil(Instant.now().plusSeconds(365 * 24 * 60 * 60)); // 1 year
                }
            }

            user.setUpdatedAt(Instant.now());
            userRepository.save(user);

            logger.info()
                    .message("Successfully updated user from identity event")
                    .detail("userId", user.getId().toString())
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("username", user.getUsername())
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to update user from identity event")
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("error", e.getMessage())
                    .log();
            throw e;
        }
    }

    /**
     * Handles identity.deleted events by marking the user as INACTIVE.
     * We don't hard delete to preserve audit trails.
     *
     * @param event the identity deleted event
     */
    private void handleIdentityDeleted(IdentityLifecycleEvent event) {
        Optional<User> userOpt = userRepository.findByIdentityId(event.getIdentityId());

        if (!userOpt.isPresent()) {
            logger.warn()
                    .message("User not found for identity deletion")
                    .detail("identityId", event.getIdentityId().toString())
                    .log();
            return;
        }

        try {
            User user = userOpt.get();

            // Soft delete: mark as INACTIVE and lock account
            user.setStatus(UserStatus.INACTIVE);
            user.setLockedUntil(Instant.now().plusSeconds(365 * 24 * 60 * 60)); // Lock for 1 year
            user.setUpdatedAt(Instant.now());

            userRepository.save(user);

            logger.info()
                    .message("Successfully deactivated user from identity deletion event")
                    .detail("userId", user.getId().toString())
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("username", user.getUsername())
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to deactivate user from identity deletion event")
                    .detail("identityId", event.getIdentityId().toString())
                    .detail("error", e.getMessage())
                    .log();
            throw e;
        }
    }

    /**
     * Maps Identity service status to Auth service UserStatus.
     *
     * @param identityStatus the identity status (ACTIVE, INACTIVE, SUSPENDED)
     * @return corresponding UserStatus
     */
    private UserStatus mapIdentityStatusToUserStatus(String identityStatus) {
        switch (identityStatus) {
            case "ACTIVE":
                return UserStatus.ACTIVE;
            case "INACTIVE":
                return UserStatus.INACTIVE;
            case "SUSPENDED":
                return UserStatus.LOCKED;
            default:
                logger.warn()
                        .message("Unknown identity status, defaulting to INACTIVE")
                        .detail("status", identityStatus)
                        .log();
                return UserStatus.INACTIVE;
        }
    }
}