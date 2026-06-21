package com.aidentx.authservice.config;

import com.aidentx.authservice.security.JwtAuthenticationFilter;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security configuration class that sets up HTTP security for the
 * application.
 * <p>
 * This configuration:
 * <ul>
 * <li>Disables CSRF for stateless APIs</li>
 * <li>Supports JWT, LDAP, SAML, and OAuth2 authentication mechanisms</li>
 * <li>Uses custom {@link AuthenticationProvider} to replace deprecated
 * DaoAuthenticationProvider</li>
 * <li>Integrates a custom JWT filter</li>
 * <li>Supports dynamic SSO configuration updates via Spring Cloud</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final IUnifiedLogger logger;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DynamicOAuth2ClientRegistrationRepository dynamicOAuth2Repository;
    private final DynamicSamlRelyingPartyRegistrationRepository dynamicSamlRepository;
    private final DynamicLdapAuthenticationProvider dynamicLdapProvider;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor-based injection for all security dependencies.
     *
     * @param jwtAuthenticationFilter custom filter that validates JWT tokens
     * @param dynamicOAuth2Repository repository for dynamic OAuth2 client
     *                                configurations
     * @param dynamicSamlRepository   repository for dynamic SAML relying party
     *                                configurations
     * @param dynamicLdapProvider     provider for dynamic LDAP authentication
     * @param userDetailsService      service for loading user details from the
     *                                database
     * @param passwordEncoder         encoder for password hashing and verification
     * @param loggerFactory           factory to create a logger for this class
     */
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            DynamicOAuth2ClientRegistrationRepository dynamicOAuth2Repository,
            DynamicSamlRelyingPartyRegistrationRepository dynamicSamlRepository,
            DynamicLdapAuthenticationProvider dynamicLdapProvider,
            @Qualifier("customUserDetailsService") UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.dynamicOAuth2Repository = dynamicOAuth2Repository;
        this.dynamicSamlRepository = dynamicSamlRepository;
        this.dynamicLdapProvider = dynamicLdapProvider;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.logger = unifiedLoggerFactory.getLogger("SecurityConfig", "config-security");
    }

    /**
     * Configures the HTTP security filter chain.
     *
     * @param http the {@link HttpSecurity} instance
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception in case of configuration errors
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> {
                    boolean samlEnabled = isSamlEnabled();
                    boolean oauth2Enabled = isOAuth2Enabled();

                    if (oauth2Enabled || samlEnabled) {
                        logger.info()
                                .message("Configuring session management for SSO")
                                .detail("samlEnabled", samlEnabled)
                                .detail("oauth2Enabled", oauth2Enabled)
                                .log();
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
                    } else {
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                    }
                })
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login/**",
                                "/api/v1/auth/password/reset-request/**",
                                "/api/v1/auth/password/reset/**",
                                "/api/v1/auth/refresh/**",
                                "/api/v1/auth/providers/**",
                                "/api/v1/auth/ldap/**",
                                "/api/v1/logs/**",
                                "/api/v1/auth/saml/**",
                                "/api/v1/auth/oauth2/**",
                                "/api/v1/auth/session-id/**",
                                "/api/v1/auth/ws/**", // WebSocket endpoint with SockJS
                                "/api/v1/users/by-identity/**", // Inter-service endpoint for user lookup
                                "/api/v1/users/*", // Inter-service endpoint for user lookup by ID
                                "/api/password-expiration-notifications/test-trigger", // Test endpoint for cron job
                                "/api/password-expiration-notifications/status", // Status endpoint
                                "/ws/**", "/oauth2/**", "/saml2/**", "/login/**", // General SSO endpoints
                                "/actuator/**", "/favicon.ico", "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(customAuthenticationEntryPoint()));

        configureSamlLogin(http);
        configureOauth2Login(http);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private boolean isSamlEnabled() {
        try {
            return dynamicSamlRepository.isSamlEnabled();
        } catch (Exception e) {
            logger.warn()

                    .message(String.format(AppConstants.SECURITY_SAML_CONFIG_ERROR_LOG, e.getMessage()))
                    .detail("exceptionType", e.getClass().getSimpleName())
                    .detail("exceptionMessage", e.getMessage())
                    .log();
            return false;
        }
    }

    private boolean isOAuth2Enabled() {
        try {
            return dynamicOAuth2Repository.isOAuth2Enabled();
        } catch (Exception e) {
            logger.warn()

                    .message(String.format(AppConstants.SECURITY_OAUTH2_CONFIG_ERROR_LOG, e.getMessage()))
                    .detail("exceptionType", e.getClass().getSimpleName())
                    .detail("exceptionMessage", e.getMessage())
                    .log();
            return false;
        }
    }

    private boolean isLdapEnabled() {
        try {
            return dynamicLdapProvider.isLdapEnabled();
        } catch (Exception e) {
            logger.warn()

                    .message(String.format(AppConstants.SECURITY_LDAP_CONFIG_ERROR_LOG, e.getMessage()))
                    .detail("exceptionType", e.getClass().getSimpleName())
                    .detail("exceptionMessage", e.getMessage())
                    .log();
            return false;
        }
    }

    /**
     * Configures SAML login flow if enabled in the configuration.
     *
     * @param http the {@link HttpSecurity} instance
     * @throws Exception in case of configuration errors
     */
    private void configureSamlLogin(HttpSecurity http) throws Exception {
        if (isSamlEnabled()) {
            logger.info().message(AppConstants.SECURITY_SAML_LOGIN_CONFIG_LOG).log();
            http.saml2Login(saml2 -> saml2
                    .relyingPartyRegistrationRepository(dynamicSamlRepository)
                    .successHandler((request, response, authentication) -> {
                        logger.info().message(
                                String.format(AppConstants.SECURITY_SAML_AUTH_SUCCESS_LOG, authentication.getName()));
                        response.sendRedirect(AppConstants.SECURITY_SAML_CALLBACK_SUCCESS_URL);
                    })
                    .failureHandler((request, response, exception) -> {
                        logger.error(exception).message(
                                String.format(AppConstants.SECURITY_SAML_AUTH_FAILED_LOG, exception.getMessage()));
                        response.sendRedirect(AppConstants.SECURITY_SAML_CALLBACK_ERROR_URL +
                                URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8));
                    }));
        }
    }

    /**
     * Configures OAuth2 login flow if enabled in the configuration.
     *
     * @param http the {@link HttpSecurity} instance
     * @throws Exception in case of configuration errors
     */
    private void configureOauth2Login(HttpSecurity http) throws Exception {
        if (isOAuth2Enabled()) {
            logger.info().message(AppConstants.SECURITY_OAUTH2_LOGIN_CONFIG_LOG).log();
            http.oauth2Login(oauth2 -> oauth2
                    .clientRegistrationRepository(dynamicOAuth2Repository)
                    .defaultSuccessUrl(AppConstants.SECURITY_OAUTH2_CALLBACK_SUCCESS_URL, true)
                    .failureUrl(AppConstants.SECURITY_OAUTH2_CALLBACK_ERROR_URL)
                    .successHandler((request, response, authentication) -> {
                        logger.info().message(
                                String.format(AppConstants.SECURITY_OAUTH2_AUTH_SUCCESS_LOG, authentication.getName()));
                        response.sendRedirect(AppConstants.SECURITY_OAUTH2_CALLBACK_SUCCESS_URL);
                    })
                    .failureHandler((request, response, exception) -> {
                        logger.error(exception).message(
                                String.format(AppConstants.SECURITY_OAUTH2_AUTH_FAILED_LOG, exception.getMessage()));
                        response.sendRedirect(AppConstants.SECURITY_OAUTH2_CALLBACK_ERROR_URL +
                                URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8));
                    }));
        }
    }

    /**
     * Configures the authentication manager with custom authentication providers.
     *
     * @return the configured {@link AuthenticationManager}
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        List<AuthenticationProvider> providers = new ArrayList<>();
        if (isLdapEnabled()) {
            providers.add(dynamicLdapProvider);
            logger.info().message(AppConstants.SECURITY_LDAP_PROVIDER_ENABLED_LOG).log();
        }

        // Always add the database authentication provider as a fallback or primary
        providers.add(databaseAuthenticationProvider());
        logger.info().message(isLdapEnabled() ? "Database authentication provider configured as fallback."
                : "Database authentication provider configured as primary.");

        return new ProviderManager(providers);
    }

    /**
     * Creates an AuthenticationProvider for database-backed authentication.
     * 
     * @return A configured AuthenticationProvider.
     */
    private AuthenticationProvider databaseAuthenticationProvider() {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                try {
                    String username = authentication.getName();
                    String password = authentication.getCredentials().toString();
                    UserDetails user = userDetailsService.loadUserByUsername(username);
                    if (passwordEncoder.matches(password, user.getPassword())) {
                        return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
                    } else {
                        throw new BadCredentialsException(AppConstants.SECURITY_INVALID_CREDENTIALS_MSG);
                    }
                } catch (Exception e) {
                    logger.error(e)
                            .message(String.format(AppConstants.DB_AUTH_FAILED_FOR_USER, authentication.getName()))
                            .log();
                    throw new BadCredentialsException(AppConstants.AUTH_FAILED_MSG, e);
                }
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return authentication.equals(UsernamePasswordAuthenticationToken.class);
            }
        };
    }

    /**
     * Custom authentication entry point for handling unauthorized requests.
     *
     * @return the custom {@link AuthenticationEntryPoint}
     */
    @Bean
    public AuthenticationEntryPoint customAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter()
                    .write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
        };
    }
}