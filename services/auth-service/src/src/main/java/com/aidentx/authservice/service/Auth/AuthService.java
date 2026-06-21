package com.aidentx.authservice.service.Auth;

import com.aidentx.authservice.exception.*;
import com.aidentx.authservice.model.dto.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for handling authentication-related operations.
 */
public interface AuthService {

    /**
     * Authenticates a user based on the provided login credentials.
     *
     * @param request the login request containing username/email and password
     * @return a {@link LoginSuccessResponse} containing a {@link TokenResponse} with
     * the JWT token and its expiration details if authentication is successful
     * @throws InvalidCredentialsException if the credentials are invalid
     * @throws AccountLockedException      if the user account is locked
     * @throws UserNotFoundException       if no user is found with the given credentials
     */
    LoginSuccessResponse<TokenResponse> login(LoginRequest request);

    /**
     * Generates a new access token using a valid refresh token.
     * <p>
     * Validates the provided refresh token, and if valid and not expired or revoked,
     * issues a new JWT access token. Optionally, this method may issue a new refresh token
     * as part of token rotation.
     * </p>
     *
     * @param request the refresh token request containing the refresh token string
     * @return a {@link LoginSuccessResponse} containing a {@link TokenResponse} with
     * the new access token, optionally a new refresh token, and its expiration details
     * @throws InvalidRefreshTokenException if the refresh token is invalid, expired, or revoked
     */
    LoginSuccessResponse<TokenResponse> refreshAccessToken(RefreshTokenRequest request);

    /**
     * Logs out a user by invalidating their session(s).
     * If refreshToken is provided, only that specific session is deleted.
     * If refreshToken is null, all user sessions are deleted.
     *
     * @param userId the UUID of the user to log out
     * @param refreshToken the refresh token to invalidate (optional)
     */
    void logout(UUID userId, String refreshToken);

    /**
     * Validates if a session exists in the system.
     *
     * @param sessionId the session ID to validate
     * @return true if session exists and is active, false otherwise
     */
    boolean isSessionValid(UUID sessionId);

    /**
     * Retrieves all active sessions for the authenticated user's tenant.
     * Each session includes user details (userId, username, email).
     *
     * @param tenantId the tenant ID from the JWT token
     * @return list of session responses with user details
     */
    List<com.aidentx.authservice.model.dto.UserSessionResponse> getAllSessions(String tenantId);

    /**
     * Retrieves all active sessions for the authenticated user's tenant with pagination.
     * Each session includes user details (userId, username, email).
     *
     * @param tenantId the tenant ID from the JWT token
     * @param page the page number (0-based)
     * @param size the page size
     * @return paginated session responses with user details
     */
    org.springframework.data.domain.Page<com.aidentx.authservice.model.dto.UserSessionResponse> getAllSessionsPaginated(String tenantId, int page, int size, String search, String currentRefreshToken);

    /**
     * Extracts the tenant ID from a JWT token.
     *
     * @param token the JWT token
     * @return the tenant ID
     */
    String extractTenantIdFromToken(String token);

    /**
     * Extracts the user ID from a JWT token.
     *
     * @param token the JWT token
     * @return the user ID
     */
    String extractUserIdFromToken(String token);

    /**
     * Retrieves the list of enabled authentication providers (LDAP, SAML, OAuth2).
     *
     * @return a map containing provider details and enabled status for each provider
     */
    Map<String, Object> getAuthProviders();

    /**
     * Authenticates a user against the configured LDAP server.
     *
     * @param request the login request containing username and password
     * @return a LoginSuccessResponse with authentication result and user tokens
     */
    LoginSuccessResponse<TokenResponse> ldapLogin(LoginRequest request);

    /**
     * Initiates SAML login by providing redirect information.
     *
     * @return a map with SAML redirect information
     */
    Map<String, Object> initiateSamlLogin();

