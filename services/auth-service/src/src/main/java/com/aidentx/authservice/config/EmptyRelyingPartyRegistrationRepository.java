package com.aidentx.authservice.config;

import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

/**
 * An empty implementation of {@link RelyingPartyRegistrationRepository}.
 * <p>
 * This is used as a no-op bean when SAML support is disabled in the application.
 * Any call to {@link #findByRegistrationId(String)} will always return {@code null}.
 *
 * <p>This prevents Spring Security from failing due to a missing SAML registration repository bean.
 */
public class EmptyRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    /**
     * Always returns {@code null} since no SAML providers are registered.
     *
     * @param registrationId the registration ID (ignored)
     * @return always {@code null}
     */
    @Override
    public RelyingPartyRegistration findByRegistrationId(String registrationId) {
        return null;
    }
}
