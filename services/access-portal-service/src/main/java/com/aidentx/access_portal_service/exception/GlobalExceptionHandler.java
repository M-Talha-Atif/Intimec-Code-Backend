package com.aidentx.access_portal_service.exception;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.response.ApiResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final IUnifiedLogger logger;

    public GlobalExceptionHandler(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(
                AccessPortalConstants.EXCEPTION_LOGGER_NAME,
                AccessPortalConstants.LOG_CATEGORY_EXCEPTION
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        logger.error()
                .message("Illegal argument exception")
                .detail("error", ex.getMessage())
                .detail("path", request.getRequestURI())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ILLEGAL_ARGUMENT)
                .log();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(
            DuplicateResourceException ex,
            HttpServletRequest request
    ) {
        logger.warn()
                .message("Duplicate resource detected")
                .detail("error", ex.getMessage())
                .detail("path", request.getRequestURI())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_DUPLICATE_ERROR)
                .log();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String errorMsg = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        logger.error()
                .message("Request validation failed")
                .detail("error", errorMsg)
                .detail("path", request.getRequestURI())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_ERROR)
                .log();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMsg));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleCustomValidation(
            ValidationException ex,
            HttpServletRequest request
    ) {
        logger.error()
                .message("Business validation failed")
                .detail("error", ex.getMessage())
                .detail("errors", ex.getErrors().toString())
                .detail("path", request.getRequestURI())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_VALIDATION_ERROR)
                .log();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .data(ex.getErrors())
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        logger.warn()
                .message("Access denied")
                .detail("error", ex.getMessage())
                .detail("path", request.getRequestURI())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ACCESS_DENIED)
                .log();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.<String>builder()
                        .success(false)
                        .message(ex.getMessage())
                        .path(request.getRequestURI())
                        .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        logger.error()
                .message("Illegal state encountered")
                .detail("error", ex.getMessage())
                .detail("path", request.getRequestURI())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_ILLEGAL_STATE)
                .log();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.CONFLICT.value()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(
            SecurityException ex,
            HttpServletRequest request
    ) {
        logger.error()
                .message("Security exception occurred")
                .detail("error", ex.getMessage())
                .detail("path", request.getRequestURI())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_SECURITY_EXCEPTION)
                .log();

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), HttpStatus.FORBIDDEN.value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleUnexpected(
            Exception ex,
            HttpServletRequest request
    ) {
        logger.error()
                .message("Unhandled system exception")
                .detail("exceptionClass", ex.getClass().getName())
                .detail("error", ex.getMessage())
                .detail("path", request.getRequestURI())
                .objectType(AccessPortalConstants.LOG_OBJECT_TYPE_UNEXPECTED_EXCEPTION)
                .log();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }

    @ExceptionHandler(DuplicateBeneficiaryException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateBeneficiaryException(
            DuplicateBeneficiaryException ex,
            HttpServletRequest request) {

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("conflictingUsers", ex.getConflictingUserNames());
        errorDetails.put("conflictingUserIds", ex.getConflictingUserIds());

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage(), errorDetails);
        response.setPath(request.getRequestURI());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }
}
