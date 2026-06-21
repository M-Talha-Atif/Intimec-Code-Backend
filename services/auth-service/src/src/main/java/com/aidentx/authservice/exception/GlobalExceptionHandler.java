package com.aidentx.authservice.exception;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.model.dto.ErrorResponse;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * Intercepts exceptions thrown in controllers and maps them to appropriate HTTP responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private final IUnifiedLogger unifiedLogger;

    public GlobalExceptionHandler(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.unifiedLogger = unifiedLoggerFactory.getLogger("GlobalExceptionHandler", "exception");
    }

    /**
     * Handles custom application exceptions and maps them to a 400 Bad Request response.
     *
     * @param ex the custom exception thrown
     * @return a ResponseEntity with an ErrorResponse containing error messages
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        List<ErrorResponse.ErrorMessage> errorMessages = List.of(new ErrorResponse.ErrorMessage(ex.getMessage()));
        ErrorResponse errorResponse = new ErrorResponse(errorMessages);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles validation errors on method arguments (e.g., @Valid).
     * Maps them to a 400 Bad Request response with detailed field error messages.
     *
     * @param ex the validation exception thrown
     * @return a ResponseEntity with an ErrorResponse containing field-level error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ErrorResponse.ErrorMessage> errorMessages = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.ErrorMessage(error.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(errorMessages);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles cases where a user is not found in the system.
     * Maps them to a 404 Not Found response with structured timestamped error details.
     *
     * @param ex the UserNotFoundException thrown
     * @return a ResponseEntity with a detailed error structure
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Object> handleUserNotFoundException(UserNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorDetails(
                        Instant.now(),
                        HttpStatus.NOT_FOUND.value(),
                        AppConstants.USER_NOT_FOUND_TITLE,
                        ex.getMessage(),
                        request.getRequestURI()  // now 'request' is available here
                )
        );
    }


    /**
     * Handles authentication failures due to invalid username or password.
     * Maps them to a 401 Unauthorized response with structured error details including request path.
     *
     * @param ex      the InvalidCredentialsException thrown
     * @param request the HTTP request that caused the exception
     * @return a ResponseEntity with detailed error structure and request path
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Object> handleInvalidCredentialsException(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                new ErrorDetails(
                        Instant.now(),
                        HttpStatus.UNAUTHORIZED.value(),
                        AppConstants.UNAUTHORIZED_TITLE,
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles cases where a user's account is locked.
     * Maps the exception to a 423 Locked HTTP status response with detailed error information.
     *
     * @param ex      the AccountLockedException thrown
     * @param request the HTTP servlet request that caused the exception
     * @return a ResponseEntity containing error details with status 423 LOCKED
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Object> handleAccountLockedException(
            AccountLockedException ex,
            HttpServletRequest request) {

        ErrorDetails errorDetails = new ErrorDetails(
                Instant.now(),
                HttpStatus.LOCKED.value(),
                AppConstants.ACCOUNT_LOCKED_TITLE,
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.LOCKED).body(errorDetails);
    }

    /**
     * Handles cases where the old password is incorrect during password update.
     * Maps them to a 400 Bad Request response with structured error details including request path.
     *
     * @param ex      the InvalidOldPasswordException thrown
     * @param request the HTTP request that caused the exception
     * @return a ResponseEntity with detailed error structure and request path
     */
    @ExceptionHandler(InvalidOldPasswordException.class)
    public ResponseEntity<Object> handleInvalidOldPasswordException(
            InvalidOldPasswordException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ErrorDetails(
                        Instant.now(),
                        HttpStatus.BAD_REQUEST.value(),
                        AppConstants.INVALID_OLD_PASSWORD_TITLE,
                        ex.getMessage(),
                        request.getRequestURI()
                )
        );
    }

    /**
     * Handles SAML redirect exceptions by returning an HTTP 302 redirect to the front-end URL,
     * appending the error message as a query parameter if present.
     *
     * @param ex the SamlRedirectException thrown
     * @return a ResponseEntity with HTTP 302 and Location header set to the front-end URL (with error message)
     */
    @ExceptionHandler(SamlRedirectException.class)
    public ResponseEntity<Void> handleSamlRedirectException(SamlRedirectException ex) {
        String url = ex.getRedirectUrl();
        String errorMsg = ex.getErrorMessage();
        if (errorMsg != null && !errorMsg.isBlank()) {
            // Append error as a query parameter (URL-encoded)
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "error=" + java.net.URLEncoder.encode(errorMsg, java.nio.charset.StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(HttpStatus.FOUND) // 302
                .header("Location", url)
                .build();
    }

    /**
     * Handles OAuth2 redirect exceptions by returning an HTTP 302 redirect to the front-end URL,
     * appending the error message as a query parameter if present.
     *
     * @param ex the OAuth2RedirectException thrown
     * @return a ResponseEntity with HTTP 302 and Location header set to the front-end URL (with error message)
     */
    @ExceptionHandler(OAuth2RedirectException.class)
    public ResponseEntity<Void> handleOAuth2RedirectException(OAuth2RedirectException ex) {
        String url = ex.getRedirectUrl();
        String errorMsg = ex.getMessage();
        if (errorMsg != null && !errorMsg.isBlank()) {
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "error=" + java.net.URLEncoder.encode(errorMsg, java.nio.charset.StandardCharsets.UTF_8);
        }
        return ResponseEntity.status(HttpStatus.FOUND) // 302
                .header("Location", url)
                .build();
    }

    /**
     * Handles policy already exists exceptions.
     * Maps them to a 409 Conflict response with detailed error information.
     *
     * @param ex      the PolicyAlreadyExistsException thrown
     * @param request the HTTP servlet request that caused the exception
     * @return a ResponseEntity containing error details with status 409 CONFLICT
     */
    @ExceptionHandler(PolicyAlreadyExistsException.class)
    public ResponseEntity<Object> handlePolicyAlreadyExistsException(
            PolicyAlreadyExistsException ex,
            HttpServletRequest request) {

        ErrorDetails errorDetails = new ErrorDetails(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Policy Already Exists",
                ex.getMessage(),
                request.getRequestURI()
        );

        unifiedLogger.warn()
                .message("Policy already exists exception")
                .detail("path", request.getRequestURI())
                .detail("errorMessage", ex.getMessage())
                .log();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorDetails);
    }

    /**
     * Handles policy not found exceptions.
     * Maps them to a 404 Not Found response with detailed error information.
     *
     * @param ex      the PolicyNotFoundException thrown
     * @param request the HTTP servlet request that caused the exception
     * @return a ResponseEntity containing error details with status 404 NOT_FOUND
     */
    @ExceptionHandler(PolicyNotFoundException.class)
    public ResponseEntity<Object> handlePolicyNotFoundException(
            PolicyNotFoundException ex,
            HttpServletRequest request) {

        ErrorDetails errorDetails = new ErrorDetails(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Policy Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        unifiedLogger.warn()
                .message("Policy not found exception")
                .detail("path", request.getRequestURI())
                .detail("errorMessage", ex.getMessage())
                .log();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorDetails);
    }

    /**
     * Handles password validation failures with detailed error arrays.
     * Returns a 400 Bad Request response with validation error details.
     *
     * @param ex      the PasswordValidationException thrown
     * @param request the HTTP request that caused the exception
     * @return a ResponseEntity with detailed validation error structure and HTTP 400 status
     */
    @ExceptionHandler(PasswordValidationException.class)
    public ResponseEntity<Object> handlePasswordValidationException(PasswordValidationException ex, HttpServletRequest request) {
        unifiedLogger.warn()
                .message(com.aidentx.authservice.service.Auth.AppConstants.LOG_PASSWORD_VALIDATION_FAILED)
                .detail("path", request.getRequestURI())
                .detail("errorCount", ex.getValidationErrors().size())
                .detail("errors", String.join(", ", ex.getValidationErrors()))
                .log();

        // Create response with errors array
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("timestamp", Instant.now().toEpochMilli());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", com.aidentx.authservice.service.Auth.AppConstants.PASSWORD_VALIDATION_FAILED_TITLE);
        response.put("message", com.aidentx.authservice.service.Auth.AppConstants.PASSWORD_VALIDATION_FAILED_MESSAGE);
        response.put("errors", ex.getValidationErrors());
        response.put("path", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles all other uncaught exceptions.
     * Returns a generic 500 Internal Server Error response with a safe error message.
     *
     * @param ex      the exception thrown
     * @param request the HTTP request that caused the exception
     * @return a ResponseEntity with generic error details and HTTP 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorDetails errorDetails = new ErrorDetails(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                AppConstants.GENERIC_ERROR_TITLE,
                AppConstants.GENERIC_ERROR_MESSAGE,
                request.getRequestURI()
        );
        // Log the exception using unified structured logger
        unifiedLogger.error(ex)
                .message(AppConstants.GENERIC_ERROR_MESSAGE)
                .detail("path", request.getRequestURI())
                .log();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }
}
