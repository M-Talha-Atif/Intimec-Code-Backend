package com.aidentx.authservice.controllers;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.config.IdpConfigProperties;
import com.aidentx.authservice.model.dto.*;
import com.aidentx.authservice.service.Auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for authentication endpoints.
 * Handles login, logout, and token refresh operations for users.
 *
 * <p>Endpoints include:
 * <ul>
 *   <li>User login with credentials</li>
 *   <li>Token refresh using a valid refresh token</li>
 *   <li>User logout (deletes refresh tokens)</li>
 * </ul>
 *
 * <p>All user-facing messages are managed via {@link AppConstants}.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final IdpConfigProperties idpConfig;

    public AuthController(AuthService authService, IdpConfigProperties idpConfig) {
        this.authService = authService;
        this.idpConfig = idpConfig;
    }

    /**
     * Authenticates a user with given credentials.
     *
     * @param request the login request containing username/email and password
     * @return a successful login response with access and refresh tokens
     */
    @PostMapping("/login")
    public LoginSuccessResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Refreshes the access token using a valid refresh token.
     *
     * @param request the refresh token request containing the refresh token
     * @return a successful response containing a new access token and the same refresh token
     */
    @PostMapping("/refresh")
    public LoginSuccessResponse<TokenResponse> refreshAccessToken(@RequestBody @Valid RefreshTokenRequest request) {
        return authService.refreshAccessToken(request);
    }


    /**
     * Endpoint to log out the currently authenticated user.
     * <p>
     * Checks authentication status and extracts the user ID from the security context.
     * Calls the service layer to perform logout by deleting the specific refresh token and session.
     * 
     * Request body should contain:
     * {
     *   "refreshToken": "your-refresh-token-here"  // Required to logout from specific device
     * }
     *
     * @param request the logout request containing the refresh token
     * @return HTTP 200 with success message if logout succeeds,
     * HTTP 401 if not authenticated or invalid principal,
     * HTTP 400 if logout fails due to other errors.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody(required = false) Map<String, String> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AppConstants.NOT_AUTHENTICATED);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AppConstants.INVALID_AUTH_PRINCIPAL);
        }

        try {
            UUID userId = UUID.fromString(userDetails.getUsername());
            String refreshToken = (request != null) ? request.get("refreshToken") : null;
            authService.logout(userId, refreshToken);
            return ResponseEntity.ok(AppConstants.LOGOUT_SUCCESS);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AppConstants.LOGOUT_FAILURE_PREFIX + e.getMessage());
        }
    }

    /**
     * Validates if the current user's session exists in the system.
     * Used on frontend initialization to verify session validity.
     * Requires authentication - extracts session_id from cookie via X-Session-Id header.
     *
     * @param request HTTP request to extract session_id from X-Session-Id header
     * @return HTTP 200 with {valid: true} if session exists,
     *         HTTP 200 with {valid: false} if session doesn't exist,
     *         HTTP 401 if not authenticated
     */
    @GetMapping("/validate-session")
    public ResponseEntity<Map<String, Boolean>> validateSession(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("valid", false));
        }

        try {
            // Extract session_id from X-Session-Id header (passed from frontend cookie)
            String sessionId = request.getHeader("X-Session-Id");
            
            if (sessionId == null || sessionId.isEmpty()) {
                return ResponseEntity.ok(Map.of("valid", false));
            }

            boolean isValid = authService.isSessionValid(UUID.fromString(sessionId));
            return ResponseEntity.ok(Map.of("valid", isValid));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
    }

    /**
     * Endpoint to get all active sessions for the authenticated user's tenant.
     * Extracts tenantId from the JWT token and returns all sessions with user details.
     *
     * @param request HTTP request to extract the JWT token
     * @return HTTP 200 with list of sessions,
     * HTTP 401 if not authenticated
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getAllSessions(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String search) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AppConstants.NOT_AUTHENTICATED);
        }

        try {
            // Extract JWT token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
            }
            
            String token = authHeader.substring(7);
            String tenantId = authService.extractTenantIdFromToken(token);
            
            // Get refresh token from X-Refresh-Token header (passed by Next.js API route from cookies)
            String currentRefreshToken = request.getHeader("X-Refresh-Token");
            
            org.springframework.data.domain.Page<com.aidentx.authservice.model.dto.UserSessionResponse> sessions = 
                authService.getAllSessionsPaginated(tenantId, page, size, search, currentRefreshToken);
            
            // Return paginated response
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", sessions.getContent());
            response.put("totalElements", sessions.getTotalElements());
            response.put("totalPages", sessions.getTotalPages());
            response.put("currentPage", sessions.getNumber());
            response.put("pageSize", sessions.getSize());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to retrieve sessions: " + e.getMessage());
        }
    }

    /**
     * Returns the list of configured Identity Providers (IdPs).
     * Useful for dynamically rendering login options on the frontend.
     *
     * @return list of IdP configurations (e.g., Google, Microsoft)
     */
    @GetMapping("/idps")
    public List<IdpConfigProperties.Idp> getIdps() {
        return idpConfig.getIdps();
    }

    /**
     * Returns enabled authentication providers (delegated to service).
     *
     * @return ResponseEntity containing a map of enabled providers
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getAuthProviders() {
        return ResponseEntity.ok(authService.getAuthProviders());
    }

    /**
     * LDAP login endpoint (delegated to service).
     *
     * @param request the login request
     * @return ResponseEntity with login result
     */
    @PostMapping("/ldap/login")
    public ResponseEntity<?> ldapLogin(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.ldapLogin(request));
    }

    /**
     * Initiate SAML login (redirect to SAML URL).
     *
     * @param response the HttpServletResponse for redirect
     * @throws IOException if redirect fails
     */
    @GetMapping("/saml/login")
    public void initiateSamlLogin(HttpServletResponse response) throws IOException {
        Map<String, Object> result = authService.initiateSamlLogin();

        if (result.containsKey("error")) {
            // If there's an error, send a bad request response
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, (String) result.get("error"));
            return;
        }

        // Get the redirect URL and perform the redirect
        String redirectUrl = (String) result.get("redirectUrl");
        if (redirectUrl != null) {
            response.sendRedirect(redirectUrl);
        } else {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, AppConstants.NO_REDIRECT_URL_PROVIDED);
        }
    }

    /**
     * Initiate SAML SSO (delegated to service).
     *
     * @return ResponseEntity with SSO initiation result
     */
    @GetMapping("/saml/sso")
    public ResponseEntity<?> initiateSamlSso() {
        return ResponseEntity.ok(authService.initiateSamlSso());
    }

    /**
     * Handle SAML authentication response (delegated to service).
     *
     * @param authentication the Authentication object
     * @return ResponseEntity with SAML response result
     */
    @PostMapping("/saml/sso")
    public ResponseEntity<?> handleSamlResponse(Authentication authentication) {
        return ResponseEntity.ok(authService.handleSamlResponse(authentication));
    }

    /**
     * Initiate OAuth2 login (delegated to service).
     *
     * @return ResponseEntity with OAuth2 login initiation result
     */
    @GetMapping("/oauth2/login")
    public ResponseEntity<?> initiateOAuth2Login() {
        try {
            Map<String, Object> result = authService.initiateOAuth2Login();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Log the exception and return error response
            Map<String, Object> errorResponse = Map.of(
                "message", e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * OAuth2 callback endpoint for authorization code flow (GET, delegated to service).
     *
     * @param code              the authorization code
     * @param state             the state parameter
     * @param error             error string if present
     * @param error_description error description if present
     * @param success           success flag
     * @param message           message string
     * @param request           HttpServletRequest
     * @param authentication    Authentication object
     * @return ResponseEntity with callback result
     */
    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> handleOAuth2CallbackGet(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String error_description,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) String message,
            HttpServletRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(authService.handleOAuth2CallbackGet(code, state, error, error_description, success, message, authentication));
    }

    /**
     * Handle OAuth2 authentication callback (POST, delegated to service).
     *
     * @param authentication the authentication object containing user details
     * @return a ResponseEntity containing the result of the OAuth2 callback handling
     */
    @PostMapping("/oauth2/callback")
    public ResponseEntity<?> handleOAuth2Callback(Authentication authentication) {
        return ResponseEntity.ok(authService.handleOAuth2Callback(authentication));
    }

    /**
     * Handle successful SAML authentication callback (delegated to service).
     *
     * @param success indicates if the SAML authentication was successful
     * @param error indicates if there was an error during SAML authentication
     * @param message an optional message about the authentication result
     * @return a ResponseEntity containing the login success response with token information
     */
    @GetMapping("/saml/callback")
    public ResponseEntity<LoginSuccessResponse<TokenResponse>> handleSamlCallback(
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) Boolean error,
            @RequestParam(required = false) String message) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(authService.handleSamlCallback(success, error, message, authentication));
    }

    /**
     * Updates the password for the currently authenticated user.
     *
     * @param request        the update password request containing old and new passwords
     * @param authentication the Authentication object (JWT principal)
     * @return a ResponseEntity containing a status/message response indicating success
     */
    @PutMapping("/password")
    public ResponseEntity<StatusMessageResponse> updatePassword(
            @RequestBody @Valid UpdatePasswordRequest request,
            Authentication authentication) {
        // Extract user ID from JWT principal
        UUID userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            // If your UserDetails has a getId() method, use it; otherwise, extract from username/email
            // userId = userDetails.getId();
            // For now, assume username is UUID string
            try {
                userId = UUID.fromString(userDetails.getUsername());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new StatusMessageResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid authentication principal"));
            }
        }
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new StatusMessageResponse(HttpStatus.UNAUTHORIZED.value(), "User not authenticated"));
        }
        StatusMessageResponse response = authService.updatePassword(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiates the password reset process by email.
     * <p>
     * Accepts an email, checks if the user exists, generates a password reset token,
     * and sends an email with a reset link to the user if found.
     *
     * @param request the password reset request containing the user's email
     * @return a status message response (always 200 for security, even if user not found)
     */
    @PostMapping("/password/reset-request")
    public ResponseEntity<StatusMessageResponse> requestPasswordReset(@RequestBody @Valid PasswordResetRequest request) {
        StatusMessageResponse response = authService.requestPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Resets the user's password using a valid password reset token.
     *
     * @param token   the password reset token (from the reset link)
     * @param request the reset password request containing the new password
     * @return a status message response
     */
    @PutMapping("/password/reset/{token}")
    public ResponseEntity<StatusMessageResponse> resetPassword(
            @PathVariable String token,
            @RequestBody @Valid ResetPasswordRequest request) {
        StatusMessageResponse response = authService.resetPassword(token, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping("/session-id")
    public ResponseEntity<String> getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(true); // true creates if not exists
        return ResponseEntity.ok(session.getId());
    }

    /**
     * Gets the current user's profile information.
     *
     * @return a map containing username and displayName
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, String>> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUserProfile());
    }

    /**
     * Terminates a user session by session ID.
     * 
     * <p>This endpoint allows administrators or users to terminate an active session.
     * When terminated:
     * <ul>
     *   <li>The session is deleted from the database</li>
     *   <li>The associated refresh token is revoked</li>
     *   <li>A real-time WebSocket notification is sent to the user if connected</li>
     *   <li>The user will be instantly logged out if currently active</li>
     * </ul>
     * 
     * <p>Authorization: The requesting user must belong to the same tenant as the session.
     *
     * @param sessionId the UUID of the session to terminate
     * @param request   HTTP request to extract JWT token
     * @return HTTP 200 with success message if terminated,
     *         HTTP 401 if not authenticated,
     *         HTTP 403 if unauthorized,
     *         HTTP 404 if session not found
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> terminateSession(
            @PathVariable UUID sessionId,
            HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AppConstants.NOT_AUTHENTICATED);
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AppConstants.INVALID_AUTH_PRINCIPAL);
        }

        try {
            // Extract user ID from JWT principal
            UUID userId = UUID.fromString(userDetails.getUsername());
            
            // Extract JWT token from Authorization header to get tenantId
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
            }
            
            String token = authHeader.substring(7);
            String tenantId = authService.extractTenantIdFromToken(token);
            
            // Call service to terminate the session
            authService.terminateSession(sessionId, userId, tenantId);
            
            return ResponseEntity.ok(Map.of(
                "message", AppConstants.SESSION_TERMINATED_SUCCESS,
                "sessionId", sessionId.toString()
            ));
            
        } catch (IllegalArgumentException e) {
            // Session not found or unauthorized
            if (e.getMessage().contains(AppConstants.SESSION_NOT_FOUND)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", AppConstants.SESSION_NOT_FOUND));
            } else if (e.getMessage().contains(AppConstants.SESSION_UNAUTHORIZED_TERMINATION)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", AppConstants.SESSION_UNAUTHORIZED_TERMINATION));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", AppConstants.SESSION_TERMINATION_FAILED + ": " + e.getMessage()));
        }
    }

}

