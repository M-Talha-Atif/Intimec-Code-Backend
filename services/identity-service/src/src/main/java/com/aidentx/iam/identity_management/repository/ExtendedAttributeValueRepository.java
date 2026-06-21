package com.aidentx.iam.identity_management.repository;

import com.aidentx.iam.identity_management.model.entity.ExtendedAttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for fetching extended attribute values with their definitions in a
 * single query.
 * Uses JPA Specifications for efficient querying.
 */
@Repository
public interface ExtendedAttributeValueRepository extends JpaRepository<ExtendedAttributeDefinition, UUID>,
        JpaSpecificationExecutor<ExtendedAttributeDefinition> {

    /**
     * Custom query to fetch all extended attribute values for an identity with their definitions.
     * Uses native SQL for better control over type casting in UNION queries.
     */
    @Query(value = """
        SELECT DISTINCT 
            ead.id,
            ead.created_at,
            ead.data_type,
            ead.description,
            ead.display_name,
            ead.display_order,
            ead.identity_type,
            ead.is_required,
            ead.is_sensitive,
            ead.is_unique,
            ead.is_writable,
            ead.system_key,
            ead.tenant_id,
            ead.updated_at,
            'STRING' as value_type,
            sv.value::text as string_value,
            NULL::numeric as number_value,
            NULL::boolean as boolean_value,
            NULL::timestamp as date_value
        FROM extended_attribute_definitions ead
        LEFT JOIN ext_attr_string_values sv ON ead.id = sv.definition_id AND sv.identity_id = :identityId
        WHERE ead.tenant_id = :tenantId AND ead.identity_type = :identityType AND sv.value IS NOT NULL

        UNION

        SELECT DISTINCT 
            ead.id,
            ead.created_at,
            ead.data_type,
            ead.description,
            ead.display_name,
            ead.display_order,
            ead.identity_type,
            ead.is_required,
            ead.is_sensitive,
            ead.is_unique,
            ead.is_writable,
            ead.system_key,
            ead.tenant_id,
            ead.updated_at,
            'NUMBER' as value_type,
            NULL::text as string_value,
            nv.value as number_value,
            NULL::boolean as boolean_value,
            NULL::timestamp as date_value
        FROM extended_attribute_definitions ead
        LEFT JOIN ext_attr_number_values nv ON ead.id = nv.definition_id AND nv.identity_id = :identityId
        WHERE ead.tenant_id = :tenantId AND ead.identity_type = :identityType AND nv.value IS NOT NULL

        UNION

        SELECT DISTINCT 
            ead.id,
            ead.created_at,
            ead.data_type,
            ead.description,
            ead.display_name,
            ead.display_order,
            ead.identity_type,
            ead.is_required,
            ead.is_sensitive,
            ead.is_unique,
            ead.is_writable,
            ead.system_key,
            ead.tenant_id,
            ead.updated_at,
            'BOOLEAN' as value_type,
            NULL::text as string_value,
            NULL::numeric as number_value,
            bv.value as boolean_value,
            NULL::timestamp as date_value
        FROM extended_attribute_definitions ead
        LEFT JOIN ext_attr_boolean_values bv ON ead.id = bv.definition_id AND bv.identity_id = :identityId
        WHERE ead.tenant_id = :tenantId AND ead.identity_type = :identityType AND bv.value IS NOT NULL

        UNION

        SELECT DISTINCT 
            ead.id,
            ead.created_at,
            ead.data_type,
            ead.description,
            ead.display_name,
            ead.display_order,
            ead.identity_type,
            ead.is_required,
            ead.is_sensitive,
            ead.is_unique,
            ead.is_writable,
            ead.system_key,
            ead.tenant_id,
            ead.updated_at,
            'ARRAY' as value_type,
            av.value::text as string_value,
            NULL::numeric as number_value,
            NULL::boolean as boolean_value,
            NULL::timestamp as date_value
        FROM extended_attribute_definitions ead
        LEFT JOIN ext_attr_array_values av ON ead.id = av.definition_id AND av.identity_id = :identityId
        WHERE ead.tenant_id = :tenantId AND ead.identity_type = :identityType AND av.value IS NOT NULL

        UNION

        SELECT DISTINCT 
            ead.id,
            ead.created_at,
            ead.data_type,
            ead.description,
            ead.display_name,
            ead.display_order,
            ead.identity_type,
            ead.is_required,
            ead.is_sensitive,
            ead.is_unique,
            ead.is_writable,
            ead.system_key,
            ead.tenant_id,
            ead.updated_at,
            'OBJECT' as value_type,
            ov.value::text as string_value,
            NULL::numeric as number_value,
            NULL::boolean as boolean_value,
            NULL::timestamp as date_value
        FROM extended_attribute_definitions ead
        LEFT JOIN ext_attr_object_values ov ON ead.id = ov.definition_id AND ov.identity_id = :identityId
        WHERE ead.tenant_id = :tenantId AND ead.identity_type = :identityType AND ov.value IS NOT NULL
        """, nativeQuery = true)
    List<Object[]> findAllValuesWithDefinitions(@Param("identityId") UUID identityId,
            @Param("tenantId") String tenantId,
            @Param("identityType") String identityType);
}