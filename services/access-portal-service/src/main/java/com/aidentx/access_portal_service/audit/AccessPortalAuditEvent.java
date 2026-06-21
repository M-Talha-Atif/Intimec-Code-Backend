package com.aidentx.access_portal_service.audit;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

/**
 * Record representing an access portal audit event.
 * Used to capture tile/cart/access request actions for audit logging.
 *
 * @param tenantId The tenant ID
 * @param actorId The ID of the actor performing the action
 * @param actorType The type of actor (user, system)
 * @param actorName The name/username of the actor
 * @param entityId The ID of the entity being acted upon (tile, cart item, access request)
 * @param entityType The type of entity (TILE, CART_ITEM, ACCESS_REQUEST)
 * @param action The action performed (tile-created, cart-item-added, access-request-submitted)
 * @param stateBefore The state before the action
 * @param stateAfter The state after the action
 * @param clientIp The client IP address
 * @param userAgent The user agent string
 */
public record AccessPortalAuditEvent(
        String tenantId,
        UUID actorId,
        String actorType,      // user / system
        String actorName,
        UUID entityId,
        String entityType,     // TILE, CART_ITEM, ACCESS_REQUEST
        String action,         // tile-created, cart-item-added, access-request-submitted
        String riskLevel,
        JsonNode stateBefore,
        JsonNode stateAfter,
        String clientIp,
        String userAgent
) {
    /**
     * Creates an audit event with system actor.
     */
    public static AccessPortalAuditEvent system(
            String tenantId,
            UUID entityId,
            String entityType,
            String action,
            String riskLevel,
            JsonNode stateBefore,
            JsonNode stateAfter
    ) {
        return new AccessPortalAuditEvent(
                tenantId,
                null,
                "system",
                "System",
                entityId,
                entityType,
                action,
                riskLevel,
                stateBefore,
                stateAfter,
                null,
                null
        );
    }

    /**
     * Creates an audit event with user actor.
     */
    public static AccessPortalAuditEvent user(
            UUID actorId,
            String actorName,
            String tenantId,
            UUID entityId,
            String entityType,
            String riskLevel,
            String action,
            JsonNode stateBefore,
            JsonNode stateAfter,
            String clientIp,
            String userAgent
    ) {
        return new AccessPortalAuditEvent(
                tenantId,
                actorId,
                "user",
                actorName,
                entityId,
                entityType,
                action,
                riskLevel,
                stateBefore,
                stateAfter,
                clientIp,
                userAgent
        );
    }
}
