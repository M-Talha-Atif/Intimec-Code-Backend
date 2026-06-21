package com.aidentx.iam.access_roles_management.mapper;

import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditConstants;
import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditEvent;
import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditLogConstants;
import com.aidentx.iam.access_roles_management.model.dto.AccessRoleAuditHistoryDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.fasterxml.jackson.databind.JsonNode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Mapper for converting between access role audit event, persistent audit log,
 * and UI-facing history DTO.
 *
 * <p>This mapper keeps persistence and presentation mapping logic out of the
 * service layer, following the Single Responsibility Principle.</p>
 */
@Mapper(
        componentModel = "spring",
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL
)
public interface AccessRoleAuditLogMapper {

    /**
     * Maps a persistent audit log entity to a UI-facing history DTO.
     *
     * @param entity the persistent audit log entity
     * @return mapped history DTO
     */
    AccessRoleAuditHistoryDto entityToHistoryDto(AccessRoleAuditLog entity);

    /**
     * Maps an in-memory audit event to a persistent audit log entity.
     *
     * @param event the in-memory audit event created by the audit aspect
     * @return persistent audit log entity ready to be saved
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "ruleId", ignore = true)
    @Mapping(target = "ruleName", ignore = true)
    @Mapping(target = "roleId", source = "entityId", qualifiedByName = "stringToUuid")
    @Mapping(target = "roleName", source = "entityName")
    @Mapping(target = "roleType", source = "roleType")
    @Mapping(target = "entityType", source = "roleType", qualifiedByName = "roleTypeToEntityType")
    @Mapping(target = "activityLabel", expression = "java(com.aidentx.iam.access_roles_management.audit.AccessRoleAuditConstants.toActivityLabel(event.action(), event.roleType()))")
    @Mapping(target = "action", source = "action")
    @Mapping(target = "triggerSource", expression = "java(resolveTriggerSource(event))")
    @Mapping(target = "description", expression = "java(resolveDescription(event))")
    @Mapping(target = "actorId", source = "actorId")
    @Mapping(target = "actorUsername", source = "actorUsername")
    @Mapping(target = "stateBefore", source = "stateBefore")
    @Mapping(target = "stateAfter", source = "stateAfter")
    @Mapping(target = "changedFields", expression = "java(computeChangedFields(event))")
    @Mapping(target = "addedChildren", expression = "java(emptyToNullChildren(event.addedChildren()))")
    @Mapping(target = "removedChildren", expression = "java(emptyToNullChildren(event.removedChildren()))")
    @Mapping(target = "failedChildren", expression = "java(emptyToNullFailedChildren(event.failedChildren()))")
    @Mapping(target = "addedEntitlements", expression = "java(emptyToNullEntitlements(event.addedEntitlements()))")
    @Mapping(target = "removedEntitlements", expression = "java(emptyToNullEntitlements(event.removedEntitlements()))")
    AccessRoleAuditLog eventToEntity(AccessRoleAuditEvent event);

    /**
     * Maps a raw role type to a user-friendly entity type shown to the UI.
     *
     * @param roleType raw role subtype (business, technical, etc.)
     * @return entity type such as Business Role or Technical Role
     */
    @Named("roleTypeToEntityType")
    default String roleTypeToEntityType(String roleType) {
        return AccessRoleAuditConstants.toEntityType(roleType);
    }

    /**
     * Maps a raw action to a user-friendly description.
     *
     * @param action the raw audit action
     * @return human-readable description
     */
    @Named("actionToDescription")
    default String actionToDescription(String action) {
        if (Objects.equals(action, AccessRoleAuditConstants.ACTION_ROLE_CREATED)) {
            return AccessRoleAuditConstants.DESC_ROLE_CREATED;
        }
        if (Objects.equals(action, AccessRoleAuditConstants.ACTION_ROLE_UPDATED)) {
            return AccessRoleAuditConstants.DESC_ROLE_UPDATED;
        }
        if (Objects.equals(action, AccessRoleAuditConstants.ACTION_ROLE_DELETED)) {
            return AccessRoleAuditConstants.DESC_ROLE_DELETED;
        }
        if (Objects.equals(action, AccessRoleAuditConstants.ACTION_ENTITLEMENTS_UPDATED)) {
            return AccessRoleAuditConstants.DESC_ENTITLEMENTS_UPDATED;
        }
        if (Objects.equals(action, AccessRoleAuditConstants.ACTION_CHILDREN_UPDATED)) {
            return AccessRoleAuditConstants.DESC_CHILDREN_UPDATED;
        }
        return action;
    }

