package com.aidentx.authservice.config;

import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Dynamic implementation of {@link RelyingPartyRegistrationRepository} that
 * loads
 * and maps SAML configuration from the {@link SsoConfigurationManager}.
 * <p>
 * This class supports runtime refresh using {@link RefreshScope}, allowing SAML
 * configurations to be updated without restarting the application.
 *
 * <h3>Features:</h3>
 * <ul>
 * <li>Reads and maps {@link SamlConfig} into
 * {@link RelyingPartyRegistration}</li>
 * <li>Handles SAML bindings (POST or REDIRECT)</li>
 * <li>Parses IdP X.509 certificates in PEM format</li>
 * <li>Supports logout bindings and NameID format resolution</li>
 * <li>Logs registration errors for observability</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
@Component
@RefreshScope
public class DynamicSamlRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    private final IUnifiedLogger logger;
    private final SsoConfigurationManager ssoConfigurationManager;

    /**
     * Constructor for dependency injection.
     *
     * @param ssoConfigurationManager the configuration manager providing SAML
     *                                settings
     */
    public DynamicSamlRelyingPartyRegistrationRepository(SsoConfigurationManager ssoConfigurationManager,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.ssoConfigurationManager = ssoConfigurationManager;
        this.logger = unifiedLoggerFactory.getLogger("SsoConfigManager", AppConstants.SAML_LOGGER_CATEGORY);
    }

    /**
     * Returns the {@link RelyingPartyRegistration} based on the provided
     * registration ID,
     * if SAML is enabled and properly configured.
     *
     * @param registrationId the ID of the relying party (usually "saml")
     * @return the constructed {@link RelyingPartyRegistration} or {@code null} if
     *         SAML is disabled or misconfigured
     */
    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        try {
            SamlConfig samlConfig = ssoConfigurationManager.getSamlConfig();
            if (samlConfig != null && samlConfig.isEnabled()) {
                return mapToRelyingPartyRegistration(samlConfig);
            }
            logger.debug().category(AppConstants.SAML_CONFIG_CATEGORY)
                    .message(AppConstants.LOG_SAML_CONFIG_NOT_FOUND)
                    .detail(AppConstants.LOG_DETAIL_REGISTRATION_ID, registrationId)
                    .log();
            return null;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_SAML_FIND_REGISTRATION_FAILED + registrationId).log();
            return null;
        }
    }

    /**
     * Converts a {@link SamlConfig} instance into a
     * {@link RelyingPartyRegistration}
     * recognized by Spring Security.
     *
     * @param samlConfig the loaded SAML configuration object
     * @return the mapped registration or {@code null} if conversion fails
     */
    private RelyingPartyRegistration mapToRelyingPartyRegistration(SamlConfig samlConfig) {
        if (samlConfig == null) {
            return null;
        }
        try {
            final Saml2MessageBinding samlBinding = AppConstants.SAML_BINDING_REDIRECT
                    .equalsIgnoreCase(samlConfig.getSamlBinding())
                            ? Saml2MessageBinding.REDIRECT
                            : Saml2MessageBinding.POST;

            RelyingPartyRegistration.Builder builder = RelyingPartyRegistration
                    .withRegistrationId(AppConstants.SAML_PROVIDER_REGISTRATION_ID) // Use a consistent, descriptive ID
                    .entityId(samlConfig.getEntityId())
                    .assertionConsumerServiceLocation(samlConfig.getAssertionConsumerServiceUrl())
                    .assertionConsumerServiceBinding(samlBinding);

            if (samlConfig.getNameIdFormat() != null && !samlConfig.getNameIdFormat().isEmpty()) {
                String nameIdFormatUrn = convertToNameIdFormatUrn(samlConfig.getNameIdFormat());
                builder.nameIdFormat(nameIdFormatUrn);
            }

            if (samlConfig.getSingleLogoutServiceUrl() != null) {
                builder
                        .singleLogoutServiceLocation(samlConfig.getSingleLogoutServiceUrl())
                        .singleLogoutServiceBinding(samlBinding);
            }

            builder.assertingPartyMetadata(party -> party
                    .entityId(samlConfig.getIdpEntityId())
                    .singleSignOnServiceLocation(samlConfig.getIdpSsoUrl())
                    .singleSignOnServiceBinding(samlBinding)
                    .wantAuthnRequestsSigned(false)
                    .verificationX509Credentials(c -> {
                        X509Certificate idpCert = getVerificationCertificate(samlConfig);
                        if (idpCert != null) {
                            c.add(Saml2X509Credential.verification(idpCert));
                        } else {
                            logger.warn().category(AppConstants.SAML_CONFIG_CATEGORY)
                                    .message(AppConstants.LOG_SAML_CERT_MISSING)
                                    .log();
                        }
                    }));

            return builder.build();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_SAML_MAPPING_FAILED).log();
            return null;
        }
    }

    /**
     * Parses a PEM-formatted certificate string into an {@link X509Certificate}.
     *
     * @param certificatePem the raw certificate string including headers
     * @return the parsed certificate object
     * @throws Exception if parsing fails
     */
    private X509Certificate parseCertificate(String certificatePem) throws Exception {
        String certificateBase64 = certificatePem
                .replace(AppConstants.CERT_BEGIN_HEADER, "")
                .replace(AppConstants.CERT_END_HEADER, "")
                .replaceAll(AppConstants.WHITESPACE_REGEX, "");

        byte[] certificateBytes = Base64.getDecoder().decode(certificateBase64);
        CertificateFactory certificateFactory = CertificateFactory.getInstance(AppConstants.CERT_TYPE_X509);
        return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));
    }

    /**
     * Retrieves and parses the IdP certificate used for verifying SAML assertions.
     *
     * @param samlConfig the current SAML configuration
     * @return the {@link X509Certificate} or {@code null} if missing or invalid
     */
    private X509Certificate getVerificationCertificate(SamlConfig samlConfig) {
        try {
            String certificatePem = samlConfig.getIdpX509Certificate();

            if (certificatePem == null || certificatePem.trim().isEmpty()) {
                logger.warn().category(AppConstants.SAML_CONFIG_CATEGORY)
                        .message(AppConstants.LOG_SAML_CERT_NOT_CONFIGURED)
                        .log();
                return null;
            }

            return parseCertificate(certificatePem);
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_SAML_CERT_PARSE_FAILED).log();
            return null;
        }
    }

    /**
     * Converts simplified or shorthand NameID format labels into standard SAML
     * URNs.
     * Falls back to {@code unspecified} if unrecognized.
     *
     * @param nameIdFormat the provided NameID format string
     * @return a valid URN string for SAML NameID format
     */
    private String convertToNameIdFormatUrn(String nameIdFormat) {
        if (nameIdFormat == null) {
            return AppConstants.NAMEID_FORMAT_UNSPECIFIED;
        }

        return switch (nameIdFormat.toLowerCase()) {
            case AppConstants.NAMEID_SHORT_EMAIL, AppConstants.NAMEID_SHORT_EMAIL_ALT ->
                AppConstants.NAMEID_FORMAT_EMAIL;
            case AppConstants.NAMEID_SHORT_ENTITY -> AppConstants.NAMEID_FORMAT_ENTITY;
            case AppConstants.NAMEID_SHORT_KERBEROS -> AppConstants.NAMEID_FORMAT_KERBEROS;
            case AppConstants.NAMEID_SHORT_PERSISTENT -> AppConstants.NAMEID_FORMAT_PERSISTENT;
            case AppConstants.NAMEID_SHORT_TRANSIENT -> AppConstants.NAMEID_FORMAT_TRANSIENT;
            case AppConstants.NAMEID_SHORT_UNSPECIFIED -> AppConstants.NAMEID_FORMAT_UNSPECIFIED;
            case AppConstants.NAMEID_SHORT_WINDOWS_DOMAIN -> AppConstants.NAMEID_FORMAT_WINDOWS_DOMAIN;
            case AppConstants.NAMEID_SHORT_X509_SUBJECT -> AppConstants.NAMEID_FORMAT_X509_SUBJECT;
            default -> {
                if (nameIdFormat.startsWith(AppConstants.NAMEID_URN_PREFIX)) {
                    yield nameIdFormat;
                }
                logger.warn().category(AppConstants.SAML_CONFIG_CATEGORY)
                        .message(AppConstants.LOG_SAML_NAMEID_UNKNOWN)
                        .detail(AppConstants.LOG_DETAIL_PROVIDED_FORMAT, nameIdFormat)
                        .log();
                yield AppConstants.NAMEID_FORMAT_UNSPECIFIED;
            }
        };
    }

    /**
     * Returns a list containing all available and enabled
     * {@link RelyingPartyRegistration} instances.
     *
     * @return list of relying party registrations; empty if none available
     */
    public List<RelyingPartyRegistration> getAllRegistrations() {
        List<RelyingPartyRegistration> registrations = new ArrayList<>();
        try {
            SamlConfig samlConfig = ssoConfigurationManager.getSamlConfig();
            if (samlConfig != null && samlConfig.isEnabled()) {
                RelyingPartyRegistration registration = mapToRelyingPartyRegistration(samlConfig);
                if (registration != null) {
                    registrations.add(registration);
                }
            }
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_SAML_GET_ALL_REGISTRATIONS_FAILED).log();
        }
        return registrations;
    }

    /**
     * Checks whether SAML authentication is currently enabled based on runtime
     * configuration.
     *
     * @return {@code true} if SAML is enabled and configured, otherwise
     *         {@code false}
     */
    public boolean isSamlEnabled() {
        try {
            SamlConfig samlConfig = ssoConfigurationManager.getSamlConfig();
            return samlConfig != null && samlConfig.isEnabled();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_SAML_STATUS_CHECK_FAILED).log();
            return false;
        }
    }
}