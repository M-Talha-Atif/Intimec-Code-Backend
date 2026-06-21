package com.aidentx.iam.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the Core Service.
 * <p>
 * This class handles all exceptions thrown by the core service controllers
 * and converts them into appropriate HTTP responses with meaningful error
 * messages.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles DuplicateSchemaException thrown when attempting to create
     * a schema with a code that already exists for the tenant.
     *
     * @param ex      the DuplicateSchemaException
     * @param request the web request
     * @return ResponseEntity with 409 Conflict status and error details
     */
    @ExceptionHandler(DuplicateSchemaException.class)
    public ResponseEntity<Object> handleDuplicateSchemaException(
            DuplicateSchemaException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * Handles DuplicateDefaultSchemaException thrown when attempting to set
     * a schema as default when another default already exists.
     *
     * @param ex      the DuplicateDefaultSchemaException
     * @param request the web request
     * @return ResponseEntity with 409 Conflict status and error details
     */
    @ExceptionHandler(DuplicateDefaultSchemaException.class)
    public ResponseEntity<Object> handleDuplicateDefaultSchemaException(
            DuplicateDefaultSchemaException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * Handles DuplicateFieldKeyException thrown when attempting to create
     * a field with a system key that already exists in the schema.
     *
     * @param ex      the DuplicateFieldKeyException
     * @param request the web request
     * @return ResponseEntity with 409 Conflict status and error details
     */
    @ExceptionHandler(DuplicateFieldKeyException.class)
    public ResponseEntity<Object> handleDuplicateFieldKeyException(
            DuplicateFieldKeyException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * Handles DuplicateIdentityException thrown when attempting to create
     * an identity with a username or email that already exists.
     *
     * @param ex      the DuplicateIdentityException
     * @param request the web request
     * @return ResponseEntity with 409 Conflict status and error details
     */
    @ExceptionHandler(DuplicateIdentityException.class)
    public ResponseEntity<Object> handleDuplicateIdentityException(
            DuplicateIdentityException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * Handles SchemaNotFoundException thrown when a requested schema
     * cannot be found.
     *
     * @param ex      the SchemaNotFoundException
     * @param request the web request
     * @return ResponseEntity with 404 Not Found status and error details
     */
    @ExceptionHandler(SchemaNotFoundException.class)
    public ResponseEntity<Object> handleSchemaNotFoundException(
            SchemaNotFoundException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles FieldNotFoundException thrown when a requested field
     * cannot be found.
     *
     * @param ex      the FieldNotFoundException
     * @param request the web request
     * @return ResponseEntity with 404 Not Found status and error details
     */
    @ExceptionHandler(FieldNotFoundException.class)
    public ResponseEntity<Object> handleFieldNotFoundException(
            FieldNotFoundException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles InactiveSchemaException thrown when attempting to perform
     * operations on an inactive schema.
     *
     * @param ex      the InactiveSchemaException
     * @param request the web request
     * @return ResponseEntity with 400 Bad Request status and error details
     */
    @ExceptionHandler(InactiveSchemaException.class)
    public ResponseEntity<Object> handleInactiveSchemaException(
            InactiveSchemaException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles CoreFieldDeletionException thrown when attempting to delete
     * a core field which is not allowed.
     *
     * @param ex      the CoreFieldDeletionException
     * @param request the web request
     * @return ResponseEntity with 400 Bad Request status and error details
     */
    @ExceptionHandler(CoreFieldDeletionException.class)
    public ResponseEntity<Object> handleCoreFieldDeletionException(
            CoreFieldDeletionException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles validation errors from @Valid annotations on request bodies.
     * This catches validation errors from Jakarta Bean Validation
     * (e.g., @Size, @NotBlank).
     *
     * @param ex      the MethodArgumentNotValidException
     * @param request the web request
     * @return ResponseEntity with 400 Bad Request status and validation error
     *         details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Validation failed");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            Map<String, String> errorDetail = new LinkedHashMap<>();
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errorDetail.put("field", fieldName);
            errorDetail.put("message", errorMessage);
            errors.add(errorDetail);
        });
        body.put("errors", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles constraint violations from direct validation calls.
     * This catches validation errors from manual validation or entity-level
     * constraints.
     *
     * @param ex      the ConstraintViolationException
     * @param request the web request
     * @return ResponseEntity with 400 Bad Request status and validation error
     *         details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Validation failed");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        List<Map<String, String>> errors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            Map<String, String> errorDetail = new LinkedHashMap<>();
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errorDetail.put("field", fieldName);
            errorDetail.put("message", errorMessage);
            errors.add(errorDetail);
        }
        body.put("errors", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles IllegalArgumentException thrown for invalid input parameters.
     *
     * @param ex      the IllegalArgumentException
     * @param request the web request
     * @return ResponseEntity with 400 Bad Request status and error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles generic RuntimeException as a fallback.
     *
     * @param ex      the RuntimeException
     * @param request the web request
     * @return ResponseEntity with 500 Internal Server Error status and error
     *         details
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Object> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred: " + ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles ShadowNotFoundException thrown when a shadow is not found.
     *
     * @param ex      the ShadowNotFoundException
     * @param request the web request
     * @return ResponseEntity with 404 Not Found status and error details
     */
    @ExceptionHandler(com.aidentx.iam.shadow_management.exception.ShadowNotFoundException.class)
    public ResponseEntity<Object> handleShadowNotFoundException(
            com.aidentx.iam.shadow_management.exception.ShadowNotFoundException ex,
            WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Not Found");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles ShadowAlreadyLinkedException thrown when attempting to link
     * a shadow that is already linked.
     *
     * @param ex      the ShadowAlreadyLinkedException
     * @param request the web request
     * @return ResponseEntity with 400 Bad Request status and error details
     */
    @ExceptionHandler(com.aidentx.iam.shadow_management.exception.ShadowAlreadyLinkedException.class)
    public ResponseEntity<Object> handleShadowAlreadyLinkedException(
            com.aidentx.iam.shadow_management.exception.ShadowAlreadyLinkedException ex,
            WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles DuplicateShadowException thrown when attempting to create
     * a duplicate shadow.
     *
     * @param ex      the DuplicateShadowException
     * @param request the web request
     * @return ResponseEntity with 409 Conflict status and error details
     */
    @ExceptionHandler(com.aidentx.iam.shadow_management.exception.DuplicateShadowException.class)
    public ResponseEntity<Object> handleDuplicateShadowException(
            com.aidentx.iam.shadow_management.exception.DuplicateShadowException ex,
            WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * Handles ShadowNotLinkedException thrown when attempting to operate on
     * a shadow that is not linked.
     *
     * @param ex      the ShadowNotLinkedException
     * @param request the web request
     * @return ResponseEntity with 400 Bad Request status and error details
     */
    @ExceptionHandler(com.aidentx.iam.shadow_management.exception.ShadowNotLinkedException.class)
    public ResponseEntity<Object> handleShadowNotLinkedException(
            com.aidentx.iam.shadow_management.exception.ShadowNotLinkedException ex,
            WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles generic Exception as a fallback for any unhandled exceptions.
     *
     * @param ex      the Exception
     * @param request the web request
     * @return ResponseEntity with 500 Internal Server Error status and error
     *         details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(
            Exception ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred");
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