    default String resolveTriggerSource(AccessRoleAuditEvent event) {
        return isImportCreated(event) ? AccessRoleAuditConstants.TRIGGER_IMPORT : AccessRoleAuditConstants.TRIGGER_API;
    }

    default String resolveDescription(AccessRoleAuditEvent event) {
        if (isImportCreated(event)) {
            return AccessRoleAuditConstants.DESC_ROLE_CREATED_THROUGH_IMPORT;
        }
        return actionToDescription(event.action());
    }

    default boolean isImportCreated(AccessRoleAuditEvent event) {
        if (event == null || !Objects.equals(event.action(), AccessRoleAuditConstants.ACTION_ROLE_CREATED)) {
            return false;
        }
        JsonNode stateAfter = event.stateAfter();
        return stateAfter != null
                && stateAfter.has(AccessRoleAuditConstants.STATE_FIELD_CREATED_THROUGH)
                && Objects.equals(
                        stateAfter.get(AccessRoleAuditConstants.STATE_FIELD_CREATED_THROUGH).asText(),
                        AccessRoleAuditConstants.TRIGGER_IMPORT);
    }

    /**
     * Safely converts a UUID string to UUID.
     *
     * @param value UUID string
     * @return parsed UUID or null
     */
    @Named("stringToUuid")
    default UUID stringToUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    /**
     * Computes changed fields for the persistent audit record.
     *
     * <p>For relationship sync operations, uses stable semantic field names.
     * For metadata update, compares before/after JSON object fields.
     * For create/delete, returns an empty list.</p>
     *
     * @param event the audit event
     * @return list of changed field names
     */
    default List<String> computeChangedFields(AccessRoleAuditEvent event) {
        if (Objects.equals(event.action(), AccessRoleAuditConstants.ACTION_ENTITLEMENTS_UPDATED)) {
            return List.of(AccessRoleAuditLogConstants.FIELD_ENTITLEMENTS);
        }

        if (Objects.equals(event.action(), AccessRoleAuditConstants.ACTION_CHILDREN_UPDATED)) {
            return List.of(AccessRoleAuditLogConstants.FIELD_CHILD_ROLES);
        }

        if (!Objects.equals(event.action(), AccessRoleAuditConstants.ACTION_ROLE_UPDATED)) {
            return List.of();
        }

        if (!(event.stateBefore() instanceof JsonNode before) || !(event.stateAfter() instanceof JsonNode after)) {
            return List.of();
        }

        if (!before.isObject() || !after.isObject()) {
            return List.of(AccessRoleAuditLogConstants.FIELD_ROLE);
        }

        List<String> changedFields = new ArrayList<>();
        Iterator<String> fieldNames = after.fieldNames();

        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            JsonNode beforeValue = before.get(field);
            JsonNode afterValue = after.get(field);
            if (!Objects.equals(beforeValue, afterValue)) {
                changedFields.add(field);
            }
        }

        Iterator<String> beforeFieldNames = before.fieldNames();
        while (beforeFieldNames.hasNext()) {
            String field = beforeFieldNames.next();
            if (!after.has(field) && !changedFields.contains(field)) {
                changedFields.add(field);
            }
        }

        return changedFields;
    }

    /**
     * Returns null instead of empty collections for child details.
     *
     * @param details child detail list
     * @return original list or null if empty
     */
    default Object emptyToNullChildren(List<AccessRoleAuditEvent.ChildRoleAuditDetail> details) {
        return CollectionUtils.isEmpty(details) ? null : details;
    }

    /**
     * Returns null instead of empty collections for failed child details.
     *
     * @param details failed child detail list
     * @return original list or null if empty
     */
    default Object emptyToNullFailedChildren(List<AccessRoleAuditEvent.FailedChildRoleAuditDetail> details) {
        return CollectionUtils.isEmpty(details) ? null : details;
    }

    /**
     * Returns null instead of empty collections for entitlement details.
     *
     * @param details entitlement detail list
     * @return original list or null if empty
     */
    default Object emptyToNullEntitlements(List<AccessRoleAuditEvent.EntitlementAuditDetail> details) {
        return CollectionUtils.isEmpty(details) ? null : details;
    }
}

