package com.aidentx.iam.access_roles_management.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionGroupDto;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRulePreviewRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRulePreviewEntitlementResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRulePreviewIdentityResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleAssignmentRulePreviewResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.RoleEntitlementPreviewSummaryResponse;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleExpressionParser;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleNormalizationService;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRulePreviewService;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleQueryService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementResolutionService;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only preview engine that simulates role assignment/revocation without writing any rows.
 */
@Service
public class AccessRoleAssignmentRulePreviewServiceImpl implements AccessRoleAssignmentRulePreviewService {

    public static final String ACTION_WILL_ADD = "WILL_ADD";
    public static final String ACTION_WILL_REMOVE = "WILL_REMOVE";
    public static final String ACTION_ALREADY_ASSIGNED = "ALREADY_ASSIGNED";

    private final AccessRoleRepository accessRoleRepository;
    private final AccessRoleAssignmentRuleRepository ruleRepository;
    private final IdentityRoleAssignmentRepository assignmentRepository;
    private final RoleEntitlementResolutionService roleEntitlementResolutionService;
    private final EntitlementRepository entitlementRepository;
    private final AccessRoleAssignmentRuleExpressionParser expressionParser;
    private final AccessRoleAssignmentRuleNormalizationService normalizationService;
    private final AccessRoleAssignmentRuleQueryService queryService;

    public AccessRoleAssignmentRulePreviewServiceImpl(AccessRoleRepository accessRoleRepository,
                                                      AccessRoleAssignmentRuleRepository ruleRepository,
                                                      IdentityRoleAssignmentRepository assignmentRepository,
                                                      RoleEntitlementResolutionService roleEntitlementResolutionService,
                                                      EntitlementRepository entitlementRepository,
                                                      AccessRoleAssignmentRuleExpressionParser expressionParser,
                                                      AccessRoleAssignmentRuleNormalizationService normalizationService,
                                                      AccessRoleAssignmentRuleQueryService queryService) {
        this.accessRoleRepository = accessRoleRepository;
        this.ruleRepository = ruleRepository;
        this.assignmentRepository = assignmentRepository;
        this.roleEntitlementResolutionService = roleEntitlementResolutionService;
        this.entitlementRepository = entitlementRepository;
        this.expressionParser = expressionParser;
        this.normalizationService = normalizationService;
        this.queryService = queryService;
    }

