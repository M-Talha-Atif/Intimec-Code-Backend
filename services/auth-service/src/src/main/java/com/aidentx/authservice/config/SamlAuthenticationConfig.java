package com.aidentx.authservice.config;

import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Spring configuration for SAML authentication integration.
 * <p>
 * Provides all required beans for SAML relying party registration and
 * certificate management.
 * This configuration is only active when SAML is enabled in the SSO
 * configuration.
 * If SAML is disabled or not properly configured, this class provides a no-op
 * {@link RelyingPartyRegistrationRepository} to ensure the application context
 * loads without errors.
 *
 * <p>
 * Key beans and helpers provided:
 * <ul>
 * <li>{@link RelyingPartyRegistrationRepository} for SAML relying party
 * management</li>
 * <li>Validation of required SAML configuration fields</li>
 * <li>Helpers for building SAML registration and parsing certificates</li>
 * <li>Support for custom NameID formats and single logout</li>
 * </ul>
 *
 * <p>
 * All error and status messages are managed via {@link AppConstants}.
 */
@Configuration
public class SamlAuthenticationConfig {

    private final IUnifiedLogger logger;
    private final SsoConfigurationManager ssoConfigurationManager;

    /**
     * Constructor for dependency injection.
     *
     * @param ssoConfigurationManager The manager for fetching SSO configurations.
     *                         \
     */
    public SamlAuthenticationConfig(SsoConfigurationManager ssoConfigurationManager,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.ssoConfigurationManager = ssoConfigurationManager;
        this.logger = unifiedLoggerFactory.getLogger("SamlAuthConfig", "config-saml");
    }

