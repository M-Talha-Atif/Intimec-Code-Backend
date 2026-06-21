package com.aidentx.access_portal_service.controller;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.response.ApiResponse;
import com.aidentx.access_portal_service.dto.response.access_request.AccessPortalAnalyticsDTO;
import com.aidentx.access_portal_service.service.AccessPortalAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.aidentx.access_portal_service.config.AccessPortalConstants.HEADER_TENANT_ID;
import static com.aidentx.access_portal_service.config.AccessPortalConstants.*;
import static com.aidentx.access_portal_service.util.RequestContextUtil.resolveUserId;

import java.util.UUID;

/**
 * REST controller for access portal analytics.
 * Single endpoint — returns all dashboard metrics in one call.
 */
@RestController
@RequestMapping(API_BASE_ANALYTICS)
@RequiredArgsConstructor
@Tag(name = TAG_ANALYTICS_API, description = TAG_ANALYTICS_API_DESC)
public class AccessPortalAnalyticsController {

    private final AccessPortalAnalyticsService analyticsService;

    /**
     * Returns aggregated analytics for the tenant's access requests
     * within a rolling time window.
     *
     * @param tenantId header — tenant identifier
     * @param days     query param — rolling window in days (default 30)
     * @param userIdHeader user identifier from header
     * @return analytics DTO with all dashboard sections
     */
    @GetMapping
    @Operation(
            summary = OP_GET_ANALYTICS,
            description = OP_GET_ANALYTICS_DESC
    )
    public ResponseEntity<ApiResponse<AccessPortalAnalyticsDTO>> getAnalytics(
            @RequestHeader(value = HEADER_TENANT_ID, required = false) String tenantId,
            @Parameter(description = "Rolling time window in days (e.g. 30, 45, 60)")
            @RequestParam(defaultValue = "" + ANALYTICS_DEFAULT_DAYS) int days,
                    @RequestHeader(HEADER_USER_ID) String userIdHeader
    ) {
        // Guard: clamp days to a sensible range to prevent abuse
        int safeDays = Math.max(1, Math.min(days, 365));

        UUID userId = resolveUserId(userIdHeader);

        AccessPortalAnalyticsDTO response = analyticsService.getAnalytics(userId,tenantId, safeDays);

        return ResponseEntity.ok(
                ApiResponse.success(SUCCESS_ANALYTICS_FETCHED, response)
        );
    }
}