    /**
     * Initiates SAML SSO by providing redirect information.
     *
     * @return a map with SAML SSO redirect information
     */
    Map<String, Object> initiateSamlSso();

    /**
     * Handles SAML authentication response.
     *
     * @param authentication the SAML authentication object
     * @return a {@link LoginSuccessResponse} containing a {@link TokenResponse} with
     * the JWT token and its expiration details if authentication is successful
     */
    LoginSuccessResponse<TokenResponse> handleSamlResponse(Authentication authentication);

    /**
     * Initiates OAuth2 login by providing redirect information.
     *
     * @return a map with OAuth2 redirect information
     */
    Map<String, Object> initiateOAuth2Login();

    /**
     * Handles OAuth2 callback for authorization code flow (GET).
     *
     * @param code             the authorization code
     * @param state            the state parameter
     * @param error            the error parameter
     * @param errorDescription the error description
     * @param success          the success flag
     * @param message          the message
     * @param authentication   the authentication object
     * @return a response object: either LoginSuccessResponse<TokenResponse> or error map
     */
    Object handleOAuth2CallbackGet(String code, String state, String error, String errorDescription, Boolean success, String message, Authentication authentication);

    /**
     * Handles OAuth2 authentication callback (POST).
     *
     * @param authentication the OAuth2 authentication object
     * @return a map with authentication result and user details
     */
    Map<String, Object> handleOAuth2Callback(Authentication authentication);

    /**
     * Handles SAML authentication callback.
     *
     * @param success        success parameter from the redirect
     * @param error          error parameter from the redirect
     * @param message        error message parameter from the redirect
     * @param authentication the authentication object
     * @return a {@link LoginSuccessResponse} containing a {@link TokenResponse} with
     * the JWT token and its expiration details if authentication is successful
     */
    LoginSuccessResponse<TokenResponse> handleSamlCallback(Boolean success, Boolean error, String message, Authentication authentication);

    /**
     * Updates the password for the currently authenticated user.
     * <p>
     * The user must be authenticated (JWT). The method checks if the user exists,
     * verifies the old password, and updates to the new password if valid.
     * Throws InvalidOldPasswordException if the old password does not match.
     * </p>
     *
     * @param request the update password request containing old and new passwords
     * @param userId  the ID of the authenticated user (from JWT)
     * @return a status/message response indicating success
     * @throws InvalidOldPasswordException if the old password is incorrect
     * @throws UserNotFoundException       if the user does not exist
     */
    StatusMessageResponse updatePassword(UpdatePasswordRequest request, UUID userId);

    /**
     * Initiates the password reset process for the given email.
     * <p>
     * If the user exists, generates a password reset token and sends an email with a reset link.
     * Always returns a generic success message for security.
     *
     * @param request the password reset request containing the user's email
     * @return a status message response
     */
    StatusMessageResponse requestPasswordReset(PasswordResetRequest request);

    /**
     * Resets the user's password using a valid password reset token.
     *
     * @param token   the password reset token
     * @param request the reset password request containing the new password
     * @return a status message response
     */
    StatusMessageResponse resetPassword(String token, ResetPasswordRequest request);

    /**
     * Gets the current authenticated user's profile information.
     *
     * @return a map containing username and displayName
     */
    Map<String, String> getCurrentUserProfile();

    /**
     * Terminates a user session by session ID.
     * 
     * <p>This method validates that the session exists and belongs to the same tenant
     * as the requesting user. It then deletes the session, associated refresh token,
     * and sends a real-time WebSocket notification to the user if they are connected.
     * 
     * <p>The user will be instantly logged out if they are currently connected.
     *
     * @param sessionId the UUID of the session to terminate
     * @param userId    the UUID of the requesting user (from JWT)
     * @param tenantId  the tenant ID of the requesting user (from JWT)
     * @throws IllegalArgumentException if session not found or unauthorized
     */
    void terminateSession(UUID sessionId, UUID userId, String tenantId);
}