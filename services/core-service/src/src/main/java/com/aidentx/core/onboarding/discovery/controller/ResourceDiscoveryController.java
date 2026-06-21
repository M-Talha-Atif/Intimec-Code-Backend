package com.aidentx.core.onboarding.discovery.controller;

import com.aidentx.core.onboarding.discovery.service.ResourceDiscoveryService;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/connectors") // Standardized base path for all connector operations
public class ResourceDiscoveryController {

    private final ResourceDiscoveryService resourceDiscoveryService;

    public ResourceDiscoveryController(ResourceDiscoveryService resourceDiscoveryService) {
        this.resourceDiscoveryService = resourceDiscoveryService;
    }

    // GET /api/v1/connectors
    @GetMapping
    public ResponseEntity<?> discoverAvailableConnectors() {
        return handleRequest(() -> resourceDiscoveryService.getAvailableConnectors());
    }

    // GET /api/v1/connectors/{connectorId}/properties
    @GetMapping("/{connectorId}/properties")
    public ResponseEntity<?> getConnectorProperties(@PathVariable String connectorId) {
        return handleRequest(() -> resourceDiscoveryService.getConnectorProperties(connectorId));
    }

    // POST /api/v1/connectors/{connectorId}/test
    @PostMapping("/{connectorId}/test")
    public ResponseEntity<?> testConnection(
            @PathVariable String connectorId,
            @RequestBody Map<String, Object> configuration) {
        return handleRequest(() -> resourceDiscoveryService.testConnection(connectorId, configuration));
    }

    // POST /api/v1/connectors/{connectorId}/objects
    @PostMapping("/{connectorId}/objects")
    public ResponseEntity<?> discoverObjectClasses(
            @PathVariable String connectorId,
            @RequestBody Map<String, Object> configuration) {
        return handleRequest(() -> resourceDiscoveryService.discoverObjectClasses(connectorId, configuration));
    }

    // POST /api/v1/connectors/{connectorId}/schema/{objectName}
    @PostMapping("/{connectorId}/schema/{objectName}")
    public ResponseEntity<?> discoverObjectSchema(
            @PathVariable String connectorId,
            @PathVariable String objectName,
            @RequestBody Map<String, Object> configuration) {
        return handleRequest(
                () -> resourceDiscoveryService.discoverObjectSchema(connectorId, objectName, configuration));
    }

    @PostMapping("/{connectorId}/preview/{objectClassName}")
    public ResponseEntity<?> previewObjectType(
            @PathVariable String connectorId,
            @PathVariable String objectClassName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestBody Map<String, Object> requestBody) {
        return handleRequest(() -> resourceDiscoveryService.previewObjectType(connectorId, objectClassName, requestBody, page, size));
    }

    // GET /api/v1/connectors/{connectorId}/icon
    @GetMapping("/{connectorId}/icon")
    public ResponseEntity<byte[]> getConnectorIcon(@PathVariable String connectorId) {
        try {
            ResourceDiscoveryService.IconData iconData = resourceDiscoveryService.getConnectorIconData(connectorId);

            if (iconData.getData() == null || iconData.getData().length == 0) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(iconData.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"connector-icon\"")
                    .body(iconData.getData());

        } catch (StatusRuntimeException e) {
            HttpStatus status = mapGrpcStatus(e.getStatus().getCode());
            return ResponseEntity.status(status).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // --- Private Helper for Consistent Error Handling ---
    private <T> ResponseEntity<?> handleRequest(ServiceCall<T> supplier) {
        try {
            return ResponseEntity.ok(supplier.execute());
        } catch (StatusRuntimeException e) {
            HttpStatus status = mapGrpcStatus(e.getStatus().getCode());
            return ResponseEntity.status(status).body(Map.of(
                    "error", "gRPC Error",
                    "grpc_code", e.getStatus().getCode(),
                    "message", e.getStatus().getDescription()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Application Error",
                    "message", e.getMessage() != null ? e.getMessage() : "An unexpected error occurred."));
        }
    }

    private HttpStatus mapGrpcStatus(io.grpc.Status.Code code) {
        return switch (code) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    @FunctionalInterface
    private interface ServiceCall<T> {
        T execute() throws Exception;
    }
}