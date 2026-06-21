package com.aidentx.authservice.config;

import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;

/**
 * Spring configuration for LDAP authentication integration.
 * <p>
 * This class provides all the necessary beans required to perform
 * authentication
 * against an LDAP server. It includes the configuration for context source,
 * user search,
 * bind authenticator, authorities populator, and a main authentication
 * provider.
 * </p>
 * <p>
 * If LDAP is not enabled or misconfigured, dummy beans are created to prevent
 * the app
 * from failing at startup while effectively disabling LDAP authentication.
 * </p>
 */
@Configuration
public class LdapAuthenticationConfig {

    private final IUnifiedLogger logger;
    private final SsoConfigurationManager ssoConfigurationManager;

    /**
     * Constructor for dependency injection.
     *
     * @param ssoConfigurationManager The manager for fetching SSO configurations.
     */
    public LdapAuthenticationConfig(SsoConfigurationManager ssoConfigurationManager,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.ssoConfigurationManager = ssoConfigurationManager;
        this.logger = unifiedLoggerFactory.getLogger("LdapAuthConfig", "config-ldap");
    }

    /**
     * Creates the LDAP context source bean.
     *
     * @return a configured {@link DefaultSpringSecurityContextSource}, or a dummy
     *         context source if config is invalid
     */
    @Bean
    public DefaultSpringSecurityContextSource contextSource() {
        try {
            LdapConfig ldapConfig = ssoConfigurationManager.getLdapConfig();
            if (ldapConfig == null || !ldapConfig.isEnabled()) {
                logger.info().message(AppConstants.LDAP_DISABLED_OR_NOT_CONFIGURED).log();
                return new DefaultSpringSecurityContextSource("ldap://localhost:389");
            }

            if (!isValidLdapConfig(ldapConfig)) {
                logger.warn()
                        .message(AppConstants.LDAP_CONFIG_INVALID_OR_INCOMPLETE)
                        .log();
                return new DefaultSpringSecurityContextSource("ldap://localhost:389");
            }

            DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(
                    ldapConfig.getUrl());
            contextSource.setUserDn(ldapConfig.getBindDn());
            contextSource.setPassword(ldapConfig.getBindPassword());
            contextSource.setAnonymousReadOnly(false);
            contextSource.setPooled(false);

            java.util.Hashtable<String, Object> baseEnv = new java.util.Hashtable<>();
            baseEnv.put("java.naming.ldap.version", "3");
            baseEnv.put("java.naming.security.authentication", "simple");
            contextSource.setBaseEnvironmentProperties(baseEnv);

            return contextSource;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LDAP_CONTEXT_SOURCE_FAILED).log();
            return new DefaultSpringSecurityContextSource("ldap://localhost:389");
        }
    }

    /**
     * Creates a {@link BindAuthenticator} bean.
     *
     * @return a configured {@link BindAuthenticator}, or a dummy one if config is
     *         invalid
     */
    @Bean
    public BindAuthenticator bindAuthenticator() {
        try {
            LdapConfig ldapConfig = ssoConfigurationManager.getLdapConfig();
            if (ldapConfig == null || !ldapConfig.isEnabled()) {
                logger.info().message(AppConstants.LDAP_DISABLED_OR_NOT_CONFIGURED_BIND_AUTH).log();
                BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource());
                bindAuthenticator
                        .setUserSearch(new FilterBasedLdapUserSearch("ou=people", "(uid={0})", contextSource()));
                return bindAuthenticator;
            }

            if (!isValidLdapConfig(ldapConfig)) {
                logger.warn().message(AppConstants.LDAP_CONFIG_INVALID_OR_INCOMPLETE_BIND_AUTH).log();
                BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource());
                bindAuthenticator
                        .setUserSearch(new FilterBasedLdapUserSearch("ou=people", "(uid={0})", contextSource()));
                return bindAuthenticator;
            }

            BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource());
            bindAuthenticator.setUserSearch(ldapUserSearch());
            return bindAuthenticator;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LDAP_BIND_AUTH_FAILED).log();
            BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource());
            bindAuthenticator.setUserSearch(new FilterBasedLdapUserSearch("ou=people", "(uid={0})", contextSource()));
            return bindAuthenticator;
        }
    }

    /**
     * Creates the {@link LdapUserSearch} bean used for finding users.
     *
     * @return a configured {@link FilterBasedLdapUserSearch}
     */
    @Bean
    public LdapUserSearch ldapUserSearch() {
        try {
            LdapConfig ldapConfig = ssoConfigurationManager.getLdapConfig();
            if (ldapConfig == null || !ldapConfig.isEnabled()) {
                logger.info().message(AppConstants.LDAP_DISABLED_OR_NOT_CONFIGURED_USER_SEARCH).log();
                return new FilterBasedLdapUserSearch("ou=people", "(uid={0})", contextSource());
            }

            if (!isValidLdapConfig(ldapConfig)) {
                logger.warn().message(AppConstants.LDAP_CONFIG_INVALID_OR_INCOMPLETE_USER_SEARCH).log();
                return new FilterBasedLdapUserSearch("ou=people", "(uid={0})", contextSource());
            }

            return new FilterBasedLdapUserSearch(
                    ldapConfig.getBaseDn(),
                    ldapConfig.getUserSearchFilter(),
                    contextSource());
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LDAP_USER_SEARCH_FAILED).log();
            return new FilterBasedLdapUserSearch("ou=people", "(uid={0})", contextSource());
        }
    }

    /**
     * Creates the {@link DefaultLdapAuthoritiesPopulator} bean.
     *
     * @return a configured authorities populator that maps LDAP groups to roles
     */
    @Bean
    public DefaultLdapAuthoritiesPopulator authoritiesPopulator() {
        try {
            LdapConfig ldapConfig = ssoConfigurationManager.getLdapConfig();
            if (ldapConfig == null || !ldapConfig.isEnabled()) {
                logger.info().message(AppConstants.LDAP_DISABLED_OR_NOT_CONFIGURED_AUTHORITIES).log();
                return new DefaultLdapAuthoritiesPopulator(contextSource(), "ou=groups");
            }

            if (!isValidLdapConfig(ldapConfig)) {
                logger.warn().message(AppConstants.LDAP_CONFIG_INVALID_OR_INCOMPLETE_AUTHORITIES).log();
                return new DefaultLdapAuthoritiesPopulator(contextSource(), "ou=groups");
            }

            // Using empty group search base to indicate searching from the context source's
            // base DN
            DefaultLdapAuthoritiesPopulator authoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource(),
                    "");
            authoritiesPopulator.setIgnorePartialResultException(true);
            return authoritiesPopulator;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LDAP_AUTHORITIES_POPULATOR_FAILED).log();
            return new DefaultLdapAuthoritiesPopulator(contextSource(), "ou=groups");
        }
    }

    /**
     * Creates the LDAP {@link AuthenticationProvider} bean for Spring Security.
     *
     * @return a configured {@link LdapAuthenticationProvider}, or a no-op
     *         implementation if LDAP is disabled
     */
    @Bean
    public AuthenticationProvider ldapAuthenticationProvider() {
        try {
            LdapConfig ldapConfig = ssoConfigurationManager.getLdapConfig();
            if (ldapConfig == null || !ldapConfig.isEnabled()) {
                logger.info().message(AppConstants.LDAP_DISABLED_OR_NOT_CONFIGURED_AUTH_PROVIDER).log();
                return new EmptyAuthenticationProvider();
            }

            if (!isValidLdapConfig(ldapConfig)) {
                logger.warn().message(AppConstants.LDAP_CONFIG_INVALID_OR_INCOMPLETE_AUTH_PROVIDER).log();
                return new EmptyAuthenticationProvider();
            }

            LdapAuthenticationProvider provider = new LdapAuthenticationProvider(bindAuthenticator(),
                    authoritiesPopulator());
            provider.setUserDetailsContextMapper(new LdapUserDetailsMapper());
            return provider;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LDAP_AUTH_PROVIDER_FAILED).log();
            return new EmptyAuthenticationProvider();
        }
    }

    /**
     * Validates whether the given LDAP configuration contains all required fields.
     *
     * @param ldapConfig the LDAP configuration to check
     * @return true if all required fields are present, false otherwise
     */
    private boolean isValidLdapConfig(LdapConfig ldapConfig) {
        if (ldapConfig == null) {
            return false;
        }

        return hasText(ldapConfig.getUrl()) &&
                hasText(ldapConfig.getBaseDn()) &&
                hasText(ldapConfig.getUserSearchFilter());
    }

    /**
     * Checks if a string is non-null and non-empty after trimming.
     *
     * @param str the string to check
     * @return true if the string is non-empty, false otherwise
     */
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * A no-op {@link AuthenticationProvider} implementation used when LDAP is not
     * active.
     * Always returns null for authentication and supports no types.
     */
    private static class EmptyAuthenticationProvider implements AuthenticationProvider {
        @Override
        public Authentication authenticate(Authentication authentication) throws AuthenticationException {
            return null; // This provider does nothing, allowing the chain to continue
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return false; // Does not support any authentication type
        }
    }
}