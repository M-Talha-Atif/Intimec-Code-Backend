package com.aidentx.authservice.config;

import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static com.aidentx.authservice.config.AppConstants.*;

/**
 * Spring configuration class for integrating OAuth2 authentication.
 * <p>
 * Provides a dynamic configuration of OAuth2 clients based on external SSO
 * settings
 * managed through {@link SsoConfigurationManager}. If OAuth2 is disabled or
 * invalid,
 * a no-op {@link ClientRegistrationRepository} is registered to allow the app
 * to start gracefully.
 *
 * <p>
 * Key features:
 * <ul>
 * <li>Validation of OAuth2 configuration fields</li>
 * <li>Construction of a {@link ClientRegistration} instance</li>
 * <li>Support for JWK Set URI for OIDC</li>
 * </ul>
 */
@Configuration
public class OAuth2AuthenticationConfig {

    private final IUnifiedLogger logger;
    private final SsoConfigurationManager ssoConfigurationManager;

    /**
     * Constructor for dependency injection.
     *
     * @param ssoConfigurationManager The manager for fetching SSO configurations.
     */
    public OAuth2AuthenticationConfig(SsoConfigurationManager ssoConfigurationManager,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.ssoConfigurationManager = ssoConfigurationManager;
        this.logger = unifiedLoggerFactory.getLogger("Oauth2AuthConfig", OAUTH2_AUTH_CONFIG_LOGGER_CATEGORY);
    }

    /**
     * Bean for the {@link ClientRegistrationRepository}.
     * <p>
     * If OAuth2 is enabled and properly configured, returns a repository
     * containing a single {@link ClientRegistration} instance.
     * Otherwise, returns a no-op implementation.
     *
     * @return a {@link ClientRegistrationRepository}
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        try {
            OAuth2Config oauth2Config = ssoConfigurationManager.getOAuth2Config();
            if (oauth2Config == null || !oauth2Config.isEnabled()) {
                logger.info().message(OAUTH2_DISABLED_OR_NOT_CONFIGURED).log();
                return new EmptyClientRegistrationRepository();
            }

            if (!isValidOAuth2Config(oauth2Config)) {
                logger.warn()

                        .message(OAUTH2_CONFIG_INVALID_OR_INCOMPLETE)
                        .log();
                return new EmptyClientRegistrationRepository();
            }

            ClientRegistration registration = createClientRegistration(oauth2Config);
            logger.info()
                    .message(LOG_OAUTH2_CLIENT_REGISTERED)
                    .objectId(oauth2Config.getProviderId())
                    .log();
            return new InMemoryClientRegistrationRepository(registration);

        } catch (Exception e) {
            logger.error(e).message(OAUTH2_CONFIG_FAILED).log();
            return new EmptyClientRegistrationRepository();
        }
    }

    /**
     * Validates the given {@link OAuth2Config} object.
     * Ensures that all required fields are non-null and non-blank.
     *
     * @param oauth2Config the configuration object to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidOAuth2Config(OAuth2Config oauth2Config) {
        if (oauth2Config == null) {
            return false;
        }

        return hasText(oauth2Config.getClientId()) &&
                hasText(oauth2Config.getClientSecret()) &&
                hasText(oauth2Config.getProviderId()) &&
                hasText(oauth2Config.getRedirectUri()) &&
                hasText(oauth2Config.getAuthorizationUri()) &&
                hasText(oauth2Config.getTokenUri()) &&
                hasText(oauth2Config.getUserInfoUri());
    }

    /**
     * Utility method to check if a string is non-null and contains non-whitespace
     * characters.
     *
     * @param str the input string
     * @return true if the string has text, false otherwise
     */
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Constructs a {@link ClientRegistration} object using the provided
     * {@link OAuth2Config}.
     *
     * @param oauth2Config the OAuth2 configuration to use
     * @return a configured {@link ClientRegistration}
     * @throws RuntimeException if registration creation fails
     */
    private ClientRegistration createClientRegistration(OAuth2Config oauth2Config) {
        try {
            ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(oauth2Config.getProviderId())
                    .clientId(oauth2Config.getClientId())
                    .clientSecret(oauth2Config.getClientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(oauth2Config.getRedirectUri())
                    .scope(oauth2Config.getScopes())
                    .authorizationUri(oauth2Config.getAuthorizationUri())
                    .tokenUri(oauth2Config.getTokenUri())
                    .userInfoUri(oauth2Config.getUserInfoUri())
                    .userNameAttributeName(oauth2Config.getUserNameAttribute())
                    .clientName(oauth2Config.getName());

            if (hasText(oauth2Config.getJwkSetUri())) {
                builder.jwkSetUri(oauth2Config.getJwkSetUri());
                logger.info()
                        .message(LOG_OAUTH2_JWK_SET_URI_CONFIGURED)
                        .objectId(oauth2Config.getProviderId())
                        .detail(LOG_DETAIL_JWK_SET_URI, oauth2Config.getJwkSetUri())
                        .log();
            } else {
                logger.warn()
                        .message(LOG_OAUTH2_JWK_SET_URI_EMPTY)
                        .objectId(oauth2Config.getProviderId())
                        .log();
            }

            logger.debug()
                    .message(LOG_OAUTH2_BUILDING_REGISTRATION)
                    .objectId(oauth2Config.getProviderId())
                    .detail(LOG_DETAIL_CLIENT_NAME, oauth2Config.getName())
                    .detail(LOG_DETAIL_CLIENT_ID, oauth2Config.getClientId())
                    .detail(LOG_DETAIL_REDIRECT_URI, oauth2Config.getRedirectUri())
                    .log();

            return builder.build();

        } catch (Exception e) {
            logger.error(e).message(String.format(OAUTH2_CLIENT_REGISTRATION_FAILED, oauth2Config.getProviderId())).log();
            throw new RuntimeException(OAUTH2_CLIENT_REGISTRATION_FAILED_MSG, e);
        }
    }

    /**
     * No-op implementation of {@link ClientRegistrationRepository} used when OAuth2
     * is disabled or misconfigured.
     * Always returns null for any registration ID to avoid breaking the app
     * context.
     */
    private static class EmptyClientRegistrationRepository implements ClientRegistrationRepository {
        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return null;
        }
    }
}