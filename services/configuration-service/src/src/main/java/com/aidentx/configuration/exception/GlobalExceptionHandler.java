package com.aidentx.configuration.exception;

import com.aidentx.configuration.config.AppConstants;
import com.aidentx.configuration.exception.artifact.*;
import com.aidentx.configuration.exception.configuration.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for configuration service REST controllers.
 * <p>
 * This class provides centralized exception handling for all configuration service
 * exceptions, ensuring consistent error responses and proper logging.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final IUnifiedLogger logger;

    public GlobalExceptionHandler(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.logger = unifiedLoggerFactory.getLogger("GlobalExceptionHandler", AppConstants.LOG_CATEGORY);
    }

    /**
     * Handles configuration not found exceptions.
     *
     * @param ex      the configuration not found exception
     * @param request the web request
     * @return a ResponseEntity with error details and NOT_FOUND status
     */
    @ExceptionHandler(ConfigurationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationNotFound(
            ConfigurationNotFoundException ex, WebRequest request) {
        
        logger.warn()
                .message("Configuration not found")
                .detail(AppConstants.DETAIL_CONFIG_TYPE, ex.getConfigType())
                .detail(AppConstants.DETAIL_CONFIG_KEY, ex.getConfigKey())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                request.getDescription(false)
        );

        errorResponse.put("configType", ex.getConfigType());
        errorResponse.put("configKey", ex.getConfigKey());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles configuration already exists exceptions.
     *
     * @param ex      the configuration already exists exception
     * @param request the web request
     * @return a ResponseEntity with error details and CONFLICT status
     */
    @ExceptionHandler(ConfigurationAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationAlreadyExists(
            ConfigurationAlreadyExistsException ex, WebRequest request) {
        
        logger.warn()
                .message("Configuration already exists")
                .detail(AppConstants.DETAIL_CONFIG_TYPE, ex.getConfigType())
                .detail(AppConstants.DETAIL_CONFIG_KEY, ex.getConfigKey())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                request.getDescription(false)
        );

        errorResponse.put("configType", ex.getConfigType());
        errorResponse.put("configKey", ex.getConfigKey());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles configuration validation exceptions.
     *
     * @param ex      the configuration validation exception
     * @param request the web request
     * @return a ResponseEntity with error details and BAD_REQUEST status
     */
    @ExceptionHandler(ConfigurationValidationException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationValidation(
            ConfigurationValidationException ex, WebRequest request) {
        
        logger.warn()
                .message("Configuration validation failed")
                .detail(AppConstants.DETAIL_CONFIG_TYPE, ex.getConfigType())
                .detail(AppConstants.DETAIL_CONFIG_KEY, ex.getConfigKey())
                .detail("validationError", ex.getValidationError())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getDescription(false)
        );

        errorResponse.put("configType", ex.getConfigType());
        errorResponse.put("configKey", ex.getConfigKey());
        errorResponse.put("validationError", ex.getValidationError());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles configuration patch exceptions.
     *
     * @param ex      the configuration patch exception
     * @param request the web request
     * @return a ResponseEntity with error details and BAD_REQUEST status
     */
    @ExceptionHandler(ConfigurationPatchException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationPatch(
            ConfigurationPatchException ex, WebRequest request) {
        
        logger.warn()
                .message("Configuration patch failed")
                .detail(AppConstants.DETAIL_CONFIG_TYPE, ex.getConfigType())
                .detail(AppConstants.DETAIL_CONFIG_KEY, ex.getConfigKey())
                .detail("patchError", ex.getPatchError())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getDescription(false)
        );

        errorResponse.put("configType", ex.getConfigType());
        errorResponse.put("configKey", ex.getConfigKey());
        errorResponse.put("patchError", ex.getPatchError());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles configuration encryption exceptions.
     *
     * @param ex      the configuration encryption exception
     * @param request the web request
     * @return a ResponseEntity with error details and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(ConfigurationEncryptionException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationEncryption(
            ConfigurationEncryptionException ex, WebRequest request) {
        
        logger.error(ex)
                .message("Configuration encryption/decryption failed")
                .detail(AppConstants.DETAIL_CONFIG_TYPE, ex.getConfigType())
                .detail(AppConstants.DETAIL_CONFIG_KEY, ex.getConfigKey())
                .detail("operation", ex.getOperation())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                "Internal server error occurred during encryption/decryption operation",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles artifact not found exceptions.
     *
     * @param ex      the artifact not found exception
     * @param request the web request
     * @return a ResponseEntity with error details and NOT_FOUND status
     */
    @ExceptionHandler(ArtifactNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleArtifactNotFound(
            ArtifactNotFoundException ex, WebRequest request) {
        
        logger.warn()
                .message("Artifact not found")
                .detail(AppConstants.DETAIL_CONFIG_TYPE, ex.getConfigType())
                .detail(AppConstants.DETAIL_CONFIG_KEY, ex.getConfigKey())
                .detail("artifactType", ex.getArtifactType())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                request.getDescription(false)
        );

        errorResponse.put("configType", ex.getConfigType());
        errorResponse.put("configKey", ex.getConfigKey());
        errorResponse.put("artifactType", ex.getArtifactType());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles artifact validation exceptions.
     *
     * @param ex      the artifact validation exception
     * @param request the web request
     * @return a ResponseEntity with error details and BAD_REQUEST status
     */
    @ExceptionHandler(ArtifactValidationException.class)
    public ResponseEntity<Map<String, Object>> handleArtifactValidation(
            ArtifactValidationException ex, WebRequest request) {
        
        logger.warn()
                .message("Artifact validation failed")
                .detail(AppConstants.DETAIL_CONFIG_TYPE, ex.getConfigType())
                .detail(AppConstants.DETAIL_CONFIG_KEY, ex.getConfigKey())
                .detail("artifactType", ex.getArtifactType())
                .detail("validationError", ex.getValidationError())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getDescription(false)
        );

        errorResponse.put("configType", ex.getConfigType());
        errorResponse.put("configKey", ex.getConfigKey());
        errorResponse.put("artifactType", ex.getArtifactType());
        errorResponse.put("validationError", ex.getValidationError());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles artifact file exceptions.
     *
     * @param ex      the artifact file exception
     * @param request the web request
     * @return a ResponseEntity with error details and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(ArtifactFileException.class)
    public ResponseEntity<Map<String, Object>> handleArtifactFile(
            ArtifactFileException ex, WebRequest request) {
        
        logger.error(ex)
                .message("Artifact file operation failed")
                .detail(AppConstants.DETAIL_CONFIG_TYPE, ex.getConfigType())
                .detail(AppConstants.DETAIL_CONFIG_KEY, ex.getConfigKey())
                .detail("artifactType", ex.getArtifactType())
                .detail(AppConstants.LOG_DETAIL_FILE_PATH, ex.getFilePath())
                .detail("operation", ex.getOperation())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                "Internal server error occurred during file operation",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles artifact encryption exceptions.
     *
     * @param ex      the artifact encryption exception
     * @param request the web request
     * @return a ResponseEntity with error details and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(ArtifactEncryptionException.class)
    public ResponseEntity<Map<String, Object>> handleArtifactEncryption(
            ArtifactEncryptionException ex, WebRequest request) {
        
        logger.error(ex)
                .message("Artifact encryption/decryption failed")
                .detail(AppConstants.DETAIL_CONFIG_TYPE, ex.getConfigType())
                .detail(AppConstants.DETAIL_CONFIG_KEY, ex.getConfigKey())
                .detail("artifactType", ex.getArtifactType())
                .detail("operation", ex.getOperation())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                "Internal server error occurred during encryption/decryption operation",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles generic configuration service exceptions.
     *
     * @param ex      the configuration service exception
     * @param request the web request
     * @return a ResponseEntity with error details and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(ConfigurationServiceException.class)
    public ResponseEntity<Map<String, Object>> handleConfigurationService(
            ConfigurationServiceException ex, WebRequest request) {
        
        logger.error(ex)
                .message("Configuration service error")
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                "An error occurred in the configuration service",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles generic artifact service exceptions.
     *
     * @param ex      the artifact service exception
     * @param request the web request
     * @return a ResponseEntity with error details and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(ArtifactServiceException.class)
    public ResponseEntity<Map<String, Object>> handleArtifactService(
            ArtifactServiceException ex, WebRequest request) {
        
        logger.error(ex)
                .message("Artifact service error")
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                ex.getErrorCode(),
                "An error occurred in the artifact service",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles gRPC StatusRuntimeException by parsing the error message to provide appropriate HTTP responses.
     *
     * @param ex      the gRPC status runtime exception
     * @param request the web request
     * @return a ResponseEntity with appropriate error details and status code
     */
    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleGrpcStatusRuntimeException(
            StatusRuntimeException ex, WebRequest request) {
        
        String message = ex.getMessage();
        Status.Code statusCode = ex.getStatus().getCode();
        
        logger.warn()
                .message("gRPC call failed")
                .detail("grpcStatus", statusCode.name())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, message)
                .log();

        // Parse the error message to extract meaningful information
        HttpStatus httpStatus;
        String errorCode;
        String userMessage;

        if (message != null && message.contains("Configuration not found")) {
            httpStatus = HttpStatus.NOT_FOUND;
            errorCode = "CONFIGURATION_NOT_FOUND";
            userMessage = "The requested configuration was not found";
        } else if (message != null && message.contains("Artifact not found")) {
            httpStatus = HttpStatus.NOT_FOUND;
            errorCode = "ARTIFACT_NOT_FOUND";
            userMessage = "The requested artifact was not found";
        } else if (message != null && message.contains("validation")) {
            httpStatus = HttpStatus.BAD_REQUEST;
            errorCode = "VALIDATION_ERROR";
            userMessage = "The request data is invalid";
        } else if (message != null && message.contains("already exists")) {
            httpStatus = HttpStatus.CONFLICT;
            errorCode = "RESOURCE_ALREADY_EXISTS";
            userMessage = "The resource already exists";
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            errorCode = "INTERNAL_SERVER_ERROR";
            userMessage = "An internal server error occurred";
        }

        Map<String, Object> errorResponse = createErrorResponse(
                errorCode,
                userMessage,
                httpStatus.value(),
                request.getDescription(false)
        );

        // Include original gRPC error details for debugging
        errorResponse.put("grpcStatus", statusCode.name());
        if (message != null && message.contains(":")) {
            String[] parts = message.split(":", 2);
            if (parts.length > 1) {
                errorResponse.put("details", parts[1].trim());
            }
        }

        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    /**
     * Handles all other unexpected exceptions.
     *
     * @param ex      the exception
     * @param request the web request
     * @return a ResponseEntity with error details and INTERNAL_SERVER_ERROR status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        logger.error(ex)
                .message("Unexpected error occurred")
                .detail(AppConstants.DETAIL_EXCEPTION_TYPE, ex.getClass().getSimpleName())
                .detail(AppConstants.DETAIL_EXCEPTION_MESSAGE, ex.getMessage())
                .log();

        Map<String, Object> errorResponse = createErrorResponse(
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getDescription(false)
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Creates a standardized error response map.
     *
     * @param errorCode   the error code
     * @param message     the error message
     * @param status      the HTTP status code
     * @param path        the request path
     * @return a map containing standardized error information
     */
    private Map<String, Object> createErrorResponse(String errorCode, String message, int status, String path) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", status);
        errorResponse.put("error", errorCode);
        errorResponse.put("message", message);
        errorResponse.put("path", path.replace("uri=", ""));
        return errorResponse;
    }
}
