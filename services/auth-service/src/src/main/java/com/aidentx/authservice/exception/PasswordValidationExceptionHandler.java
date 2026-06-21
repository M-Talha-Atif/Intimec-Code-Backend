package com.aidentx.authservice.exception;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for password validation errors.
 * <p>
 * This handler catches PasswordValidationException and returns
 * a structured response with detailed validation errors.
 * </p>
 * 
 * @author Aidentx
 * @since 1.0
 */
@RestControllerAdvice
public class PasswordValidationExceptionHandler {

    private final IUnifiedLogger logger;

    @Autowired
    public PasswordValidationExceptionHandler(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.logger = unifiedLoggerFactory.getLogger("PasswordValidationExceptionHandler", "auth");
    }

    /**
     * Handles PasswordValidationException and returns detailed validation errors.
     *
     * @param ex the password validation exception
     * @return ResponseEntity with validation errors
     */
    @ExceptionHandler(PasswordValidationException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordValidationException(PasswordValidationException ex) {
        logger.warn().message("Password validation failed")
                .detail("errorCount", ex.getValidationErrors().size())
                .detail("errors", String.join(", ", ex.getValidationErrors()))
                .log();

        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Password validation failed");
        response.put("errors", ex.getValidationErrors());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.badRequest().body(response);
    }
}
