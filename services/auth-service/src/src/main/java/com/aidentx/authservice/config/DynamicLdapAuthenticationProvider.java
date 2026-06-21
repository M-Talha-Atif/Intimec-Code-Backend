package com.aidentx.authservice.config;

import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import java.util.Hashtable;
import java.util.Objects;

/**
 * A dynamic AuthenticationProvider for LDAP that can be refreshed at runtime.
 * <p>
 * This provider retrieves LDAP configuration from
 * {@link SsoConfigurationManager}.
 * It caches the underlying {@link LdapAuthenticationProvider} and only
 * recreates it
 * when the LDAP configuration changes, ensuring efficiency while allowing for
 * dynamic updates via Spring Cloud Config. It also supports LDAP referrals.
 * </p>
 */
@Component
@RefreshScope
public class DynamicLdapAuthenticationProvider implements AuthenticationProvider {

    private final IUnifiedLogger logger;
    private final SsoConfigurationManager ssoConfigurationManager;

    private volatile LdapAuthenticationProvider cachedLdapProvider;
    private volatile LdapConfig lastLoadedConfig;
    private final Object lock = new Object();

    /**
     * Constructs the provider with necessary dependencies.
     *
     * @param ssoConfigurationManager The manager to fetch dynamic SSO
     *                                configurations.
     */
    public DynamicLdapAuthenticationProvider(SsoConfigurationManager ssoConfigurationManager,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.ssoConfigurationManager = ssoConfigurationManager;
        this.logger = unifiedLoggerFactory.getLogger("LdapAuthProvider", AppConstants.LDAP_LOGGER_CONTEXT);
    }

    /**
     * Authenticates the user against the currently configured LDAP server.
     * <p>
     * It dynamically fetches the LDAP configuration and uses a cached provider
     * if the configuration has not changed. If LDAP is not enabled, it returns
     * null,
     * allowing the authentication manager to try the next provider.
     * </p>
     *
     * @param authentication The authentication request object.
     * @return A fully authenticated object including authorities if authentication
     *         is successful.
     * @throws AuthenticationException if authentication fails.
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.trace()
                .message(AppConstants.MSG_AUTH_ATTEMPT)
                .objectId(authentication.getName()).log();

        LdapConfig currentLdapConfig = ssoConfigurationManager.getLdapConfig();
        if (currentLdapConfig == null || !currentLdapConfig.isEnabled()) {
            logger.debug()
                    .message(AppConstants.MSG_LDAP_NOT_ENABLED)
                    .log();
            return null; // Let other providers handle it
        }

        LdapAuthenticationProvider provider = getLdapProvider(currentLdapConfig);
        logger.debug()
                .message(AppConstants.MSG_LDAP_AUTH)
                .objectId(authentication.getName()).log();
        return provider.authenticate(authentication);
    }

    /**
     * Retrieves a cached {@link LdapAuthenticationProvider} or creates a new one if
     * the configuration has changed.
     * <p>
     * This method is thread-safe and uses double-checked locking to ensure that the
     * provider
     * is only created once when needed.
     * </p>
     *
     * @param ldapConfig The current LDAP configuration.
     * @return A valid LdapAuthenticationProvider instance.
     */
    private LdapAuthenticationProvider getLdapProvider(LdapConfig ldapConfig) {
        if (cachedLdapProvider == null || !Objects.equals(lastLoadedConfig, ldapConfig)) {
            synchronized (lock) {
                if (cachedLdapProvider == null || !Objects.equals(lastLoadedConfig, ldapConfig)) {
                    logger.info()
                            .message(AppConstants.MSG_LDAP_CONFIG_CHANGED)
                            .log();
                    cachedLdapProvider = createLdapProvider(ldapConfig);
                    lastLoadedConfig = ldapConfig;
                }
            }
        }
        return cachedLdapProvider;
    }

    /**
     * Creates a new instance of LdapAuthenticationProvider based on the given
     * configuration.
     * <p>
     * This version includes the fix for handling LDAP referrals by setting the
     * {@code Context.REFERRAL} property to "follow".
     * </p>
     *
     * @param ldapConfig The configuration to use for the new provider.
     * @return A configured LdapAuthenticationProvider.
     * @throws RuntimeException if the provider cannot be created.
     */
    private LdapAuthenticationProvider createLdapProvider(LdapConfig ldapConfig) {
        logger.trace()
                .message(AppConstants.MSG_CREATING_LDAP_PROVIDER)
                .detail("url", ldapConfig.getUrl())
                .detail("baseDn", ldapConfig.getBaseDn())
                .detail("userSearchFilter", ldapConfig.getUserSearchFilter()).log();
        try {
            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(
                    ldapConfig.getUrl());
            contextSource.setBase(ldapConfig.getBaseDn());

            if (ldapConfig.getBindDn() != null && !ldapConfig.getBindDn().isBlank()
                    && ldapConfig.getBindPassword() != null) {
                logger.trace()
                        .message(AppConstants.MSG_USING_BIND_DN)
                        .detail("bindDn", ldapConfig.getBindDn()).log();
                contextSource.setUserDn(ldapConfig.getBindDn());
                contextSource.setPassword(ldapConfig.getBindPassword());
            } else {
                logger.trace()
                        .message(AppConstants.MSG_NO_BIND_DN)
                        .log();
            }

            Hashtable<String, Object> env = new Hashtable<>();
            env.put("com.sun.jndi.ldap.connect.timeout", "5000");
            env.put("com.sun.jndi.ldap.read.timeout", "5000");
            env.put(Context.REFERRAL, "follow");

            contextSource.setBaseEnvironmentProperties(env);
            contextSource.afterPropertiesSet();

            FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(
                    "",
                    ldapConfig.getUserSearchFilter(),
                    contextSource);
            userSearch.setSearchSubtree(true);

            BindAuthenticator authenticator = new BindAuthenticator(contextSource);
            authenticator.setUserSearch(userSearch);
            authenticator.afterPropertiesSet();

            LdapAuthoritiesPopulator authoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, "");

            LdapAuthenticationProvider provider = new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
            logger.info()
                    .message(AppConstants.MSG_LDAP_PROVIDER_SUCCESS)
                    .log();
            return provider;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.MSG_FATAL_LDAP_CREATION);
            throw new RuntimeException(AppConstants.EXC_MSG_LDAP_PROVIDER_CREATION, e);
        }
    }

    /**
     * Specifies that this provider supports
     * {@link UsernamePasswordAuthenticationToken}.
     *
     * @param authentication The authentication class to check.
     * @return True if the authentication is a UsernamePasswordAuthenticationToken,
     *         false otherwise.
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    /**
     * Checks if LDAP is currently enabled in the configuration.
     *
     * @return True if LDAP is enabled, false otherwise.
     */
    public boolean isLdapEnabled() {
        try {
            LdapConfig ldapConfig = ssoConfigurationManager.getLdapConfig();
            return ldapConfig != null && ldapConfig.isEnabled();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.MSG_FAILED_CHECK_LDAP_STATUS).log();
            return false;
        }
    }

    /**
     * Retrieves the current LDAP configuration.
     *
     * @return The current LdapConfig object, or null if an error occurs.
     */
    public LdapConfig getLdapConfig() {
        try {
            return ssoConfigurationManager.getLdapConfig();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.MSG_FAILED_GET_LDAP_CONFIG).log();
            return null;
        }
    }
}