    @Override
    @Transactional(readOnly = true)
    public AccessRoleAssignmentRulePreviewResponse previewRule(String tenantId, AccessRoleAssignmentRulePreviewRequest request) {
        validateTenantId(tenantId);
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getRoleId() == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_ROLE_ID_REQUIRED);
        }

        AccessRole role = findTenantRoleOrThrow(tenantId, request.getRoleId());
        Optional<AccessRoleAssignmentRule> existingRule = resolveExistingRule(tenantId, request);
        UUID sourceRuleId = request.getRuleId() != null
                ? request.getRuleId()
                : existingRule.map(AccessRoleAssignmentRule::getId).orElse(null);

        List<String> identityTypes = resolveIdentityTypes(request, existingRule);
        List<AccessRoleAssignmentRuleConditionGroupDto> inputGroups = resolveInputGroups(request, existingRule);
        List<Map<String, Object>> normalizedGroups = normalizationService.normalizeConditionGroups(inputGroups, identityTypes);

        Set<UUID> matchedIdentityIds = queryService.findAllMatchingIdentityIds(tenantId, identityTypes, normalizedGroups);
        Set<UUID> currentlyAssignedIdentityIds = loadCurrentlyAssignedIdentityIds(tenantId, role.getId(), sourceRuleId);
        Set<UUID> willAdd = difference(matchedIdentityIds, currentlyAssignedIdentityIds);
        Set<UUID> willRemove = difference(currentlyAssignedIdentityIds, matchedIdentityIds);
        Set<UUID> alreadyAssigned = intersection(matchedIdentityIds, currentlyAssignedIdentityIds);

        RoleEntitlementPreviewSummaryResponse entitlementSummary = buildEntitlementSummary(tenantId, role, Boolean.TRUE.equals(request.getIncludeEntitlements()));
        List<AccessRoleAssignmentRulePreviewEntitlementResponse> entitlementRows = entitlementSummary != null
                ? entitlementSummary.getEntitlements()
                : List.of();

        List<PreviewIdentityCandidate> candidates = buildPreviewCandidates(willAdd, willRemove, alreadyAssigned);
        int page = normalizePage(request.getPage());
        int size = normalizeSize(request.getSize());
        List<PreviewIdentityCandidate> pageCandidates = page(candidates, page, size);
        Map<UUID, AccessRoleAssignmentRuleQueryService.IdentitySnapshot> snapshots = queryService.loadIdentitySnapshots(
                tenantId,
                pageCandidates.stream().map(PreviewIdentityCandidate::identityId).toList(),
                identityTypes);

        List<AccessRoleAssignmentRulePreviewIdentityResponse> population = pageCandidates.stream()
                .map(candidate -> toIdentityResponse(candidate, snapshots.get(candidate.identityId()), normalizedGroups, entitlementRows))
                .toList();

        AccessRoleAssignmentRulePreviewResponse response = new AccessRoleAssignmentRulePreviewResponse();
        response.setValid(true);
        response.setValidationMessage(AccessRoleAssignmentRuleConstants.PREVIEW_VALIDATION_SUCCESSFUL);
        response.setRoleId(role.getId());
        response.setRoleName(resolveRoleDisplayName(role));
        response.setRuleId(sourceRuleId);
        response.setNormalizedConditionGroups(normalizedGroups);
        response.setIdentityTypes(identityTypes);
        response.setTotalMatched(matchedIdentityIds.size());
        response.setWillBeAdded(willAdd.size());
        response.setWillBeRemoved(willRemove.size());
        response.setAlreadyAssigned(alreadyAssigned.size());
        response.setTotalAffected(willAdd.size() + willRemove.size());
        response.setPage(page);
        response.setSize(size);
        response.setTotalPreviewRows(candidates.size());
        response.setPreviewPopulation(population);
        response.setEntitlementSummary(entitlementSummary);
        return response;
    }

    private Optional<AccessRoleAssignmentRule> resolveExistingRule(String tenantId, AccessRoleAssignmentRulePreviewRequest request) {
        if (request.getRuleId() != null) {
            return Optional.of(ruleRepository.findByIdAndTenantId(request.getRuleId(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_RULE_NOT_FOUND + ": " + request.getRuleId())));
        }
        return ruleRepository.findByTenantIdAndRoleId(tenantId, request.getRoleId());
    }

    private List<String> resolveIdentityTypes(AccessRoleAssignmentRulePreviewRequest request,
                                              Optional<AccessRoleAssignmentRule> existingRule) {
        if (request.getIdentityTypes() != null) {
            return normalizationService.normalizeIdentityTypes(request.getIdentityTypes(), false);
        }
        return normalizationService.normalizeIdentityTypes(
                existingRule.map(AccessRoleAssignmentRule::getIdentityTypes).orElse(null),
                true);
    }

    private List<AccessRoleAssignmentRuleConditionGroupDto> resolveInputGroups(AccessRoleAssignmentRulePreviewRequest request,
                                                                               Optional<AccessRoleAssignmentRule> existingRule) {
        if (request.getExpression() != null && !request.getExpression().isBlank()) {
            return expressionParser.parse(request.getExpression());
        }
        if (request.getConditionGroups() != null && !request.getConditionGroups().isEmpty()) {
            return request.getConditionGroups();
        }
        if (existingRule.isPresent()) {
            return fromNormalizedGroups(existingRule.get().getConditionGroups());
        }
        throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_CONDITION_GROUPS_REQUIRED);
    }

    @SuppressWarnings("unchecked")
    private List<AccessRoleAssignmentRuleConditionGroupDto> fromNormalizedGroups(List<Map<String, Object>> groups) {
        if (groups == null || groups.isEmpty()) {
            return List.of();
        }
        List<AccessRoleAssignmentRuleConditionGroupDto> result = new ArrayList<>();
        for (Map<String, Object> group : groups) {
            AccessRoleAssignmentRuleConditionGroupDto dto = new AccessRoleAssignmentRuleConditionGroupDto();
            dto.setGroupOperator(stringValue(group.get("groupOperator")));
            dto.setInterGroupOperator(stringValue(group.get("interGroupOperator")));
            dto.setNegated(group.get("negated") instanceof Boolean value && value);
            Object conditions = group.get("conditions");
            if (conditions instanceof List<?> conditionList) {
                dto.setConditions(conditionList.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> {
                            Map<String, Object> condition = (Map<String, Object>) item;
                            return new com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleAssignmentRuleConditionDto(
                                    stringValue(condition.get("field")),
                                    stringValue(condition.get("operator")),
                                    stringValue(condition.get("value")),
                                    condition.get("values") instanceof List<?> values
                                            ? values.stream().map(Objects::toString).toList()
                                            : null);
                        })
                        .toList());
            }
            Object subGroups = group.get("conditionGroups");
            if (subGroups instanceof List<?> subGroupList) {
                dto.setConditionGroups(fromNormalizedGroups(subGroupList.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> (Map<String, Object>) item)
                        .toList()));
            }
            result.add(dto);
        }
        return result;
    }

    private Set<UUID> loadCurrentlyAssignedIdentityIds(String tenantId, UUID roleId, UUID sourceRuleId) {
        List<IdentityRoleAssignment> assignments = sourceRuleId != null
                ? assignmentRepository.findByTenantIdAndAssignmentRuleIdAndStatus(tenantId, sourceRuleId, IdentityRoleAssignment.Status.ACTIVE)
                : assignmentRepository.findByTenantIdAndRoleIdAndStatus(tenantId, roleId, IdentityRoleAssignment.Status.ACTIVE);
        return assignments.stream()
                .filter(assignment -> roleId.equals(assignment.getRoleId()))
                .map(IdentityRoleAssignment::getIdentityId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private RoleEntitlementPreviewSummaryResponse buildEntitlementSummary(String tenantId, AccessRole role, boolean includeEntitlements) {
        List<RoleEntitlementClosure> mappings = roleEntitlementResolutionService.resolveEffectiveEntitlementMappings(tenantId, role.getId());
        Set<UUID> effectiveIds = mappings.stream().map(RoleEntitlementClosure::getEntitlementId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> directIds = mappings.stream().filter(RoleEntitlementClosure::isDirect).map(RoleEntitlementClosure::getEntitlementId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> inheritedIds = new LinkedHashSet<>(effectiveIds);
        inheritedIds.removeAll(directIds);

        RoleEntitlementPreviewSummaryResponse summary = new RoleEntitlementPreviewSummaryResponse();
        summary.setRoleId(role.getId());
        summary.setRoleName(resolveRoleDisplayName(role));
        summary.setEffectiveEntitlementCount(effectiveIds.size());
        summary.setDirectEntitlementCount(directIds.size());
        summary.setInheritedEntitlementCount(inheritedIds.size());
        summary.setEntitlements(includeEntitlements ? buildEntitlementRows(tenantId, mappings, null) : List.of());
        return summary;
    }

    private List<AccessRoleAssignmentRulePreviewEntitlementResponse> buildEntitlementRows(String tenantId,
                                                                                          List<RoleEntitlementClosure> mappings,
                                                                                          String previewAction) {
        if (mappings.isEmpty()) {
            return List.of();
        }

        Set<UUID> entitlementIds = mappings.stream()
                .map(RoleEntitlementClosure::getEntitlementId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, Entitlement> entitlements = entitlementRepository.findAllById(entitlementIds).stream()
                .filter(entitlement -> tenantId.equals(entitlement.getTenantId()))
                .collect(Collectors.toMap(
                        Entitlement::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Set<UUID> sourceRoleIds = mappings.stream()
                .map(RoleEntitlementClosure::getSourceRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, AccessRole> sourceRolesById = sourceRoleIds.isEmpty()
                ? Map.of()
                : accessRoleRepository.findByIdIn(sourceRoleIds).stream()
                .filter(role -> tenantId.equals(role.getTenantId()))
                .collect(Collectors.toMap(
                        AccessRole::getId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return mappings.stream()
                .sorted(Comparator.comparing(RoleEntitlementClosure::isDirect)
                        .reversed()
                        .thenComparing(RoleEntitlementClosure::getDepth))
                .map(mapping -> toEntitlementResponse(
                        mapping,
                        entitlements.get(mapping.getEntitlementId()),
                        sourceRolesById.get(mapping.getSourceRoleId()),
                        previewAction
                ))
                .toList();
    }

    private AccessRoleAssignmentRulePreviewEntitlementResponse toEntitlementResponse(RoleEntitlementClosure mapping,
                                                                                 Entitlement entitlement,
                                                                                 AccessRole sourceRole,
                                                                                 String previewAction) {
    AccessRoleAssignmentRulePreviewEntitlementResponse row = new AccessRoleAssignmentRulePreviewEntitlementResponse();
    row.setEntitlementId(mapping.getEntitlementId());
    row.setSourceRoleId(mapping.getSourceRoleId());
    row.setSourceRoleName(sourceRole != null ? resolveRoleDisplayName(sourceRole) : null);
    row.setDirect(mapping.isDirect());
    row.setDepth(mapping.getDepth());
    row.setPreviewAction(previewAction);

    if (entitlement != null) {
        row.setDisplayName(entitlement.getDisplayName());
        row.setResourceId(entitlement.getResourceId());
        row.setSourceUid(entitlement.getSourceUid());
        row.setObjectClass(entitlement.getObjectClass());
        row.setKind(entitlement.getKind());
        row.setRiskLevel(entitlement.getRiskLevel());
    }

    return row;
}

    private List<PreviewIdentityCandidate> buildPreviewCandidates(Set<UUID> willAdd,
                                                                  Set<UUID> willRemove,
                                                                  Set<UUID> alreadyAssigned) {
        List<PreviewIdentityCandidate> candidates = new ArrayList<>();
        willAdd.forEach(id -> candidates.add(new PreviewIdentityCandidate(id, ACTION_WILL_ADD, true, false)));
        willRemove.forEach(id -> candidates.add(new PreviewIdentityCandidate(id, ACTION_WILL_REMOVE, false, true)));
        alreadyAssigned.forEach(id -> candidates.add(new PreviewIdentityCandidate(id, ACTION_ALREADY_ASSIGNED, true, true)));
        return candidates;
    }

    private AccessRoleAssignmentRulePreviewIdentityResponse toIdentityResponse(PreviewIdentityCandidate candidate,
                                                                               AccessRoleAssignmentRuleQueryService.IdentitySnapshot snapshot,
                                                                               List<Map<String, Object>> normalizedGroups,
                                                                               List<AccessRoleAssignmentRulePreviewEntitlementResponse> entitlementRows) {
        AccessRoleAssignmentRulePreviewIdentityResponse row = new AccessRoleAssignmentRulePreviewIdentityResponse();
        row.setIdentityId(candidate.identityId());
        row.setIdentityDisplayName(snapshot != null ? snapshot.displayName() : null);
        row.setIdentityType(snapshot != null ? snapshot.identityType() : null);
        row.setPreviewAction(candidate.previewAction());
        row.setMatchesRule(candidate.matchesRule());
        row.setCurrentlyAssigned(candidate.currentlyAssigned());
        row.setFieldValues(snapshot != null ? snapshot.fieldValues() : Map.of());
        row.setMatchEvidence(queryService.buildEvidence(snapshot, normalizedGroups, candidate.matchesRule(), null));
        row.setEntitlements(entitlementRows.stream()
                .map(entitlement -> copyEntitlement(entitlement, candidate.previewAction()))
                .toList());
        return row;
    }

   private AccessRoleAssignmentRulePreviewEntitlementResponse copyEntitlement(AccessRoleAssignmentRulePreviewEntitlementResponse source,
                                                                           String previewAction) {
    AccessRoleAssignmentRulePreviewEntitlementResponse copy = new AccessRoleAssignmentRulePreviewEntitlementResponse();
    copy.setEntitlementId(source.getEntitlementId());
    copy.setDisplayName(source.getDisplayName());
    copy.setResourceId(source.getResourceId());
    copy.setSourceUid(source.getSourceUid());
    copy.setObjectClass(source.getObjectClass());
    copy.setKind(source.getKind());
    copy.setRiskLevel(source.getRiskLevel());
    copy.setSourceRoleId(source.getSourceRoleId());
    copy.setSourceRoleName(source.getSourceRoleName());
    copy.setDirect(source.isDirect());
    copy.setDepth(source.getDepth());
    copy.setPreviewAction(previewAction);
    return copy;
}

    private List<PreviewIdentityCandidate> page(List<PreviewIdentityCandidate> candidates, int page, int size) {
        int from = Math.min(page * size, candidates.size());
        int to = Math.min(from + size, candidates.size());
        return candidates.subList(from, to);
    }

    private Set<UUID> difference(Set<UUID> left, Set<UUID> right) {
        Set<UUID> result = new LinkedHashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private Set<UUID> intersection(Set<UUID> left, Set<UUID> right) {
        Set<UUID> result = new LinkedHashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private AccessRole findTenantRoleOrThrow(String tenantId, UUID roleId) {
        return accessRoleRepository.findById(roleId)
                .filter(role -> tenantId.equals(role.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_ROLE_NOT_FOUND + ": " + roleId));
    }

    private String resolveRoleDisplayName(AccessRole role) {
        return role.getDisplayName() != null && !role.getDisplayName().isBlank() ? role.getDisplayName() : role.getName();
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 20;
        }
        return Math.min(size, 200);
    }

    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
        }
    }

    private record PreviewIdentityCandidate(UUID identityId,
                                            String previewAction,
                                            boolean matchesRule,
                                            boolean currentlyAssigned) {
    }
}

