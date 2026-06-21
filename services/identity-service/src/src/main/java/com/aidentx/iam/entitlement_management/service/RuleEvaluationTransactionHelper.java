package com.aidentx.iam.entitlement_management.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.model.dto.RuleEvaluationResult;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignmentRule;
import com.aidentx.iam.entitlement_management.model.enums.RuleOperator;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRuleRepository;
import com.aidentx.iam.entitlement_management.util.LikePatternEscaper;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.enums.IdentityStatus;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the transactional execution of a single rule evaluation with
 * integrated execution history collection.
 *
 * <p>Separated from {@link RuleEvaluationEngine} so that
 * {@code @Transactional(propagation = REQUIRES_NEW)} works correctly
 * via Spring AOP proxy (self-invocation within the same bean bypasses proxies).
 *
 * <p>The evaluation pipeline for each rule:
 * <ol>
 *   <li><b>Match:</b> Query identities matching rule conditions (paginated)</li>
 *   <li><b>Resolve shadows:</b> Bulk load shadow/account records for matched identities</li>
 *   <li><b>Check existing:</b> Bulk check which shadows already have the entitlement assigned</li>
 *   <li><b>Assign:</b> Upsert new assignments for shadows that don't have them yet</li>
 *   <li><b>Revoke:</b> Bulk delete assignments for identities that no longer match</li>
 *   <li><b>Record history:</b> Throughout the pipeline, record identity-level details
 *       in the {@link ExecutionHistoryCollector} for audit trail</li>
 * </ol>
 *
 * <p>Conditions are organized into groups. Within each group, conditions are
 * combined using the group's operator. Groups chain left-to-right. Each group
 * can optionally be negated (NOT).
 *
 * <p>Evaluation order:
 * <pre>
 *   groups = [
 *     { groupOperator: "AND", negated: false, conditions: [A, B] },
 *     { groupOperator: "OR",  negated: true,  conditions: [C, D] }
 *   ]
 *
 *   Step 1: group1 = (A AND B)              [negated=false → unchanged]
 *   Step 2: group2 = NOT(C OR D)            [negated=true → wrapped in NOT]
 *   Step 3: result  = group1 AND group2     [group1's groupOperator connects them]
 *           = (A AND B) AND NOT(C OR D)
 * </pre>
 *
 * <p>For each identity processed, a {@link MatchEvidenceBuilder} captures the actual
 * field values and per-group, per-condition evaluation results. This evidence is
 * stored as JSONB in the execution detail record — an immutable compliance receipt
 * proving why the decision was made even if rule conditions or identity data change later.
 */
@Service
public class RuleEvaluationTransactionHelper {

    private final EntityManager entityManager;
    private final EntitlementAssignmentRuleRepository ruleRepository;
    private final IdentityFieldRegistry fieldRegistry;
    private final EntitlementAssignmentRuleNormalizationService normalizationService;
    private final IUnifiedLogger logger;

    @Autowired
    public RuleEvaluationTransactionHelper(EntityManager entityManager,
                                           EntitlementAssignmentRuleRepository ruleRepository,
                                           IdentityFieldRegistry fieldRegistry,
                                           EntitlementAssignmentRuleNormalizationService normalizationService,
                                           UnifiedLoggerFactory loggerFactory) {
        this.entityManager = entityManager;
        this.ruleRepository = ruleRepository;
        this.fieldRegistry = fieldRegistry;
        this.normalizationService = normalizationService;
        this.logger = loggerFactory.getLogger(getClass(), RuleEvaluationConstants.LOGGER_MODULE_NAME);
    }

    // ===================================================================
    //  Main Evaluation Method
    // ===================================================================

    /**
     * Evaluates a single rule inside its own transaction with full
     * execution history collection.
     *
     * <p>This method performs the complete evaluation pipeline:
     * <ol>
     *   <li>Queries matched identities in paginated chunks</li>
     *   <li>For each chunk, resolves shadows, checks existing assignments,
     *       creates new assignments, and records identity-level details</li>
     *   <li>After all chunks, revokes stale assignments</li>
     *   <li>Updates the rule's lastEvaluatedAt timestamp</li>
     * </ol>
     *
     * <p>The collector is populated throughout the pipeline. The caller
     * ({@link RuleEvaluationEngine}) is responsible for flushing the
     * collector to the database after this method returns.
     *
     * @param tenantId  the tenant identifier
     * @param rule      the rule to evaluate
     * @param collector the in-memory collector for execution history
     * @return evaluation result with aggregate counts
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RuleEvaluationResult evaluateRuleInTransaction(String tenantId,
                                                          EntitlementAssignmentRule rule,
                                                          ExecutionHistoryCollector collector) {
        long startTime = System.currentTimeMillis();

        logger.info()
                .message(RuleEvaluationConstants.LOG_EVALUATE_RULE_START)
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                .detail(RuleEvaluationConstants.DETAIL_ENTITLEMENT_ID, rule.getEntitlementId().toString())
                .detail(RuleEvaluationConstants.DETAIL_RESOURCE_ID, rule.getResourceId().toString())
                .detail(RuleEvaluationConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setRuleId(rule.getId());
        result.setRuleName(rule.getName());

        if (!rule.isEnabled()) {
            collector.setIdentitiesMatched(0);

            result.setIdentitiesMatched(0);
            result.setAssignmentsCreated(0);
            result.setSkippedNoShadow(0);

            int revoked = bulkRevokeStaleAssignments(tenantId, rule, Collections.emptySet());
            result.setAssignmentsRevoked(revoked);
            collector.addBulkRevocationCount(revoked);

            rule.setLastEvaluatedAt(Instant.now().toEpochMilli());
            ruleRepository.save(rule);

            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);

            logger.info()
                    .message(RuleEvaluationConstants.LOG_EVALUATE_RULE_COMPLETE)
                    .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                    .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                    .detail(RuleEvaluationConstants.DETAIL_MATCHED_COUNT, String.valueOf(0))
                    .detail(RuleEvaluationConstants.DETAIL_CREATED_COUNT, String.valueOf(0))
                    .detail(RuleEvaluationConstants.DETAIL_REVOKED_COUNT, String.valueOf(revoked))
                    .detail(RuleEvaluationConstants.DETAIL_SKIPPED_NO_SHADOW, String.valueOf(0))
                    .detail(RuleEvaluationConstants.DETAIL_DURATION_MS, String.valueOf(duration))
                    .log();

            return result;
        }

        List<String> identityTypes = normalizationService.normalizeIdentityTypes(rule.getIdentityTypes(), true);
        List<Class<? extends Identity>> identityClasses = normalizationService.resolveIdentityClasses(identityTypes);

        Set<UUID> allMatchedIdentityIds = new HashSet<>();
        int totalCreated = 0;
        int totalSkippedNoShadow = 0;
        for (Class<? extends Identity> identityClass : identityClasses) {
            int pageNumber = 0;
            Page<UUID> identityIdPage;
            do {
                Pageable pageable = PageRequest.of(pageNumber, RuleEvaluationConstants.IDENTITY_CHUNK_SIZE);

                logger.debug()
                        .message(RuleEvaluationConstants.LOG_IDENTITY_CHUNK_PROCESSING)
                        .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                        .detail(RuleEvaluationConstants.DETAIL_IDENTITY_CLASS, identityClass.getSimpleName())
                        .detail(RuleEvaluationConstants.DETAIL_PAGE, String.valueOf(pageNumber))
                        .detail(RuleEvaluationConstants.DETAIL_CHUNK_SIZE,
                                String.valueOf(RuleEvaluationConstants.IDENTITY_CHUNK_SIZE))
                        .log();

                identityIdPage = findMatchingIdentityIds(
                        tenantId, identityClass, rule.getConditions(), pageable);

                List<UUID> chunkIds = identityIdPage.getContent();
                if (chunkIds.isEmpty()) {
                    break;
                }

                allMatchedIdentityIds.addAll(chunkIds);

                Map<UUID, IdentitySnapshot> identitySnapshots = loadIdentitySnapshots(
                        tenantId, chunkIds, identityClass);

                Map<UUID, UUID> identityToShadow = bulkLoadShadows(
                        tenantId, chunkIds, rule.getResourceId());

                logger.debug()
                        .message(RuleEvaluationConstants.LOG_SHADOW_BULK_LOOKUP_COMPLETE)
                        .detail(RuleEvaluationConstants.DETAIL_CHUNK_INDEX, String.valueOf(pageNumber))
                        .detail(RuleEvaluationConstants.DETAIL_SHADOW_COUNT,
                                String.valueOf(identityToShadow.size()))
                        .log();

                Set<UUID> shadowIdsInChunk = new HashSet<>(identityToShadow.values());
                Set<UUID> alreadyAssignedShadowIds = bulkCheckExistingAssignments(
                        tenantId, shadowIdsInChunk, rule.getEntitlementId());
                Set<UUID> existingPendingIdentityIds = bulkCheckExistingPendingAssignments(
                        tenantId, chunkIds, rule.getEntitlementId());

                logger.debug()
                        .message(RuleEvaluationConstants.LOG_ASSIGNMENT_CHECK_EXISTING)
                        .detail(RuleEvaluationConstants.DETAIL_CHUNK_INDEX, String.valueOf(pageNumber))
                        .detail(RuleEvaluationConstants.DETAIL_EXISTING_ASSIGNMENT_COUNT,
                                String.valueOf(alreadyAssignedShadowIds.size()))
                        .log();

                int chunkCreated = 0;
                int chunkSkipped = 0;

                for (UUID identityId : chunkIds) {
                    IdentitySnapshot snapshot = identitySnapshots.get(identityId);
                    String displayName = snapshot != null ? snapshot.displayName() : "Unknown";
                    UUID shadowId = identityToShadow.get(identityId);

                    if (shadowId == null) {
                        Map<String, Object> evidence = buildEvidence(
                                snapshot, rule.getConditions(), identityClass, false, null);
                        if (!existingPendingIdentityIds.contains(identityId)) {
                            upsertPendingAssignment(tenantId, identityId, rule.getEntitlementId(),
                                    rule.getResourceId(), rule.getId());
                            collector.recordAssigned(identityId, displayName, null, evidence);
                            chunkCreated++;
                        } else {
                            collector.recordAlreadyAssigned();
                        }
                        continue;
                    }

                    if (alreadyAssignedShadowIds.contains(shadowId)) {
                        collector.recordAlreadyAssigned();
                        deletePendingAssignment(tenantId, identityId, rule.getEntitlementId(), rule.getId());
                        continue;
                    }

                    upsertAssignment(tenantId, shadowId, identityId, rule.getEntitlementId(),
                            rule.getResourceId(), rule.getId());
                    deletePendingAssignment(tenantId, identityId, rule.getEntitlementId(), rule.getId());

                    Map<String, Object> evidence = buildEvidence(
                            snapshot, rule.getConditions(), identityClass, true, shadowId);
                    collector.recordAssigned(identityId, displayName, shadowId, evidence);
                    chunkCreated++;
                }

                totalCreated += chunkCreated;
                totalSkippedNoShadow += chunkSkipped;
                pageNumber++;

            } while (identityIdPage.hasNext());
        }

        collector.setIdentitiesMatched(allMatchedIdentityIds.size());

        result.setIdentitiesMatched(allMatchedIdentityIds.size());
        result.setAssignmentsCreated(totalCreated);
        result.setSkippedNoShadow(totalSkippedNoShadow);

        int revoked = bulkRevokeStaleAssignments(tenantId, rule, allMatchedIdentityIds);
        result.setAssignmentsRevoked(revoked);
        collector.addBulkRevocationCount(revoked);

        rule.setLastEvaluatedAt(Instant.now().toEpochMilli());
        ruleRepository.save(rule);

        long duration = System.currentTimeMillis() - startTime;
        result.setDurationMs(duration);

        logger.info()
                .message(RuleEvaluationConstants.LOG_EVALUATE_RULE_COMPLETE)
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                .detail(RuleEvaluationConstants.DETAIL_RULE_NAME, rule.getName())
                .detail(RuleEvaluationConstants.DETAIL_MATCHED_COUNT,
                        String.valueOf(result.getIdentitiesMatched()))
                .detail(RuleEvaluationConstants.DETAIL_CREATED_COUNT,
                        String.valueOf(result.getAssignmentsCreated()))
                .detail(RuleEvaluationConstants.DETAIL_REVOKED_COUNT,
                        String.valueOf(result.getAssignmentsRevoked()))
                .detail(RuleEvaluationConstants.DETAIL_SKIPPED_NO_SHADOW,
                        String.valueOf(result.getSkippedNoShadow()))
                .detail(RuleEvaluationConstants.DETAIL_DURATION_MS, String.valueOf(duration))
                .log();

        return result;
    }

    // ===================================================================
    //  Identity Matching — Grouped Predicate Building
    // ===================================================================

    /**
     * Finds identity IDs matching rule conditions using grouped evaluation.
     *
     * <p>Produces a paginated result of identity UUIDs that match the tenant,
     * are in ACTIVE status, and satisfy the rule's condition groups.
     *
     * @param tenantId      the tenant identifier
     * @param identityClass the JPA entity class to query
     * @param conditions    the conditions from the rule entity (grouped or legacy format)
     * @param pageable      pagination parameters
     * @param <T>           identity type
     * @return page of matching identity UUIDs
     */
    private <T extends Identity> Page<UUID> findMatchingIdentityIds(
            String tenantId,
            Class<T> identityClass,
            List<Map<String, Object>> conditions,
            Pageable pageable) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<T> countRoot = countQuery.from(identityClass);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildWhereClause(cb, countRoot, tenantId, identityClass, conditions));
        long total = entityManager.createQuery(countQuery).getSingleResult();

        if (total == 0) {
            return Page.empty(pageable);
        }

        CriteriaQuery<UUID> dataQuery = cb.createQuery(UUID.class);
        Root<T> dataRoot = dataQuery.from(identityClass);
        dataQuery.select(dataRoot.get("id"));
        dataQuery.where(buildWhereClause(cb, dataRoot, tenantId, identityClass, conditions));

        List<UUID> ids = entityManager.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new org.springframework.data.domain.PageImpl<>(ids, pageable, total);
    }

    /**
     * Builds the complete WHERE clause with grouped condition evaluation.
     *
     * <p>Always includes tenant isolation and ACTIVE status filter.
     * Then chains condition groups left-to-right.
     *
     * @param cb            the criteria builder
     * @param root          the query root
     * @param tenantId      the tenant identifier
     * @param identityClass the identity entity class
     * @param conditions    the conditions from the rule entity
     * @param <T>           identity type
     * @return combined predicate: (tenant AND status AND (grouped conditions))
     */
    private <T extends Identity> Predicate buildWhereClause(
            CriteriaBuilder cb,
            Root<T> root,
            String tenantId,
            Class<T> identityClass,
            List<Map<String, Object>> conditions) {

        Predicate tenantPredicate = cb.equal(root.get("tenantId"), tenantId);
        Predicate statusPredicate = cb.equal(root.get("status"), IdentityStatus.ACTIVE);
        Predicate conditionsPredicate = buildGroupedPredicate(cb, root, identityClass, conditions);

        return cb.and(tenantPredicate, statusPredicate, conditionsPredicate);
    }

    /**
     * Recursively parses a condition group map into a GroupedCondition record.
     *
     * @param groupMap the raw JSONB map for this group
     * @return parsed GroupedCondition with nested sub-groups
     */
    @SuppressWarnings("unchecked")
    private GroupedCondition parseGroupedCondition(Map<String, Object> groupMap) {
        String groupOp = groupMap.get("groupOperator") != null
                ? groupMap.get("groupOperator").toString().toUpperCase()
                : RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR;

        String interGroupOp = groupMap.get("interGroupOperator") != null
                ? groupMap.get("interGroupOperator").toString().toUpperCase()
                : RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR;

        boolean negated = groupMap.get("negated") instanceof Boolean b && b;

        // Parse leaf conditions
        List<Map<String, Object>> conditions = new ArrayList<>();
        Object conditionsObj = groupMap.get("conditions");
        if (conditionsObj instanceof List<?> condList) {
            for (Object item : condList) {
                if (item instanceof Map<?, ?> condMap) {
                    conditions.add((Map<String, Object>) condMap);
                }
            }
        }

        // Recursively parse sub-groups
        List<GroupedCondition> subGroups = new ArrayList<>();
        Object subGroupsObj = groupMap.get("conditionGroups");
        if (subGroupsObj instanceof List<?> subGroupList) {
            for (Object item : subGroupList) {
                if (item instanceof Map<?, ?> subGroupMap) {
                    subGroups.add(parseGroupedCondition((Map<String, Object>) subGroupMap));
                }
            }
        }

        return new GroupedCondition(groupOp, interGroupOp, negated, conditions, subGroups);
    }

    /**
     * Builds predicates by evaluating condition groups left-to-right with recursive nesting.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Parse each top-level group into a GroupedCondition (with nested sub-groups)</li>
     *   <li>For each group, recursively build its predicate via buildSingleGroupPredicate</li>
     *   <li>Chain top-level groups using the previous group's interGroupOperator</li>
     * </ol>
     *
     * @param cb            the criteria builder
     * @param root          the query root
     * @param identityClass the identity entity class
     * @param conditionData the conditions from the rule entity
     * @param <T>           identity type
     * @return the combined predicate for all condition groups
     */
    @SuppressWarnings("unchecked")
    private <T extends Identity> Predicate buildGroupedPredicate(
            CriteriaBuilder cb,
            Root<T> root,
            Class<T> identityClass,
            List<Map<String, Object>> conditionData) {

        if (conditionData == null || conditionData.isEmpty()) {
            return cb.conjunction();
        }

        // Detect legacy flat format (no groupOperator key)
        if (conditionData.get(0).containsKey("field") && !conditionData.get(0).containsKey("groupOperator")) {
            return buildLegacyFlatPredicate(cb, root, identityClass, conditionData);
        }

        // Parse all top-level groups recursively
        List<GroupedCondition> groups = conditionData.stream()
                .map(groupMap -> parseGroupedCondition((Map<String, Object>) groupMap))
                .collect(Collectors.toList());

        if (groups.isEmpty()) {
            return cb.conjunction();
        }

        // Build first group's predicate
        Predicate accumulator = buildSingleGroupPredicate(cb, root, identityClass, groups.get(0));

        // Chain subsequent groups using the PREVIOUS group's interGroupOperator
        for (int i = 1; i < groups.size(); i++) {
            String connectingOp = groups.get(i - 1).interGroupOperator();
            Predicate groupPred = buildSingleGroupPredicate(cb, root, identityClass, groups.get(i));

            if ("OR".equals(connectingOp)) {
                accumulator = cb.or(accumulator, groupPred);
            } else {
                accumulator = cb.and(accumulator, groupPred);
            }
        }

        return accumulator;
    }


    /**
     * Builds a predicate for a single condition group, recursively handling sub-groups.
     *
     * <p>Within a group, leaf conditions and sub-group predicates are combined
     * using the group's {@code groupOperator}. If the group is negated, the
     * combined result is wrapped in NOT.
     *
     * @param cb            the criteria builder
     * @param root          the query root
     * @param identityClass the identity entity class
     * @param group         the parsed condition group
     * @param <T>           identity type
     * @return the predicate for this group
     */
    private <T extends Identity> Predicate buildSingleGroupPredicate(
            CriteriaBuilder cb,
            Root<T> root,
            Class<T> identityClass,
            GroupedCondition group) {

        List<Predicate> predicates = new ArrayList<>();

        // Build predicates for leaf conditions
        for (Map<String, Object> condition : group.conditions()) {
            Predicate condPred = buildSingleConditionPredicate(cb, root, identityClass, condition);
            if (condPred != null) {
                predicates.add(condPred);
            }
        }

        // Recursively build predicates for sub-groups
        if (!group.subGroups().isEmpty()) {
            // Chain sub-groups using previous sub-group's interGroupOperator
            Predicate subAccumulator = buildSingleGroupPredicate(cb, root, identityClass, group.subGroups().get(0));

            for (int i = 1; i < group.subGroups().size(); i++) {
                String connectingOp = group.subGroups().get(i - 1).interGroupOperator();
                Predicate subPred = buildSingleGroupPredicate(cb, root, identityClass, group.subGroups().get(i));

                if ("OR".equals(connectingOp)) {
                    subAccumulator = cb.or(subAccumulator, subPred);
                } else {
                    subAccumulator = cb.and(subAccumulator, subPred);
                }
            }

            predicates.add(subAccumulator);
        }

        if (predicates.isEmpty()) {
            return group.negated() ? cb.disjunction() : cb.conjunction();
        }

        // Combine all predicates (conditions + sub-groups) using groupOperator
        Predicate combined;
        if ("OR".equals(group.groupOperator())) {
            combined = cb.or(predicates.toArray(new Predicate[0]));
        } else {
            combined = cb.and(predicates.toArray(new Predicate[0]));
        }

        // Apply negation
        if (group.negated()) {
            combined = cb.not(combined);
        }

        return combined;
    }


    /**
     * Backward-compatible predicate builder for legacy flat conditions.
     *
     * <p>Used when existing rules stored in the old flat format have not
     * been migrated to the grouped format yet. Uses per-condition
     * logicalOperator for left-to-right chaining.
     *
     * @param cb            the criteria builder
     * @param root          the query root
     * @param identityClass the identity entity class
     * @param conditions    the flat condition list
     * @param <T>           identity type
     * @return the combined predicate
     */
    private <T extends Identity> Predicate buildLegacyFlatPredicate(
            CriteriaBuilder cb,
            Root<T> root,
            Class<T> identityClass,
            List<Map<String, Object>> conditions) {

        if (conditions.isEmpty()) {
            return cb.conjunction();
        }

        Predicate accumulator = buildSingleConditionPredicate(
                cb, root, identityClass, conditions.get(0));

        for (int i = 1; i < conditions.size(); i++) {
            String logicalOp = getLogicalOperator(conditions.get(i - 1));
            Predicate current = buildSingleConditionPredicate(
                    cb, root, identityClass, conditions.get(i));

            if ("OR".equalsIgnoreCase(logicalOp)) {
                accumulator = cb.or(accumulator, current);
            } else {
                accumulator = cb.and(accumulator, current);
            }
        }

        return accumulator;
    }

    /**
     * Builds a predicate for a single condition map.
     *
     * <p>Resolves the field name to the actual entity attribute,
     * parses the operator, and delegates to {@link #buildFieldPredicate}.
     *
     * @param cb            the criteria builder
     * @param root          the query root
     * @param identityClass the identity entity class (for field resolution)
     * @param condition     the condition map with field, operator, value, values
     * @param <T>           identity type
     * @return the JPA Criteria predicate for this single condition
     */
    private <T extends Identity> Predicate buildSingleConditionPredicate(
            CriteriaBuilder cb,
            Root<T> root,
            Class<T> identityClass,
            Map<String, Object> condition) {

        String rawField = (String) condition.get("field");
        String operatorStr = (String) condition.get("operator");
        Object value = condition.get("value");
        Object values = condition.get("values");

        String resolvedField = fieldRegistry.resolveField(rawField, identityClass);
        RuleOperator operator = RuleOperator.fromString(operatorStr);

        return buildFieldPredicate(cb, root, resolvedField, operator, value, values);
    }

    /**
     * Extracts the logicalOperator from a legacy flat condition map.
     * Defaults to AND if not specified or invalid.
     *
     * @param condition the condition map
     * @return "AND" or "OR"
     */
    private String getLogicalOperator(Map<String, Object> condition) {
        Object logicalOp = condition.get("logicalOperator");
        if (logicalOp == null) {
            return RuleEvaluationConstants.DEFAULT_LOGICAL_OPERATOR;
        }
        String op = logicalOp.toString().toUpperCase();
        if (!"AND".equals(op) && !"OR".equals(op)) {
            return RuleEvaluationConstants.DEFAULT_LOGICAL_OPERATOR;
        }
        return op;
    }

    /**
     * Builds a single field predicate with case-insensitive comparison
     * and LIKE wildcard injection prevention.
     *
     * <p>All string comparisons use {@code UPPER()} for case-insensitive matching.
     * LIKE patterns are escaped via {@link LikePatternEscaper} to prevent
     * user input like "%" or "_" from acting as SQL wildcards.
     *
     * @param cb       the criteria builder
     * @param root     the query root
     * @param field    the resolved entity attribute name
     * @param operator the rule operator enum
     * @param value    the single comparison value (for EQUALS, CONTAINS, etc.)
     * @param values   the list of values (for IN, NOT_IN)
     * @return the JPA Criteria predicate
     */
    private Predicate buildFieldPredicate(CriteriaBuilder cb, Root<?> root,
                                          String field, RuleOperator operator,
                                          Object value, Object values) {
        Path<String> path = root.get(field);
        String strValue = value != null ? value.toString() : "";

        return switch (operator) {
            case EQUALS -> cb.equal(cb.upper(path), strValue.toUpperCase());
            case NOT_EQUALS -> cb.notEqual(cb.upper(path), strValue.toUpperCase());
            case CONTAINS -> cb.like(
                    cb.upper(path),
                    "%" + LikePatternEscaper.escape(strValue).toUpperCase() + "%",
                    RuleEvaluationConstants.LIKE_ESCAPE_CHAR);
            case STARTS_WITH -> cb.like(
                    cb.upper(path),
                    LikePatternEscaper.escape(strValue).toUpperCase() + "%",
                    RuleEvaluationConstants.LIKE_ESCAPE_CHAR);
            case ENDS_WITH -> cb.like(
                    cb.upper(path),
                    "%" + LikePatternEscaper.escape(strValue).toUpperCase(),
                    RuleEvaluationConstants.LIKE_ESCAPE_CHAR);
            case IN -> {
                List<String> inValues = resolveListValues(value, values);
                yield cb.upper(path).in(inValues.stream().map(String::toUpperCase).toList());
            }
            case NOT_IN -> {
                List<String> notInValues = resolveListValues(value, values);
                yield cb.not(cb.upper(path).in(notInValues.stream().map(String::toUpperCase).toList()));
            }
        };
    }

    /**
     * Builds match evidence for a single identity against a rule's conditions.
     * Supports both grouped (with nesting) and legacy flat formats.
     *
     * @param snapshot      the identity's field snapshot
     * @param conditions    the rule's condition data from JSONB
     * @param identityClass the identity entity class
     * @param shadowFound   whether a shadow account was found
     * @param shadowId      the shadow account ID (if found)
     * @return evidence map for JSONB serialization
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildEvidence(IdentitySnapshot snapshot,
                                              List<Map<String, Object>> conditions,
                                              Class<? extends Identity> identityClass,
                                              boolean shadowFound,
                                              UUID shadowId) {

        MatchEvidenceBuilder builder = new MatchEvidenceBuilder();

        Map<String, Object> fieldValues = snapshot != null
                ? snapshot.fieldValues() : Collections.emptyMap();

        // Add all identity fields to evidence
        for (Map.Entry<String, Object> entry : fieldValues.entrySet()) {
            builder.addField(entry.getKey(), entry.getValue());
        }

        // Track overall match across all groups
        boolean computedOverallMatch = true;

        if (conditions != null && !conditions.isEmpty()) {
            boolean isGrouped = conditions.get(0).containsKey("groupOperator");

            if (isGrouped) {
                computedOverallMatch = buildGroupedEvidence(builder, conditions, fieldValues, identityClass);
            } else {
                computedOverallMatch = buildLegacyFlatEvidence(builder, conditions, fieldValues, identityClass);
            }
        }

        builder.overallMatch(computedOverallMatch);
        builder.shadowFound(shadowFound, shadowId);

        return builder.build();
    }

    /**
     * Builds evidence for the grouped condition format with recursive nesting support.
     *
     * <p>Uses {@code evaluateGroupEvidence} to recursively evaluate each top-level
     * group and its nested sub-groups. Then chains top-level group results using
     * {@code interGroupOperator} to compute the overall match.
     *
     * @param builder       the evidence builder
     * @param conditionData the grouped conditions from the rule
     * @param fieldValues   the identity's field values
     * @param identityClass the identity entity class
     * @return true if the identity matches all groups combined
     */
    @SuppressWarnings("unchecked")
    private boolean buildGroupedEvidence(MatchEvidenceBuilder builder,
                                         List<Map<String, Object>> conditionData,
                                         Map<String, Object> fieldValues,
                                         Class<? extends Identity> identityClass) {

        List<GroupEvidenceResult> groupResults = new ArrayList<>();

        for (int gi = 0; gi < conditionData.size(); gi++) {
            Map<String, Object> groupMap = conditionData.get(gi);
            GroupEvidenceResult result = evaluateGroupEvidence(
                    groupMap, gi, fieldValues, identityClass, builder);
            groupResults.add(result);
        }

        // Chain top-level group results using interGroupOperator
        if (groupResults.isEmpty()) {
            return true;
        }

        boolean overall = groupResults.get(0).matched();

        for (int i = 1; i < groupResults.size(); i++) {
            String connectingOp = groupResults.get(i - 1).interGroupOperator();
            boolean currentGroupMatched = groupResults.get(i).matched();

            if ("OR".equals(connectingOp)) {
                overall = overall || currentGroupMatched;
            } else {
                overall = overall && currentGroupMatched;
            }
        }

        return overall;
    }

    /**
     * Internal record for tracking per-group evidence results during chaining.
     *
     * @param interGroupOperator the inter-group operator (used to connect to the next group)
     * @param matched            whether this group matched (after negation)
     */
    private record GroupEvidenceResult(String interGroupOperator, boolean matched) {
    }

    /**
     * Builds evidence for the legacy flat condition format.
     *
     * <p>Wraps all conditions into a single group with AND operator
     * for evidence display purposes.
     *
     * @param builder       the evidence builder
     * @param conditions    the flat conditions from the rule
     * @param fieldValues   the identity's field values
     * @param identityClass the identity entity class
     * @return true if all conditions matched
     */
    private boolean buildLegacyFlatEvidence(MatchEvidenceBuilder builder,
                                            List<Map<String, Object>> conditions,
                                            Map<String, Object> fieldValues,
                                            Class<? extends Identity> identityClass) {

        MatchEvidenceBuilder.GroupEvidenceBuilder groupBuilder =
                builder.startGroup(0, "AND", false);

        boolean allMatched = true;

        for (Map<String, Object> condition : conditions) {
            String rawField = (String) condition.get("field");
            String operator = (String) condition.get("operator");
            Object expectedValue = condition.get("value");
            Object expectedValues = condition.get("values");

            String resolvedField = fieldRegistry.resolveField(rawField, identityClass);
            Object actualValue = fieldValues.get(resolvedField);

            boolean matched = evaluateConditionInMemory(
                    actualValue, operator, expectedValue, expectedValues);

            Object displayExpected = expectedValues != null ? expectedValues : expectedValue;
            groupBuilder.addConditionResult(resolvedField, operator,
                    displayExpected, actualValue, matched);

            if (!matched) allMatched = false;
        }

        groupBuilder.setGroupMatched(allMatched);
        builder.addGroupResult(groupBuilder);

        return allMatched;
    }

    /**
     * Recursively evaluates a single condition group's evidence, including nested sub-groups.
     *
     * <p>Within a group:
     * <ol>
     *   <li>Evaluate each leaf condition in memory</li>
     *   <li>Recursively evaluate each sub-group</li>
     *   <li>Combine all results (conditions + sub-groups) using groupOperator</li>
     *   <li>Apply negation if the group is negated</li>
     * </ol>
     *
     * @param groupMap      the raw JSONB map for this group
     * @param groupIndex    the index of this group among its siblings
     * @param fieldValues   the identity's field values
     * @param identityClass the identity entity class
     * @return a pair of (built GroupEvidenceBuilder, boolean matched)
     */
    @SuppressWarnings("unchecked")
    private GroupEvidenceResult evaluateGroupEvidence(
            Map<String, Object> groupMap,
            int groupIndex,
            Map<String, Object> fieldValues,
            Class<? extends Identity> identityClass,
            MatchEvidenceBuilder builder) {

        String groupOp = groupMap.get("groupOperator") != null
                ? groupMap.get("groupOperator").toString().toUpperCase()
                : RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR;

        String interGroupOp = groupMap.get("interGroupOperator") != null
                ? groupMap.get("interGroupOperator").toString().toUpperCase()
                : RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR;

        boolean negated = groupMap.get("negated") instanceof Boolean b && b;

        MatchEvidenceBuilder.GroupEvidenceBuilder groupBuilder =
                builder.startGroup(groupIndex, groupOp, negated);

        // --- Evaluate leaf conditions ---

        List<Map<String, Object>> conditions = new ArrayList<>();
        Object conditionsObj = groupMap.get("conditions");
        if (conditionsObj instanceof List<?> condList) {
            for (Object item : condList) {
                if (item instanceof Map<?, ?> condMap) {
                    conditions.add((Map<String, Object>) condMap);
                }
            }
        }

        // Track all individual results (conditions + sub-groups) for combining
        List<Boolean> allResults = new ArrayList<>();

        for (Map<String, Object> cond : conditions) {
            String rawField = (String) cond.get("field");
            String operator = (String) cond.get("operator");
            Object expectedValue = cond.get("value");
            Object expectedValues = cond.get("values");

            String resolvedField = fieldRegistry.resolveField(rawField, identityClass);
            Object actualValue = fieldValues.get(resolvedField);

            boolean condMatched = evaluateConditionInMemory(
                    actualValue, operator, expectedValue, expectedValues);

            Object displayExpected = expectedValues != null ? expectedValues : expectedValue;
            groupBuilder.addConditionResult(resolvedField, operator,
                    displayExpected, actualValue, condMatched);

            allResults.add(condMatched);
        }

        // --- Recursively evaluate sub-groups ---

        List<Map<String, Object>> subGroups = new ArrayList<>();
        Object subGroupsObj = groupMap.get("conditionGroups");
        if (subGroupsObj instanceof List<?> subGroupList) {
            for (Object item : subGroupList) {
                if (item instanceof Map<?, ?> subGroupMap) {
                    subGroups.add((Map<String, Object>) subGroupMap);
                }
            }
        }

        if (!subGroups.isEmpty()) {
            // Evaluate sub-groups and chain them using previous sub-group's interGroupOperator
            List<GroupEvidenceResult> subGroupResults = new ArrayList<>();

            for (int si = 0; si < subGroups.size(); si++) {
                // Create a temporary builder for sub-group evidence
                MatchEvidenceBuilder subBuilder = new MatchEvidenceBuilder();
                GroupEvidenceResult subResult = evaluateGroupEvidence(
                        subGroups.get(si), si, fieldValues, identityClass, subBuilder);

                subGroupResults.add(subResult);

                // Build sub-group evidence map and add to parent group
                MatchEvidenceBuilder.GroupEvidenceBuilder subGroupBuilder =
                        builder.startGroup(si,
                                subGroups.get(si).get("groupOperator") != null
                                        ? subGroups.get(si).get("groupOperator").toString().toUpperCase()
                                        : RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR,
                                subGroups.get(si).get("negated") instanceof Boolean nb && nb);
                subGroupBuilder.setGroupMatched(subResult.matched());

                // Re-evaluate sub-group to populate its evidence (conditions + nested)
                // We use the already-computed match result
                groupBuilder.addSubGroupResult(buildSubGroupEvidenceMap(
                        subGroups.get(si), si, fieldValues, identityClass, subResult.matched()));
            }

            // Chain sub-group results
            if (!subGroupResults.isEmpty()) {
                boolean subChainResult = subGroupResults.get(0).matched();
                for (int i = 1; i < subGroupResults.size(); i++) {
                    String connectingOp = subGroupResults.get(i - 1).interGroupOperator();
                    if ("OR".equals(connectingOp)) {
                        subChainResult = subChainResult || subGroupResults.get(i).matched();
                    } else {
                        subChainResult = subChainResult && subGroupResults.get(i).matched();
                    }
                }
                allResults.add(subChainResult);
            }
        }

        // --- Combine all results using groupOperator ---

        boolean rawGroupResult;
        if (allResults.isEmpty()) {
            rawGroupResult = true;
        } else if ("OR".equals(groupOp)) {
            rawGroupResult = allResults.stream().anyMatch(Boolean::booleanValue);
        } else {
            rawGroupResult = allResults.stream().allMatch(Boolean::booleanValue);
        }

        // Apply negation
        boolean finalGroupResult = negated ? !rawGroupResult : rawGroupResult;

        groupBuilder.setGroupMatched(finalGroupResult);
        builder.addGroupResult(groupBuilder);

        return new GroupEvidenceResult(interGroupOp, finalGroupResult);
    }

    /**
     * Builds a complete evidence map for a sub-group, including its conditions
     * and recursively nested sub-groups.
     *
     * @param groupMap      the raw JSONB map for this sub-group
     * @param groupIndex    the index of this sub-group among its siblings
     * @param fieldValues   the identity's field values
     * @param identityClass the identity entity class
     * @param matched       the pre-computed match result for this sub-group
     * @return complete evidence map for JSONB serialization
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSubGroupEvidenceMap(
            Map<String, Object> groupMap,
            int groupIndex,
            Map<String, Object> fieldValues,
            Class<? extends Identity> identityClass,
            boolean matched) {

        String groupOp = groupMap.get("groupOperator") != null
                ? groupMap.get("groupOperator").toString().toUpperCase()
                : RuleEvaluationConstants.DEFAULT_GROUP_OPERATOR;

        boolean negated = groupMap.get("negated") instanceof Boolean b && b;

        Map<String, Object> evidenceMap = new LinkedHashMap<>();
        evidenceMap.put("groupIndex", groupIndex);
        evidenceMap.put("groupOperator", groupOp);
        evidenceMap.put("negated", negated);

        // Evaluate and record leaf conditions
        List<Map<String, Object>> conditionEvidence = new ArrayList<>();
        Object conditionsObj = groupMap.get("conditions");
        if (conditionsObj instanceof List<?> condList) {
            for (Object item : condList) {
                if (item instanceof Map<?, ?> condMap) {
                    Map<String, Object> cond = (Map<String, Object>) condMap;
                    String rawField = (String) cond.get("field");
                    String operator = (String) cond.get("operator");
                    Object expectedValue = cond.get("value");
                    Object expectedValues = cond.get("values");

                    String resolvedField = fieldRegistry.resolveField(rawField, identityClass);
                    Object actualValue = fieldValues.get(resolvedField);

                    boolean condMatched = evaluateConditionInMemory(
                            actualValue, operator, expectedValue, expectedValues);

                    Map<String, Object> condEvidence = new LinkedHashMap<>();
                    condEvidence.put("field", resolvedField);
                    condEvidence.put("operator", operator);
                    condEvidence.put("expectedValue", expectedValues != null ? expectedValues : expectedValue);
                    condEvidence.put("actualValue", actualValue);
                    condEvidence.put("matched", condMatched);
                    conditionEvidence.add(condEvidence);
                }
            }
        }
        evidenceMap.put("conditions", conditionEvidence);

        // Recursively build nested sub-group evidence
        List<Map<String, Object>> nestedEvidence = new ArrayList<>();
        Object subGroupsObj = groupMap.get("conditionGroups");
        if (subGroupsObj instanceof List<?> subGroupList) {
            int si = 0;
            for (Object item : subGroupList) {
                if (item instanceof Map<?, ?> subGroupMap) {
                    // Recursively evaluate this nested sub-group
                    MatchEvidenceBuilder tempBuilder = new MatchEvidenceBuilder();
                    GroupEvidenceResult subResult = evaluateGroupEvidence(
                            (Map<String, Object>) subGroupMap, si, fieldValues, identityClass, tempBuilder);

                    Map<String, Object> subEvidence = buildSubGroupEvidenceMap(
                            (Map<String, Object>) subGroupMap, si, fieldValues, identityClass, subResult.matched());
                    nestedEvidence.add(subEvidence);
                    si++;
                }
            }
        }
        if (!nestedEvidence.isEmpty()) {
            evidenceMap.put("conditionGroups", nestedEvidence);
        }

        evidenceMap.put("groupMatched", matched);
        return evidenceMap;
    }

    /**
     * Evaluates a single condition against an actual field value in memory.
     *
     * <p>This mirrors the SQL predicate logic but runs in Java for evidence recording.
     * All comparisons are case-insensitive to match the database behavior.
     *
     * @param actualValue    the identity's actual field value
     * @param operator       the operator string (EQUALS, CONTAINS, etc.)
     * @param expectedValue  the expected value from the condition
     * @param expectedValues the expected values list (for IN/NOT_IN)
     * @return true if the condition matches
     */
    private boolean evaluateConditionInMemory(Object actualValue, String operator,
                                              Object expectedValue, Object expectedValues) {
        if (actualValue == null) {
            return "NOT_EQUALS".equalsIgnoreCase(operator) || "NOT_IN".equalsIgnoreCase(operator);
        }

        String actual = actualValue.toString().toUpperCase();
        String expected = expectedValue != null ? expectedValue.toString().toUpperCase() : "";

        return switch (operator.toUpperCase()) {
            case "EQUALS" -> actual.equals(expected);
            case "NOT_EQUALS" -> !actual.equals(expected);
            case "CONTAINS" -> actual.contains(expected);
            case "STARTS_WITH" -> actual.startsWith(expected);
            case "ENDS_WITH" -> actual.endsWith(expected);
            case "IN" -> {
                List<String> list = resolveListValues(expectedValue, expectedValues)
                        .stream().map(String::toUpperCase).toList();
                yield list.contains(actual);
            }
            case "NOT_IN" -> {
                List<String> list = resolveListValues(expectedValue, expectedValues)
                        .stream().map(String::toUpperCase).toList();
                yield !list.contains(actual);
            }
            default -> false;
        };
    }

    // ===================================================================
    //  Identity Snapshot Loading
    // ===================================================================

    /**
     * Loads identity field values for a chunk of identity IDs.
     *
     * <p>Returns a map of identity ID to {@link IdentitySnapshot} containing
     * the display name and all field values needed for match evidence.
     *
     * @param tenantId      the tenant identifier
     * @param identityIds   the identity IDs to load
     * @param identityClass the JPA entity class
     * @param <T>           identity type
     * @return map of identity ID to snapshot
     */
    @SuppressWarnings("unchecked")
    private <T extends Identity> Map<UUID, IdentitySnapshot> loadIdentitySnapshots(
            String tenantId,
            List<UUID> identityIds,
            Class<T> identityClass) {

        if (identityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> fields = fieldRegistry.getFieldsForClass(identityClass);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<T> root = query.from(identityClass);

        List<Selection<?>> selections = new ArrayList<>();
        selections.add(root.get("id").alias("id"));
        for (String field : fields) {
            try {
                selections.add(root.get(field).alias(field));
            } catch (IllegalArgumentException ignored) {
            }
        }

        query.multiselect(selections);
        query.where(
                cb.equal(root.get("tenantId"), tenantId),
                root.get("id").in(identityIds)
        );

        List<Tuple> tuples = entityManager.createQuery(query).getResultList();

        Map<UUID, IdentitySnapshot> result = new HashMap<>(tuples.size());
        for (Tuple tuple : tuples) {
            UUID id = tuple.get("id", UUID.class);

            Map<String, Object> fieldValues = new LinkedHashMap<>();
            for (String field : fields) {
                try {
                    Object val = tuple.get(field);
                    if (val != null) {
                        fieldValues.put(field, val.toString());
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            String displayName = resolveDisplayName(fieldValues);
            result.put(id, new IdentitySnapshot(id, displayName, fieldValues));
        }

        return result;
    }

    /**
     * Resolves a human-readable display name from identity field values.
     *
     * <p>Priority: displayName > firstName + lastName > username > "Unknown"
     *
     * @param fieldValues the identity's field values
     * @return the best available display name
     */
    private String resolveDisplayName(Map<String, Object> fieldValues) {
        Object displayName = fieldValues.get("displayName");
        if (displayName != null && !displayName.toString().isBlank()) {
            return displayName.toString();
        }

        Object firstName = fieldValues.get("firstName");
        Object lastName = fieldValues.get("lastName");
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }

        Object username = fieldValues.get("username");
        if (username != null) {
            return username.toString();
        }

        Object serviceName = fieldValues.get("serviceName");
        if (serviceName != null && !serviceName.toString().isBlank()) {
            return serviceName.toString();
        }

        return "Unknown";
    }

    // ===================================================================
    //  Bulk Shadow Resolution
    // ===================================================================

    /**
     * Bulk loads shadow (account) records for a chunk of identity IDs.
     *
     * <p>Queries the shadows table for LINKED accounts on the target resource.
     * Returns a map of identity ID → shadow ID.
     *
     * @param tenantId    the tenant identifier
     * @param identityIds the identity IDs to resolve shadows for
     * @param resourceId  the target resource UUID
     * @return map of identity ID to shadow ID
     */
    @SuppressWarnings("unchecked")
    private Map<UUID, UUID> bulkLoadShadows(String tenantId, List<UUID> identityIds, UUID resourceId) {
        if (identityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        logger.debug()
                .message(RuleEvaluationConstants.LOG_SHADOW_BULK_LOOKUP)
                .detail(RuleEvaluationConstants.DETAIL_CHUNK_SIZE, String.valueOf(identityIds.size()))
                .detail(RuleEvaluationConstants.DETAIL_RESOURCE_ID, resourceId.toString())
                .log();

        List<Object[]> rows = entityManager.createQuery(
                        "SELECT s.linkedIdentityId, s.id FROM Shadow s " +
                                "WHERE s.tenantId = :tenantId " +
                                "AND s.linkedIdentityId IN (:identityIds) " +
                                "AND s.resourceId = :resourceId " +
                                "AND s.kind = :kind")
                .setParameter("tenantId", tenantId)
                .setParameter("identityIds", identityIds)
                .setParameter("resourceId", resourceId.toString())
                .setParameter("kind", RuleEvaluationConstants.SHADOW_KIND_ACCOUNT)
                .getResultList();

        Map<UUID, UUID> result = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            UUID identityId = (UUID) row[0];
            UUID shadowId = (UUID) row[1];
            result.putIfAbsent(identityId, shadowId);
        }
        return result;
    }

    // ===================================================================
    //  Bulk Assignment Existence Check
    // ===================================================================

    /**
     * Bulk checks which shadow IDs already have an assignment for the given entitlement.
     *
     * @param tenantId      the tenant identifier
     * @param shadowIds     the shadow IDs to check
     * @param entitlementId the entitlement to check assignment for
     * @return set of shadow IDs that already have the assignment
     */
    @SuppressWarnings("unchecked")
    private Set<UUID> bulkCheckExistingAssignments(String tenantId, Set<UUID> shadowIds, UUID entitlementId) {
        if (shadowIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<UUID> existing = entityManager.createQuery(
                        "SELECT ea.shadowId FROM EntitlementAssignment ea " +
                                "WHERE ea.tenantId = :tenantId " +
                                "AND ea.shadowId IN (:shadowIds) " +
                                "AND ea.entitlementId = :entitlementId " +
                                "AND ea.source = :source")
                .setParameter("tenantId", tenantId)
                .setParameter("shadowIds", new ArrayList<>(shadowIds))
                .setParameter("entitlementId", entitlementId)
                .setParameter("source", RuleEvaluationConstants.SOURCE_DYNAMIC)
                .getResultList();

        return new HashSet<>(existing);
    }

    /**
     * Bulk checks which identities already have pending no-shadow assignments for an entitlement.
     */
    @SuppressWarnings("unchecked")
    private Set<UUID> bulkCheckExistingPendingAssignments(String tenantId, List<UUID> identityIds, UUID entitlementId) {
        if (identityIds.isEmpty()) {
            return Collections.emptySet();
        }

        List<UUID> existing = entityManager.createQuery(
                        "SELECT ea.identityId FROM EntitlementAssignment ea " +
                                "WHERE ea.tenantId = :tenantId " +
                                "AND ea.shadowId IS NULL " +
                                "AND ea.identityId IN (:identityIds) " +
                                "AND ea.entitlementId = :entitlementId " +
                                "AND ea.source = :source")
                .setParameter("tenantId", tenantId)
                .setParameter("identityIds", identityIds)
                .setParameter("entitlementId", entitlementId)
                .setParameter("source", RuleEvaluationConstants.SOURCE_DYNAMIC)
                .getResultList();

        return new HashSet<>(existing);
    }

    // ===================================================================
    //  Atomic Upsert
    // ===================================================================

    /**
     * Atomically upserts a single entitlement assignment using
     * {@code INSERT ... ON CONFLICT DO NOTHING}.
     *
     * @param tenantId      the tenant identifier
     * @param shadowId      the shadow (account) UUID
     * @param entitlementId the entitlement UUID to assign
     * @param resourceId    the resource UUID
     * @param ruleId        the rule UUID that created this assignment
     */
    private void upsertAssignment(String tenantId, UUID shadowId, UUID identityId, UUID entitlementId,
                                  UUID resourceId, UUID ruleId) {
        long now = Instant.now().toEpochMilli();

        Query nativeQuery = entityManager.createNativeQuery(RuleEvaluationConstants.NATIVE_UPSERT_ASSIGNMENT);
        nativeQuery.setParameter("id", UUID.randomUUID());
        nativeQuery.setParameter("tenantId", tenantId);
        nativeQuery.setParameter("shadowId", shadowId);
        nativeQuery.setParameter("identityId", identityId);
        nativeQuery.setParameter("entitlementId", entitlementId);
        nativeQuery.setParameter("resourceId", resourceId);
        nativeQuery.setParameter("source", RuleEvaluationConstants.SOURCE_DYNAMIC);
        nativeQuery.setParameter("ruleId", ruleId);
        nativeQuery.setParameter("assignedAt", now);
        nativeQuery.setParameter("updatedAt", now);
        nativeQuery.executeUpdate();
    }

    /** Creates a pending identity-scoped assignment when no account shadow exists. */
    private void upsertPendingAssignment(String tenantId, UUID identityId, UUID entitlementId,
                                         UUID resourceId, UUID ruleId) {
        long now = Instant.now().toEpochMilli();

        Query nativeQuery = entityManager.createNativeQuery(RuleEvaluationConstants.NATIVE_UPSERT_PENDING_ASSIGNMENT);
        nativeQuery.setParameter("id", UUID.randomUUID());
        nativeQuery.setParameter("tenantId", tenantId);
        nativeQuery.setParameter("identityId", identityId);
        nativeQuery.setParameter("entitlementId", entitlementId);
        nativeQuery.setParameter("resourceId", resourceId);
        nativeQuery.setParameter("source", RuleEvaluationConstants.SOURCE_DYNAMIC);
        nativeQuery.setParameter("ruleId", ruleId);
        nativeQuery.setParameter("assignedAt", now);
        nativeQuery.setParameter("updatedAt", now);
        nativeQuery.executeUpdate();
    }

    /** Removes a stale pending row after the identity receives a shadow-backed assignment. */
    private void deletePendingAssignment(String tenantId, UUID identityId, UUID entitlementId, UUID ruleId) {
        Query nativeQuery = entityManager.createNativeQuery(RuleEvaluationConstants.NATIVE_DELETE_PENDING_ASSIGNMENT);
        nativeQuery.setParameter("tenantId", tenantId);
        nativeQuery.setParameter("identityId", identityId);
        nativeQuery.setParameter("entitlementId", entitlementId);
        nativeQuery.setParameter("ruleId", ruleId);
        nativeQuery.executeUpdate();
    }

    // ===================================================================
    //  Bulk Revocation
    // ===================================================================

    /**
     * Revokes DYNAMIC assignments for identities that no enabled rule for the same entitlement still matches.
     *
     * @param tenantId                      the tenant identifier
     * @param rule                          the rule whose entitlement is being reconciled
     * @param currentRuleMatchedIdentityIds the identities that currently match this rule
     * @return number of assignments revoked
     */
    private int bulkRevokeStaleAssignments(String tenantId,
                                           EntitlementAssignmentRule rule,
                                           Set<UUID> currentRuleMatchedIdentityIds) {
        Set<UUID> protectedIdentityIds = collectEntitlementMatchedIdentityIds(
                tenantId, rule, currentRuleMatchedIdentityIds);

        logger.debug()
                .message(RuleEvaluationConstants.LOG_REVOCATION_START)
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                .detail(RuleEvaluationConstants.DETAIL_ENTITLEMENT_ID, rule.getEntitlementId().toString())
                .detail(RuleEvaluationConstants.DETAIL_MATCHED_COUNT,
                        String.valueOf(protectedIdentityIds.size()))
                .log();

        int revoked = revokeDynamicAssignmentsForEntitlement(
                tenantId, rule.getEntitlementId(), protectedIdentityIds);

        logger.debug()
                .message(RuleEvaluationConstants.LOG_REVOCATION_COMPLETE)
                .detail(RuleEvaluationConstants.DETAIL_RULE_ID, rule.getId().toString())
                .detail(RuleEvaluationConstants.DETAIL_ENTITLEMENT_ID, rule.getEntitlementId().toString())
                .detail(RuleEvaluationConstants.DETAIL_REVOKED_COUNT, String.valueOf(revoked))
                .log();

        return revoked;
    }

    private Set<UUID> collectEntitlementMatchedIdentityIds(String tenantId,
                                                           EntitlementAssignmentRule currentRule,
                                                           Set<UUID> currentRuleMatchedIdentityIds) {
        Set<UUID> protectedIdentityIds = new HashSet<>();
        if (currentRule.isEnabled()) {
            protectedIdentityIds.addAll(currentRuleMatchedIdentityIds);
        }

        List<EntitlementAssignmentRule> entitlementRules = ruleRepository
                .findByTenantIdAndEntitlementIdAndEnabledTrueOrderByPriorityAsc(
                        tenantId, currentRule.getEntitlementId());

        for (EntitlementAssignmentRule entitlementRule : entitlementRules) {
            if (Objects.equals(entitlementRule.getId(), currentRule.getId())) {
                continue;
            }
            protectedIdentityIds.addAll(findAllMatchingIdentityIdsForRule(tenantId, entitlementRule));
        }

        return protectedIdentityIds;
    }

    private Set<UUID> findAllMatchingIdentityIdsForRule(String tenantId, EntitlementAssignmentRule rule) {
        List<String> identityTypes = normalizationService.normalizeIdentityTypes(rule.getIdentityTypes(), true);
        List<Class<? extends Identity>> identityClasses = normalizationService.resolveIdentityClasses(identityTypes);
        Set<UUID> matchedIdentityIds = new HashSet<>();

        for (Class<? extends Identity> identityClass : identityClasses) {
            int pageNumber = 0;
            Page<UUID> identityIdPage;
            do {
                Pageable pageable = PageRequest.of(pageNumber, RuleEvaluationConstants.IDENTITY_CHUNK_SIZE);
                identityIdPage = findMatchingIdentityIds(tenantId, identityClass, rule.getConditions(), pageable);
                matchedIdentityIds.addAll(identityIdPage.getContent());
                pageNumber++;
            } while (identityIdPage.hasNext());
        }

        return matchedIdentityIds;
    }

    int revokeDynamicAssignmentsForEntitlement(String tenantId, UUID entitlementId, Set<UUID> matchedIdentityIds) {
        if (matchedIdentityIds.isEmpty()) {
            Query revokeAll = entityManager.createNativeQuery(
                    RuleEvaluationConstants.NATIVE_REVOKE_ALL_FOR_ENTITLEMENT);
            revokeAll.setParameter("tenantId", tenantId);
            revokeAll.setParameter("entitlementId", entitlementId);
            return revokeAll.executeUpdate();
        }

        Query revoke = entityManager.createNativeQuery(
                RuleEvaluationConstants.NATIVE_BULK_REVOKE);
        revoke.setParameter("tenantId", tenantId);
        revoke.setParameter("entitlementId", entitlementId);
        revoke.setParameter("matchedIdentityIds", new ArrayList<>(matchedIdentityIds));
        return revoke.executeUpdate();
    }

    // ===================================================================
    //  Helpers
    // ===================================================================


    /**
     * Resolves a list of string values from either the "values" array
     * or a comma-separated "value" string.
     *
     * @param value  the single value (possibly comma-separated)
     * @param values the list of values
     * @return resolved list of strings
     */
    @SuppressWarnings("unchecked")
    private List<String> resolveListValues(Object value, Object values) {
        if (values instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(Object::toString).toList();
        }
        if (value != null) {
            return Arrays.stream(value.toString().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return List.of();
    }

    // ===================================================================
    //  Internal Records
    // ===================================================================

    /**
     * Immutable snapshot of an identity's field values at the time of evaluation.
     *
     * @param id          the identity UUID
     * @param displayName the resolved display name
     * @param fieldValues all identity field values as a string map
     */
    record IdentitySnapshot(UUID id, String displayName, Map<String, Object> fieldValues) {
    }

    /**
     * Internal record representing a parsed condition group with optional nested sub-groups.
     *
     * @param groupOperator      the operator for combining conditions and sub-groups WITHIN this group (AND/OR)
     * @param interGroupOperator the operator for connecting this group TO THE NEXT sibling group (AND/OR)
     * @param negated            whether the entire group's result is negated
     * @param conditions         the leaf condition maps within this group
     * @param subGroups          nested child groups within this group
     */
    private record GroupedCondition(
            String groupOperator,
            String interGroupOperator,
            boolean negated,
            List<Map<String, Object>> conditions,
            List<GroupedCondition> subGroups
    ) {
    }
}