    /**
     * Creates a {@link RelyingPartyRegistrationRepository} bean used by Spring
     * Security SAML integration.
     * <p>
     * This method dynamically builds a {@link RelyingPartyRegistration} from
     * external SAML configuration
     * managed through {@link SsoConfigurationManager}. If SAML is disabled or
     * misconfigured, it returns
     * a no-op implementation to avoid breaking the application context.
     *
     * <p>
     * Key features:
     * <ul>
     * <li>Supports POST and REDIRECT bindings</li>
     * <li>Optional NameID format support</li>
     * <li>Optional Single Logout configuration</li>
     * <li>Sets up IdP metadata with optional verification certificate</li>
     * </ul>
     *
     * @return a configured {@link RelyingPartyRegistrationRepository} or a no-op
     *         fallback
     */
    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        try {
            SamlConfig samlConfig = ssoConfigurationManager.getSamlConfig();
            if (samlConfig == null || !samlConfig.isEnabled()) {
                logger.info().message(AppConstants.SAML_DISABLED_OR_NOT_CONFIGURED).log();
                return new EmptyRelyingPartyRegistrationRepository();
            }

            if (!isValidSamlConfig(samlConfig)) {
                logger.warn()

                        .message(AppConstants.SAML_CONFIG_INVALID_OR_INCOMPLETE)
                        .log();
                return new EmptyRelyingPartyRegistrationRepository();
            }

            final Saml2MessageBinding samlBinding = "REDIRECT".equalsIgnoreCase(samlConfig.getSamlBinding())
                    ? Saml2MessageBinding.REDIRECT
                    : Saml2MessageBinding.POST;

            RelyingPartyRegistration.Builder registrationBuilder = RelyingPartyRegistration
                    .withRegistrationId("saml-provider")
                    .entityId(samlConfig.getEntityId())
                    .assertionConsumerServiceLocation(samlConfig.getAssertionConsumerServiceUrl())
                    .assertionConsumerServiceBinding(samlBinding);

            if (hasText(samlConfig.getNameIdFormat())) {
                String nameIdFormatUrn = convertToNameIdFormatUrn(samlConfig.getNameIdFormat());
                registrationBuilder.nameIdFormat(nameIdFormatUrn);
            }

            if (hasText(samlConfig.getSingleLogoutServiceUrl())) {
                registrationBuilder
                        .singleLogoutServiceLocation(samlConfig.getSingleLogoutServiceUrl())
                        .singleLogoutServiceBinding(samlBinding);
            }

            registrationBuilder.assertingPartyMetadata(party -> party
                    .entityId(samlConfig.getIdpEntityId())
                    .singleSignOnServiceLocation(samlConfig.getIdpSsoUrl())
                    .singleSignOnServiceBinding(samlBinding)
                    .wantAuthnRequestsSigned(false)
                    .verificationX509Credentials(c -> {
                        X509Certificate idpCert = getVerificationCertificate();
                        if (idpCert != null) {
                            c.add(Saml2X509Credential.verification(idpCert));
                        } else {
                            logger.warn().message(AppConstants.SAML_NO_IDP_VERIFICATION_CERT).log();
                        }
                    }));

            RelyingPartyRegistration registration = registrationBuilder.build();
            logger.info()
                    .message("Successfully created SAML Relying Party Registration")
                    .objectId(registration.getRegistrationId())
                    .log();

            return new InMemoryRelyingPartyRegistrationRepository(registration);

        } catch (Exception e) {
            logger.error(e).message(AppConstants.SAML_FAILED_TO_CREATE_CONFIG).log();
            return new EmptyRelyingPartyRegistrationRepository();
        }
    }

    /**
     * Validates that all required SAML configuration fields are present and
     * non-empty.
     *
     * @param samlConfig the SAML configuration to validate
     * @return true if the configuration is valid, false otherwise
     */
    private boolean isValidSamlConfig(SamlConfig samlConfig) {
        if (samlConfig == null) {
            return false;
        }

        return hasText(samlConfig.getEntityId()) &&
                hasText(samlConfig.getIdpEntityId()) &&
                hasText(samlConfig.getIdpSsoUrl()) &&
                hasText(samlConfig.getAssertionConsumerServiceUrl());
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
     * Parses a PEM-formatted X.509 certificate string into a
     * {@link X509Certificate}.
     *
     * @param certificatePem the PEM-formatted certificate string
     * @return the parsed X509Certificate
     * @throws Exception if parsing fails
     */
    private X509Certificate parseCertificate(String certificatePem) throws Exception {
        String certificateBase64 = certificatePem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");

        byte[] certificateBytes = Base64.getDecoder().decode(certificateBase64);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));
    }

    /**
     * Loads the IdP verification certificate from PEM format stored in config.
     * This certificate is used to verify SAML responses from the IdP.
     *
     * @return the IdP verification X509Certificate, or null if not present
     */
    private X509Certificate getVerificationCertificate() {
        try {
            SamlConfig samlConfig = ssoConfigurationManager.getSamlConfig();
            String certificatePem = samlConfig.getIdpX509Certificate();

            if (!hasText(certificatePem)) {
                logger.warn().message(AppConstants.SAML_NO_IDP_X509_CERT).log();
                return null;
            }

            return parseCertificate(certificatePem);
        } catch (Exception e) {
            logger.error(e).message(AppConstants.SAML_FAILED_TO_LOAD_IDP_CERT).log();
            throw new RuntimeException(AppConstants.SAML_FAILED_TO_LOAD_IDP_CERT, e);
        }
    }

    /**
     * Converts simplified NameID format names to full URNs.
     * Supports the simplified format names from the configuration.
     *
     * @param nameIdFormat the simplified or full NameID format string
     * @return the full URN NameID format string
     */
    private String convertToNameIdFormatUrn(String nameIdFormat) {
        if (nameIdFormat == null) {
            return "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
        }

        return switch (nameIdFormat.toLowerCase()) {
            case "emailaddress", "email" -> "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";
            case "entity" -> "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
            case "kerberos" -> "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos";
            case "persistent" -> "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";
            case "transient" -> "urn:oasis:names:tc:SAML:2.0:nameid-format:transient";
            case "unspecified" -> "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
            case "windowsdomainqualifiedname" -> "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName";
            case "x509subjectname" -> "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName";
            default -> {
                if (nameIdFormat.startsWith("urn:oasis:names:tc:SAML:")) {
                    yield nameIdFormat;
                }
                logger.warn()
                        .message("Unknown NameID format specified, falling back to unspecified.")
                        .detail("providedFormat", nameIdFormat)
                        .log();
                yield "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
            }
        };
    }

    /**
     * A no-op implementation of {@link RelyingPartyRegistrationRepository} used
     * when SAML is disabled or misconfigured.
     * This ensures the application context can still load without a valid SAML
     * configuration.
     */
    private static class EmptyRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {
        @Override
        public RelyingPartyRegistration findByRegistrationId(String registrationId) {
            return null;
        }
    }
}