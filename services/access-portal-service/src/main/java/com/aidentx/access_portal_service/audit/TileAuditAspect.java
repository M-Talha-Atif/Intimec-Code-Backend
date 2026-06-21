package com.aidentx.access_portal_service.audit;

import com.aidentx.access_portal_service.dto.response.tile.TileResponseDTO;
import com.aidentx.access_portal_service.entity.tile.Tile;
import com.aidentx.access_portal_service.repository.tile.TileRepository;
import com.aidentx.access_portal_service.util.JwtUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static com.aidentx.access_portal_service.config.AccessPortalConstants.*;

/**
 * AOP Aspect for auditing Tile operations (create, update, delete).
 * Uses TileAuditDTO to prevent circular reference issues during serialization.
 */
@Aspect
@Component
public class TileAuditAspect {

    private static final String LOG_CATEGORY = LOG_CATEGORY_TILE_AUDIT;

    private final TileRepository tileRepository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    private final AccessPortalAuditPublisher auditPublisher;
    private final JwtUtils jwtUtils;
    // Thread-local storage for state before update
    private static final ThreadLocal<JsonNode> TILE_STATE_BEFORE = new ThreadLocal<>();


    public TileAuditAspect(
            TileRepository tileRepository,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory,
            AccessPortalAuditPublisher auditPublisher, JwtUtils jwtUtils) {
        this.tileRepository = tileRepository;
        this.jwtUtils = jwtUtils;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), LOG_CATEGORY);
        this.auditPublisher = auditPublisher;
        logger.info().message("TileAuditAspect initialized").log();
    }

    /**
     * Extracts tenant ID from the HTTP request headers.
     */
    private String getCurrentTenantId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                return request.getHeader("X-Tenant-Id");
            }
        } catch (Exception e) {
            logger.warn().message("Failed to get tenantId").detail("error", e.getMessage()).log();
        }
        return DEFAULT_TENANT_ID;
    }

    /**
     * Extracts user ID from the HTTP request headers.
     */
    private UUID getCurrentUserId() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String userId = request.getHeader("X-User-Id");
                if (userId != null) return UUID.fromString(userId);
            }
        } catch (Exception e) {
            logger.warn().message("Failed to get userId").detail("error", e.getMessage()).log();
        }
        return null;
    }

    /**
     * Extract current username from the current HTTP request.
     */
    private String getCurrentUsername() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            logger.debug()
                    .category(LOG_CATEGORY)
                    .message("Attempting to extract username")
                    .detail("hasAttributes", attributes != null)
                    .log();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // Log all headers for debugging
                String xUsername = request.getHeader("X-Username");
                String authHeader = request.getHeader("Authorization");
                logger.debug()
                        .category(LOG_CATEGORY)
                        .message("Request headers check")
                        .detail("X-Username", xUsername != null ? xUsername : "NULL")
                        .detail("hasAuthorization", authHeader != null)
                        .log();

                String username = jwtUtils.extractUsername(request);
                logger.debug()
                        .category(LOG_CATEGORY)
                        .message("Username extraction result")
                        .detail("username", username != null ? username : "NULL")
                        .log();

                if (username != null) {
                    return username;
                }
            } else {
                logger.warn()
                        .category(LOG_CATEGORY)
                        .message("RequestContextHolder has no attributes - HTTP context lost")
                        .log();
            }
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message("Error extracting username from request")
                    .log();
        }
        return null;
    }


    /**
     * Converts a Tile entity to JsonNode using audit-safe DTO.
     */
    private JsonNode tileToJsonNode(Tile tile) {
        TileAuditDTO dto = TileAuditDTO.from(tile);
        return objectMapper.valueToTree(dto);
    }

    @AfterReturning(
            pointcut = "execution(* com.aidentx.access_portal_service.service.impl.TileServiceImpl.createTile(..))",
            returning = "result"
    )
    public void auditTileCreated(JoinPoint joinPoint, Object result) {
        try {
            TileResponseDTO dto = (TileResponseDTO) result;
            Tile tile = tileRepository.findById(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Tile not found: " + dto.getId()));

            // Convert to DTO first to prevent circular reference
            JsonNode stateAfter = tileToJsonNode(tile);
            UUID actorId = getCurrentUserId();

            AccessPortalAuditEvent event = actorId != null
                    ? AccessPortalAuditEvent.user(
                    actorId, getCurrentUsername(), getCurrentTenantId(),
                    tile.getId(), "TILE", tile.getMaximumRiskLevel(), "tile-created",
                    null, stateAfter, null, null
            )
                    : AccessPortalAuditEvent.system(
                    getCurrentTenantId(), tile.getId(),
                    "TILE", "tile-created", tile.getMaximumRiskLevel(),
                    null, stateAfter
            );

            auditPublisher.publish(event);
            logger.info()
                    .message("Published TILE_CREATED event")
                    .objectId(tile.getId().toString())
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to publish TILE_CREATED event")
                    .log();
        }
    }


    @Before(
            value = "execution(* com.aidentx.access_portal_service.service.impl.TileServiceImpl.updateTile(..)) && args(tenantId, tileId, modifiedBy, dto)",
            argNames = "tenantId,tileId,modifiedBy,dto"
    )
    public void captureTileBeforeUpdate(String tenantId, UUID tileId, UUID modifiedBy, Object dto) {
        try {
            // Fetch the tile BEFORE the update happens
            Tile tileBefore = tileRepository.findByIdWithRelations(tileId).orElse(null);

            if (tileBefore != null) {
                JsonNode stateBefore = tileToJsonNode(tileBefore);
                TILE_STATE_BEFORE.set(stateBefore);

                logger.debug()
                        .message("Captured tile state before update")
                        .detail("tileId", tileId.toString())
                        .log();
            } else {
                TILE_STATE_BEFORE.remove();
            }
        } catch (Exception e) {
            logger.warn()
                    .message("Failed to capture tile state before update")
                    .detail("tileId", tileId.toString())
                    .detail("error", e.getMessage())
                    .log();
            TILE_STATE_BEFORE.remove();
        }
    }

    @AfterReturning(
            pointcut = "execution(* com.aidentx.access_portal_service.service.impl.TileServiceImpl.updateTile(..))",
            returning = "result"
    )
    public void auditTileUpdated(JoinPoint joinPoint, Object result) {
        try {
            TileResponseDTO dto = (TileResponseDTO) result;

            // Fetch the tile AFTER the update
            Tile tileAfter = tileRepository.findByIdWithRelations(dto.getId())
                    .orElseThrow(() -> new RuntimeException("Tile not found: " + dto.getId()));

            JsonNode stateAfter = tileToJsonNode(tileAfter);
            JsonNode stateBefore = TILE_STATE_BEFORE.get();

            UUID actorId = getCurrentUserId();

            AccessPortalAuditEvent event = actorId != null
                    ? AccessPortalAuditEvent.user(
                    actorId,
                    getCurrentUsername(),
                    getCurrentTenantId(),
                    tileAfter.getId(),
                    "TILE",
                    tileAfter.getMaximumRiskLevel(),
                    "tile-updated",
                    stateBefore,  // State BEFORE update
                    stateAfter,   // State AFTER update
                    null,
                    null
            )
                    : AccessPortalAuditEvent.system(
                    getCurrentTenantId(),
                    tileAfter.getId(),
                    "TILE",
                    "tile-updated",
                    tileAfter.getMaximumRiskLevel(),
                    stateBefore,  // State BEFORE update
                    stateAfter    // State AFTER update
            );

            auditPublisher.publish(event);

            logger.info()
                    .message("Published TILE_UPDATED event")
                    .objectId(tileAfter.getId().toString())
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to publish TILE_UPDATED event")
                    .log();
        } finally {
            // Always clean up ThreadLocal to prevent memory leaks
            TILE_STATE_BEFORE.remove();
        }
    }

    @Before(
            value = "execution(* com.aidentx.access_portal_service.service.impl.TileServiceImpl.deleteTile(..)) && args(tileId)",
            argNames = "tileId"
    )
    public void auditTileDeleted(UUID tileId) {
        try {
            Tile tile = tileRepository.findById(tileId)
                    .orElse(null);

            String riskLevel = tile != null ? tile.getMaximumRiskLevel() : null;
            JsonNode stateBefore = tile != null ? tileToJsonNode(tile) : null;

            UUID actorId = getCurrentUserId();

            AccessPortalAuditEvent event = actorId != null
                    ? AccessPortalAuditEvent.user(
                    actorId,
                    getCurrentUsername(),
                    getCurrentTenantId(),
                    tileId,
                    "TILE",
                    riskLevel,
                    "tile-deleted",
                    stateBefore,
                    null,
                    null,
                    null
            )
                    : AccessPortalAuditEvent.system(
                    getCurrentTenantId(),
                    tileId,
                    "TILE",
                    "tile-deleted",
                    riskLevel,
                    stateBefore,
                    null
            );

            auditPublisher.publish(event);

        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to publish TILE_DELETED event")
                    .objectId(tileId.toString())
                    .log();
        }
    }

}