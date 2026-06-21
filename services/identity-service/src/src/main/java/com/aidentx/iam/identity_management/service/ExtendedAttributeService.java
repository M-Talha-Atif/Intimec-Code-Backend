package com.aidentx.iam.identity_management.service;

import com.aidentx.iam.identity_management.model.enums.IdentityType;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing extended attributes for identities.
 */
public interface ExtendedAttributeService {

        /**
         * Saves extended attributes for an identity during creation.
         * For required attributes, either a value must be provided or a default value
         * must exist.
         *
         * @param identityId   The identity ID
         * @param tenantId     The tenant ID
         * @param identityType The identity type (EMPLOYEE, NON_EMPLOYEE, NON_HUMAN)
         * @param attributes   The extended attributes to save
         */
        void saveExtendedAttributesForCreation(UUID identityId, String tenantId, IdentityType identityType,
                        Map<String, Object> attributes);

        /**
         * Saves extended attributes for an identity during update.
         * For required attributes, either a value must be provided, a default value
         * must exist, or the identity must already have a value.
         *
         * @param identityId   The identity ID
         * @param tenantId     The tenant ID
         * @param identityType The identity type (EMPLOYEE, NON_EMPLOYEE, NON_HUMAN)
         * @param attributes   The extended attributes to save
         */
        void saveExtendedAttributesForUpdate(UUID identityId, String tenantId, IdentityType identityType,
                        Map<String, Object> attributes);

        /**
         * Retrieves extended attributes for an identity.
         *
         * @param identityId   The identity ID
         * @param tenantId     The tenant ID
         * @param identityType The identity type
         * @return Map of extended attributes (system_key -> value)
         */
        Map<String, Object> findExtendedAttributesForIdentity(UUID identityId, String tenantId,
                        IdentityType identityType);

        /**
         * Deletes all extended attributes for an identity.
         *
         * @param identityId   The identity ID
         * @param tenantId     The tenant ID
         * @param identityType The identity type
         */
        void deleteExtendedAttributes(UUID identityId, String tenantId, IdentityType identityType);

        /**
         * Validates extended attributes for an update operation.
         *
         * @param tenantId           The tenant ID
         * @param identityType       The identity type
         * @param extendedAttributes The extended attributes to validate
         */
        void validateExtendedAttributeUpdate(String tenantId, IdentityType identityType,
                        Map<String, Object> extendedAttributes);
}
