package com.aidentx.llmservice.exception;

import com.aidentx.llmservice.dto.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the LLM service.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle provider not found exceptions.
     */
    @ExceptionHandler(ProviderNotFoundException.class)
    public ResponseEntity<LlmResponse> handleProviderNotFound(ProviderNotFoundException ex) {
        logger.error("Provider not found: {}", ex.getMessage());
        
        LlmResponse errorResponse = new LlmResponse();
        errorResponse.setSuccess(false);
        errorResponse.setErrorMessage(ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", errors);
        
        logger.warn("Validation failed: {}", errors);
        
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle general runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<LlmResponse> handleRuntimeException(RuntimeException ex) {
        logger.error("Runtime exception occurred", ex);
        
        LlmResponse errorResponse = new LlmResponse();
        errorResponse.setSuccess(false);
        errorResponse.setErrorMessage("An internal error occurred: " + ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle general exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<LlmResponse> handleGeneralException(Exception ex) {
        logger.error("Unexpected exception occurred", ex);
        
        LlmResponse errorResponse = new LlmResponse();
        errorResponse.setSuccess(false);
        errorResponse.setErrorMessage("An unexpected error occurred");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
