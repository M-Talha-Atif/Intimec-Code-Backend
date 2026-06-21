package com.aidentx.authservice.config;

import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic OAuth2 Client Registration Repository that supports runtime
 * configuration updates.
 *
 * This repository fetches OAuth2 client registrations from the
 * SsoConfigurationManager
 * and can be refreshed at runtime using Spring Cloud's @RefreshScope.
 *
 * @author aidentx
 * @since 1.0
 */
@Component
@RefreshScope
public class DynamicOAuth2ClientRegistrationRepository implements ClientRegistrationRepository {

    private final SsoConfigurationManager ssoConfigurationManager;
    private final IUnifiedLogger logger;

    /**
     * Constructs the repository with necessary dependencies.
     *
     * @param ssoConfigurationManager The manager for fetching SSO configurations.
     */
    public DynamicOAuth2ClientRegistrationRepository(SsoConfigurationManager ssoConfigurationManager,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.ssoConfigurationManager = ssoConfigurationManager;
        this.logger = unifiedLoggerFactory.getLogger("OAuth2ClientRegistrationRepo", AppConstants.OAUTH2_LOGGER_CATEGORY);
    }

    /**
     * Finds a {@link ClientRegistration} by its registration ID.
     * <p>
     * Attempts to fetch the current OAuth2 configuration from the SSO configuration
     * manager and map it to a Spring Security
     * {@link ClientRegistration}. If the configuration is not found or disabled,
     * logs a debug message and returns null.
     * All errors are logged and result in a null return value.
     *
     * @param registrationId the registration ID to look up
     * @return the corresponding ClientRegistration, or null if not found or invalid
     */
    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        try {
            OAuth2Config oauth2Config = ssoConfigurationManager.getOAuth2Config();
            if (oauth2Config != null && oauth2Config.isEnabled()) {
                // Ensure the requested ID matches the configured provider ID
                if (registrationId.equals(oauth2Config.getProviderId())) {
                    return mapToClientRegistration(oauth2Config);
                }
            }
            logger.debug()
                    .message(AppConstants.LOG_OAUTH2_CONFIG_NOT_FOUND)
                    .detail(AppConstants.LOG_DETAIL_REGISTRATION_ID, registrationId)
                    .log();
            return null;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_OAUTH2_FIND_REGISTRATION_FAILED + registrationId).log();
            return null;
        }
    }

    /**
     * Maps an {@link OAuth2Config} to a Spring Security {@link ClientRegistration}.
     * <p>
     * Validates the configuration before mapping. If invalid, logs a warning and
     * returns null.
     *
     * @param oauth2Config the OAuth2 configuration to map
     * @return a valid ClientRegistration, or null if the configuration is invalid
     */
    private ClientRegistration mapToClientRegistration(OAuth2Config oauth2Config) {
        if (oauth2Config == null) {
            return null;
        }

        // Validate required fields
        if (!isValidOAuth2Config(oauth2Config)) {
            logger.warn()
                    .message(AppConstants.LOG_OAUTH2_CONFIG_INVALID)
                    .objectId(oauth2Config.getProviderId())
                    .log();
            return null;
        }

        ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(
                oauth2Config.getProviderId());

        // Set required authorization grant type and client authentication method
        builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);

        // Set basic properties
        builder.clientId(oauth2Config.getClientId())
                .clientSecret(oauth2Config.getClientSecret())
                .clientName(oauth2Config.getName() != null ? oauth2Config.getName() : AppConstants.OAUTH2_DEFAULT_CLIENT_NAME);

        // Set URIs
        builder.authorizationUri(oauth2Config.getAuthorizationUri());
        builder.tokenUri(oauth2Config.getTokenUri());
        builder.userInfoUri(oauth2Config.getUserInfoUri());
        builder.redirectUri(oauth2Config.getRedirectUri());

        // Set JWK Set URI if available
        if (hasText(oauth2Config.getJwkSetUri())) {
            builder.jwkSetUri(oauth2Config.getJwkSetUri());
        }

        // Set scopes
        List<String> scopes = oauth2Config.getScopes();
        if (scopes != null && !scopes.isEmpty()) {
            builder.scope(scopes.toArray(new String[0]));
        } else {
            // Default scopes for OAuth2/OIDC
            builder.scope(AppConstants.OAUTH2_SCOPE_OPENID, AppConstants.OAUTH2_SCOPE_PROFILE, AppConstants.OAUTH2_SCOPE_EMAIL);
        }

        // Set user name attribute
        if (hasText(oauth2Config.getUserNameAttribute())) {
            builder.userNameAttributeName(oauth2Config.getUserNameAttribute());
        } else {
            builder.userNameAttributeName(AppConstants.OAUTH2_DEFAULT_USERNAME_ATTRIBUTE); // Default for OIDC
        }

        return builder.build();
    }

    /**
     * Validates that all required OAuth2 configuration fields are present and
     * non-empty.
     *
     * @param oauth2Config the OAuth2 configuration to validate
     * @return true if the configuration is valid, false otherwise
     */
    private boolean isValidOAuth2Config(OAuth2Config oauth2Config) {
        if (oauth2Config == null) {
            return false;
        }

        // Check required fields
        return hasText(oauth2Config.getClientId()) &&
                hasText(oauth2Config.getClientSecret()) &&
                hasText(oauth2Config.getProviderId()) &&
                hasText(oauth2Config.getRedirectUri()) &&
                hasText(oauth2Config.getAuthorizationUri()) &&
                hasText(oauth2Config.getTokenUri()) &&
                hasText(oauth2Config.getUserInfoUri());
    }

    /**
     * Checks if a string is non-null and contains non-whitespace text.
     *
     * @param str the string to check
     * @return true if the string has text, false otherwise
     */
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Retrieves all available {@link ClientRegistration} objects from the current
     * OAuth2 configuration.
     * <p>
     * Only enabled and valid configurations are included. Errors are logged and
     * result in an empty list.
     *
     * @return a list of all available ClientRegistrations
     */
    public List<ClientRegistration> getAllRegistrations() {
        List<ClientRegistration> registrations = new ArrayList<>();
        try {
            OAuth2Config oauth2Config = ssoConfigurationManager.getOAuth2Config();
            if (oauth2Config != null && oauth2Config.isEnabled()) {
                ClientRegistration registration = mapToClientRegistration(oauth2Config);
                if (registration != null) {
                    registrations.add(registration);
                }
            }
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_OAUTH2_GET_ALL_REGISTRATIONS_FAILED).log();
        }
        return registrations;
    }

    /**
     * Checks if OAuth2 is enabled and properly configured in the SSO configuration
     * manager.
     *
     * @return true if OAuth2 is enabled and valid, false otherwise
     */
    public boolean isOAuth2Enabled() {
        try {
            OAuth2Config oauth2Config = ssoConfigurationManager.getOAuth2Config();
            return oauth2Config != null && oauth2Config.isEnabled();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_OAUTH2_STATUS_CHECK_FAILED).log();
            return false;
        }
    }
}