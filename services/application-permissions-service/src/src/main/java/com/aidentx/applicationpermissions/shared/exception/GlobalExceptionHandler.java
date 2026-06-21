package com.aidentx.applicationpermissions.shared.exception;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Application Permissions Service.
 * Provides centralized error handling and logging for all controllers.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final IUnifiedLogger logger;

    @Autowired
    public GlobalExceptionHandler(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(AppConstants.GLOBAL_EXCEPTION_HANDLER_LOGGER_NAME, AppConstants.LOG_CATEGORY_EXCEPTION_HANDLING);
    }

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        errors.put(AppConstants.RESPONSE_KEY_MESSAGE, AppConstants.HTTP_400_BAD_REQUEST);
        errors.put(AppConstants.RESPONSE_KEY_STATUS, HttpStatus.BAD_REQUEST.value());
        errors.put(AppConstants.RESPONSE_KEY_ERRORS, fieldErrors);
        
        logger.warn()
            .message(AppConstants.LOG_VALIDATION_ERROR_OCCURRED)
            .detail(AppConstants.DETAIL_KEY_FIELD_ERRORS, fieldErrors.toString())
            .objectType(AppConstants.LOG_OBJECT_TYPE_VALIDATION_ERROR)
            .log();
        
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put(AppConstants.RESPONSE_KEY_MESSAGE, AppConstants.HTTP_403_FORBIDDEN);
        error.put(AppConstants.RESPONSE_KEY_STATUS, HttpStatus.FORBIDDEN.value());
        error.put(AppConstants.RESPONSE_KEY_ERROR, ex.getMessage());
        
        logger.warn()
            .message(AppConstants.LOG_ACCESS_DENIED)
            .detail(AppConstants.DETAIL_KEY_ERROR, ex.getMessage())
            .objectType(AppConstants.LOG_OBJECT_TYPE_ACCESS_DENIED)
            .log();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Handles general runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put(AppConstants.RESPONSE_KEY_MESSAGE, AppConstants.HTTP_500_INTERNAL_ERROR);
        error.put(AppConstants.RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put(AppConstants.RESPONSE_KEY_ERROR, ex.getMessage());
        
        logger.error(ex)
            .message(AppConstants.LOG_RUNTIME_EXCEPTION_OCCURRED)
            .detail(AppConstants.DETAIL_KEY_ERROR, ex.getMessage())
            .detail(AppConstants.DETAIL_KEY_EXCEPTION_TYPE, ex.getClass().getSimpleName())
            .objectType(AppConstants.LOG_OBJECT_TYPE_RUNTIME_EXCEPTION)
            .log();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> error = new HashMap<>();
        error.put(AppConstants.RESPONSE_KEY_MESSAGE, AppConstants.HTTP_500_INTERNAL_ERROR);
        error.put(AppConstants.RESPONSE_KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put(AppConstants.RESPONSE_KEY_ERROR, ex.getMessage());
        
        logger.error(ex)
            .message(AppConstants.LOG_UNEXPECTED_EXCEPTION_OCCURRED)
            .detail(AppConstants.DETAIL_KEY_ERROR, ex.getMessage())
            .detail(AppConstants.DETAIL_KEY_EXCEPTION_TYPE, ex.getClass().getSimpleName())
            .objectType(AppConstants.LOG_OBJECT_TYPE_EXCEPTION)
            .log();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}