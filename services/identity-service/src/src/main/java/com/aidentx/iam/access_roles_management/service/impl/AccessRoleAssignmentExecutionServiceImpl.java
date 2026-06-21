package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.audit.AccessRoleAssignmentExecutionAuditService;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecutionItem;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentExecutionItemRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentExecutionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentExecutionService;
import com.aidentx.iam.access_roles_management.service.IdentityRoleAssignmentService;
import com.aidentx.iam.entitlement_management.model.enums.RuleOperator;
import com.aidentx.iam.entitlement_management.service.IdentityFieldRegistry;
import com.aidentx.iam.entitlement_management.util.LikePatternEscaper;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.IdentityStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link AccessRoleAssignmentExecutionService}.
 *
 * <p>This service is responsible for dynamically evaluating access-role assignment rules and
 * assigning or revoking roles on identities (employees, non-employees, and non-human identities)
 * based on those rules.
 *
 * <h2>High-level flow</h2>
 * <ol>
 *   <li>A rule is loaded from the database.</li>
 *   <li>Matching identities are queried in pages using JPA Criteria API.</li>
 *   <li>Matched identities get a role assigned (or re-activated).</li>
 *   <li>Identities that were previously assigned but no longer match get the role revoked.</li>
 *   <li>Every action is recorded as an {@link AccessRoleAssignmentExecutionItem} for audit.</li>
 * </ol>
 *
 * <h2>What is the Criteria API?</h2>
 * <p>The JPA Criteria API lets you build database queries <em>in Java code</em> instead of writing
 * raw SQL or JPQL strings. Think of it as a "query builder" that is type-safe and validated at
 * compile time. Key objects you will see throughout this class:
 * <ul>
 *   <li>{@link CriteriaBuilder} – the factory; creates queries, predicates, and expressions.</li>
 *   <li>{@link CriteriaQuery} – represents one full SELECT query.</li>
 *   <li>{@link Root} – the "FROM" table/entity in the query.</li>
 *   <li>{@link Predicate} – a single condition (e.g. {@code WHERE status = 'ACTIVE'}).</li>
 *   <li>{@link Expression} – a column value or computed value inside a query.</li>
 * </ul>
 */
@Service
public class AccessRoleAssignmentExecutionServiceImpl implements AccessRoleAssignmentExecutionService {

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /**
     * Maximum number of identity IDs fetched in a single database page during rule evaluation.
     * Processing in chunks prevents out-of-memory errors when a rule matches thousands of identities.
     */
    private static final int IDENTITY_CHUNK_SIZE = 500;

    /**
     * Formatter used to parse/format schedule times stored as strings (e.g. {@code "14:30"}).
     */
    private static final DateTimeFormatter SCHEDULE_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Formatter used to parse/format schedule dates stored as strings (e.g. {@code "2025-12-31"}).
     */
    private static final DateTimeFormatter SCHEDULE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    /** Repository for loading and saving {@link AccessRoleAssignmentRule} entities. */
    private final AccessRoleAssignmentRuleRepository ruleRepository;

    /** Repository for loading and saving {@link AccessRoleAssignmentExecution} records. */
    private final AccessRoleAssignmentExecutionRepository executionRepository;

    /** Repository for saving individual {@link AccessRoleAssignmentExecutionItem} audit records. */
    private final AccessRoleAssignmentExecutionItemRepository executionItemRepository;

    /** Repository for looking up {@link AccessRole} entities. */
    private final AccessRoleRepository accessRoleRepository;

    /** Repository for querying existing {@link IdentityRoleAssignment} records. */
    private final IdentityRoleAssignmentRepository assignmentRepository;

    /** Domain service that performs the actual role assign/revoke operations. */
    private final IdentityRoleAssignmentService assignmentService;

    /**
     * Registry that maps logical field names (used inside rule conditions) to the actual JPA
     * entity field names. For example, a rule may say {@code "department"} while the Employee
     * entity stores it as {@code "departmentCode"}.
     */
    private final IdentityFieldRegistry fieldRegistry;

    /**
     * JPA EntityManager used to execute dynamic Criteria API queries.
     * This is needed because Spring Data repositories only handle fixed queries;
     * dynamic, rule-driven queries require manual query construction.
     */
    private final EntityManager entityManager;

    /** Publishes per-rule audit for CRON-triggered executions owned by this service flow. */
    private final AccessRoleAssignmentExecutionAuditService executionAuditService;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Constructs the service with all required dependencies via constructor injection.
     *
     * @param ruleRepository          repository for assignment rules
     * @param executionRepository     repository for execution records
     * @param executionItemRepository repository for execution item (audit) records
     * @param accessRoleRepository    repository for access roles
     * @param assignmentRepository    repository for identity-role assignments
     * @param assignmentService       service that assigns/revokes roles
     * @param fieldRegistry           maps logical field names to entity field names
     * @param entityManager           JPA entity manager for dynamic queries
     * @param executionAuditService   shared execution audit publisher for service-owned CRON runs
     */
    public AccessRoleAssignmentExecutionServiceImpl(
            AccessRoleAssignmentRuleRepository ruleRepository,
            AccessRoleAssignmentExecutionRepository executionRepository,
            AccessRoleAssignmentExecutionItemRepository executionItemRepository,
            AccessRoleRepository accessRoleRepository,
            IdentityRoleAssignmentRepository assignmentRepository,
            IdentityRoleAssignmentService assignmentService,
            IdentityFieldRegistry fieldRegistry,
            EntityManager entityManager,
            AccessRoleAssignmentExecutionAuditService executionAuditService) {
        this.ruleRepository = ruleRepository;
        this.executionRepository = executionRepository;
        this.executionItemRepository = executionItemRepository;
        this.accessRoleRepository = accessRoleRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentService = assignmentService;
        this.fieldRegistry = fieldRegistry;
        this.entityManager = entityManager;
        this.executionAuditService = executionAuditService;
    }

    // =========================================================================
    // Public API – service interface implementations
    // =========================================================================

    /**
     * Executes a single access-role assignment rule identified by {@code ruleId}.
     *
     * <p>The execution lifecycle:
     * <ol>
     *   <li>Validate inputs and load the rule + role from the database.</li>
     *   <li>Persist an {@link AccessRoleAssignmentExecution} in {@code RUNNING} status.</li>
     *   <li>Evaluate the rule's conditions against all active identities (in pages).</li>
     *   <li>Assign/reactivate roles for matched identities.</li>
     *   <li>Revoke roles from identities that were previously assigned but no longer match.</li>
     *   <li>Update the execution status to {@code SUCCESS} or {@code PARTIAL_FAILURE}.</li>
     *   <li>Update the rule's {@code lastEvaluatedAt} and schedule the next execution if needed.</li>
     * </ol>
     *
     * <p>If any unrecoverable error occurs the execution is saved with status {@code FAILED}.
     *
     * @param tenantId    the tenant scope; must not be blank
     * @param ruleId      the ID of the rule to execute; must not be {@code null}
     * @param triggerType how this execution was triggered (MANUAL, CRON, etc.)
     * @param triggeredBy the user or system actor that initiated the execution
     * @return the completed (and persisted) execution record
     * @throws IllegalArgumentException if {@code tenantId} is blank, {@code ruleId} is null,
     *                                  the rule does not exist, or the role referenced by the rule
     *                                  does not exist in the same tenant
     */
    @Override
    @Transactional
    public AccessRoleAssignmentExecution executeRule(String tenantId,
                                                     UUID ruleId,
                                                     AccessRoleAssignmentExecution.TriggerType triggerType,
                                                     String triggeredBy) {
        validateTenantId(tenantId);
        if (ruleId == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_RULE_NOT_FOUND + ": null");
        }

        // Load the rule from the database; throw if not found within this tenant.
        AccessRoleAssignmentRule rule = ruleRepository.findByIdAndTenantId(ruleId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        AccessRoleAssignmentRuleConstants.ERROR_RULE_NOT_FOUND + ": " + ruleId));

        // Load the target access role; it must belong to the same tenant.
        Optional<AccessRole> role = accessRoleRepository.findById(rule.getRoleId())
                .filter(candidate -> tenantId.equals(candidate.getTenantId()));

        // Persist the execution record immediately so it is visible even if something fails.
        AccessRoleAssignmentExecution execution =
                createExecution(rule, role.orElse(null), triggerType, triggeredBy);
        execution = executionRepository.save(execution);

        try {
            if (role.isEmpty()) {
                throw new IllegalArgumentException(
                        AccessRoleAssignmentRuleConstants.ERROR_ROLE_NOT_FOUND + ": " + rule.getRoleId());
            }

            // Core rule evaluation: query matching identities and assign/revoke roles.
            ExecutionCounters counters = evaluateRule(tenantId, rule, execution.getId());
            applyCounters(execution, counters);

            // Mark success or partial failure depending on error count.
            execution.complete(counters.errorCount() > 0
                    ? AccessRoleAssignmentExecution.ExecutionStatus.PARTIAL_FAILURE
                    : AccessRoleAssignmentExecution.ExecutionStatus.SUCCESS);

            // Update the rule's scheduling metadata.
            updateRuleExecutionMetadata(rule);

        } catch (Exception ex) {
            execution.fail(ex.getMessage());
            updateRuleExecutionMetadata(rule);
        }

        AccessRoleAssignmentExecution persistedExecution = executionRepository.save(execution);
        publishCronExecutionAuditIfNeeded(tenantId, ruleId, triggerType, persistedExecution, triggeredBy);
        return persistedExecution;
    }

    /**
     * Executes <em>all</em> enabled rules for a tenant, ordered by priority (lowest number first).
     *
     * <p>This is typically called from an admin UI "run all rules" button or a scheduled job
     * that sweeps the entire rule set.
     *
     * <p>Example use-case: a nightly job ensures every enabled rule has been evaluated at least
     * once, even if individual CRON schedules are misconfigured.
     *
     * @param tenantId    the tenant scope; must not be blank
     * @param triggeredBy the user or system actor initiating the run
     * @return list of execution records, one per rule (in priority order)
     */
    @Override
    @Transactional
    public List<AccessRoleAssignmentExecution> executeAllRules(String tenantId, String triggeredBy) {
        validateTenantId(tenantId);
        return executeRules(
                ruleRepository.findByTenantIdAndEnabledTrueOrderByPriorityAsc(tenantId),
                tenantId,
                AccessRoleAssignmentExecution.TriggerType.EVALUATE_ALL,
                triggeredBy);
    }

    /**
     * Executes only rules configured with schedule type {@code IMMEDIATE}.
     *
     * <p>Immediate rules are meant to fire once right after they are created (e.g. when an admin
     * saves a new rule). Calling this method ensures those rules are evaluated promptly.
     *
     * @param tenantId    the tenant scope; must not be blank
     * @param triggeredBy the user or system actor initiating the run
     * @return list of execution records for every immediate rule that was run
     */
    @Override
    @Transactional
    public List<AccessRoleAssignmentExecution> executeImmediateRules(String tenantId, String triggeredBy) {
        validateTenantId(tenantId);
        return executeRules(
                ruleRepository.findByTenantIdAndScheduleTypeAndEnabledTrueOrderByPriorityAsc(
                        tenantId, AccessRoleAssignmentRule.ScheduleType.IMMEDIATE),
                tenantId,
                AccessRoleAssignmentExecution.TriggerType.ACCESS_ROLE_RULE_CREATED,
                triggeredBy);
    }

    /**
     * Executes all CRON-scheduled rules whose {@code nextExecutionAt} timestamp is in the past
     * (i.e. rules that are "due").
     *
     * <p>This method is designed to be called periodically by a scheduler (e.g. every minute).
     * The repository query {@code findDueCronRules} filters only rules where
     * {@code nextExecutionAt <= now}.
     *
     * <p>If {@code triggeredBy} is blank the constant {@code SYSTEM_TRIGGER} is used instead,
     * which makes it clear in audit logs that the system (not a human) fired the execution.
     *
     * @param tenantId    the tenant scope; must not be blank
     * @param triggeredBy the user or system actor initiating the run; defaults to system if blank
     * @return list of execution records for every due CRON rule
     */
    @Override
    @Transactional
    public List<AccessRoleAssignmentExecution> executeCronRules(String tenantId, String triggeredBy) {
        validateTenantId(tenantId);
        return executeRules(
                ruleRepository.findDueCronRules(tenantId, Instant.now().toEpochMilli()),
                tenantId,
                AccessRoleAssignmentExecution.TriggerType.CRON,
                triggeredBy != null && !triggeredBy.isBlank()
                        ? triggeredBy
                        : AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER);
    }

    /**
     * Returns a paginated list of all execution records for a tenant, sorted by start time
     * descending (most recent first).
     *
     * @param tenantId the tenant scope; must not be blank
     * @param page     zero-based page index
     * @param size     page size (clamped to 1–200)
     * @return a page of execution records
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleAssignmentExecution> listExecutions(String tenantId, int page, int size) {
        validateTenantId(tenantId);
        return executionRepository.findByTenantIdOrderByStartedAtDesc(tenantId, pageable(page, size));
    }

    /**
     * Returns a paginated list of execution records scoped to a specific rule.
     *
     * <p>Useful when an admin wants to see the history of a particular rule: when it ran,
     * how many identities matched, and whether it succeeded.
     *
     * @param tenantId the tenant scope; must not be blank
     * @param ruleId   the rule to filter by; must not be {@code null}
     * @param page     zero-based page index
     * @param size     page size (clamped to 1–200)
     * @return a page of execution records for the given rule
     * @throws IllegalArgumentException if {@code ruleId} is {@code null}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleAssignmentExecution> listExecutionsByRule(String tenantId, UUID ruleId, int page, int size) {
        validateTenantId(tenantId);
        if (ruleId == null) {
            throw new IllegalArgumentException("Rule ID is required");
        }
        return executionRepository.findByTenantIdAndRuleIdOrderByStartedAtDesc(tenantId, ruleId, pageable(page, size));
    }

    /**
     * Returns a paginated list of execution records scoped to a specific role.
     *
     * <p>Useful when an admin wants to see every time a particular role was assigned/revoked
     * automatically across all rules.
     *
     * @param tenantId the tenant scope; must not be blank
     * @param roleId   the role to filter by; must not be {@code null}
     * @param page     zero-based page index
     * @param size     page size (clamped to 1–200)
     * @return a page of execution records for the given role
     * @throws IllegalArgumentException if {@code roleId} is {@code null}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleAssignmentExecution> listExecutionsByRole(String tenantId,
                                                                    UUID roleId,
                                                                    AccessRoleAssignmentExecution.TriggerType triggerType,
                                                                    AccessRoleAssignmentExecution.ExecutionStatus status,
                                                                    Long startedFrom,
                                                                    Long startedTo,
                                                                    int page,
                                                                    int size) {
        validateTenantId(tenantId);
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }
        validateStartedAtRange(startedFrom, startedTo);
        Specification<AccessRoleAssignmentExecution> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.equal(root.get("roleId"), roleId));

            if (triggerType != null) {
                predicates.add(cb.equal(root.get("triggerType"), triggerType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (startedFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), startedFrom));
            }
            if (startedTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), startedTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return executionRepository.findAll(
                specification,
                PageRequest.of(
                        Math.max(page, 0),
                        normalizePageSize(size),
                        Sort.by(Sort.Direction.DESC, "startedAt")));
    }

    /**
     * Returns a paginated list of individual execution items (one row per identity processed)
     * for a given execution run, sorted by processing time descending.
     *
     * @param tenantId    the tenant scope; must not be blank
     * @param executionId the execution to inspect; must not be {@code null}
     * @param page        zero-based page index
     * @param size        page size (clamped to 1–200)
     * @return a page of execution items
     * @throws IllegalArgumentException if {@code executionId} is {@code null}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleAssignmentExecutionItem> listExecutionItems(String tenantId, UUID executionId, int page, int size) {
        validateTenantId(tenantId);
        if (executionId == null) {
            throw new IllegalArgumentException("Execution ID is required");
        }
        return executionItemRepository.findByTenantIdAndExecutionIdOrderByProcessedAtDesc(
                tenantId, executionId, pageable(page, size));
    }

    /**
     * Returns a paginated list of execution items filtered by outcome (e.g. only
     * {@code ASSIGNED} items, or only {@code ERROR} items).
     *
     * <p>Example: an admin wants to investigate why some identities were not assigned.
     * They filter by {@code outcome = ERROR} to see all failures and their error messages.
     *
     * @param tenantId    the tenant scope; must not be blank
     * @param executionId the execution to inspect; must not be {@code null}
     * @param outcome     the outcome to filter by; must not be {@code null}
     * @param page        zero-based page index
     * @param size        page size (clamped to 1–200)
     * @return a page of execution items matching the requested outcome
     * @throws IllegalArgumentException if {@code executionId} or {@code outcome} is {@code null}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleAssignmentExecutionItem> listExecutionItemsByOutcome(String tenantId,
                                                                               UUID executionId,
                                                                               AccessRoleAssignmentExecutionItem.Outcome outcome,
                                                                               int page,
                                                                               int size) {
        validateTenantId(tenantId);
        if (executionId == null) {
            throw new IllegalArgumentException("Execution ID is required");
        }
        if (outcome == null) {
            throw new IllegalArgumentException("Execution item outcome is required");
        }
        return executionItemRepository.findByTenantIdAndExecutionIdAndOutcomeOrderByProcessedAtDesc(
                tenantId, executionId, outcome, pageable(page, size));
    }

    // =========================================================================
    // Private helpers – rule execution orchestration
    // =========================================================================

    /**
     * Iterates a list of rules and executes each one, collecting the resulting execution records.
     *
     * <p>Rules are executed sequentially (not in parallel) to avoid race conditions in the
     * assignment repository.
     *
     * @param rules       the ordered list of rules to execute
     * @param tenantId    the tenant scope
     * @param triggerType the trigger type to record on each execution
     * @param triggeredBy the actor that triggered this batch
     * @return list of execution records, one per rule
     */
    private List<AccessRoleAssignmentExecution> executeRules(List<AccessRoleAssignmentRule> rules,
                                                             String tenantId,
                                                             AccessRoleAssignmentExecution.TriggerType triggerType,
                                                             String triggeredBy) {
        List<AccessRoleAssignmentExecution> executions = new ArrayList<>(rules.size());
        for (AccessRoleAssignmentRule rule : rules) {
            executions.add(executeRule(tenantId, rule.getId(), triggerType, triggeredBy));
        }
        return executions;
    }

    private void publishCronExecutionAuditIfNeeded(String tenantId,
                                                   UUID ruleId,
                                                   AccessRoleAssignmentExecution.TriggerType triggerType,
                                                   AccessRoleAssignmentExecution execution,
                                                   String triggeredBy) {
        if (triggerType != AccessRoleAssignmentExecution.TriggerType.CRON) {
            return;
        }
        executionAuditService.publishSingleExecutionAudit(tenantId, execution, ruleId, triggeredBy);
    }

    /**
     * Core evaluation loop for a single rule.
     *
     * <p>For each identity type declared in the rule (EMPLOYEE, NON_EMPLOYEE, NON_HUMAN):
     * <ol>
     *   <li>Build a Criteria API query that returns the IDs of identities matching
     *       the rule's condition groups (paginated in chunks of {@value IDENTITY_CHUNK_SIZE}).</li>
     *   <li>For each page of IDs, load a lightweight "snapshot" of the identity's fields.</li>
     *   <li>Assign the role to each matched identity, recording the outcome as an execution item.</li>
     * </ol>
     * After all matching identities are processed, previously-assigned identities that are no
     * longer matched have their role revoked.
     *
     * @param tenantId    the tenant scope
     * @param rule        the rule being evaluated
     * @param executionId the ID of the parent execution record (for linking audit items)
     * @return aggregated counters describing the outcome of the evaluation
     */
    private ExecutionCounters evaluateRule(String tenantId, AccessRoleAssignmentRule rule, UUID executionId) {
        Set<UUID> allMatchedIdentityIds = new HashSet<>();
        ExecutionCounters counters = new ExecutionCounters();

        for (String identityType : resolveIdentityTypes(rule)) {
            Class<? extends Identity> identityClass = resolveIdentityClass(identityType);
            int pageNumber = 0;
            Page<UUID> identityIdPage;

            do {
                Pageable pageable = PageRequest.of(pageNumber, IDENTITY_CHUNK_SIZE);
                identityIdPage = findMatchingIdentityIds(tenantId, identityClass, rule.getConditionGroups(), pageable);

                List<UUID> identityIds = identityIdPage.getContent();
                if (identityIds.isEmpty()) {
                    break;
                }

                allMatchedIdentityIds.addAll(identityIds);
                counters.identitiesMatched += identityIds.size();

                // Load field values (displayName, department, etc.) for all IDs in one query.
                Map<UUID, IdentitySnapshot> snapshots =
                        loadIdentitySnapshots(tenantId, identityIds, identityClass);

                for (UUID identityId : identityIds) {
                    IdentitySnapshot snapshot = snapshots.get(identityId);
                    try {
                        AccessRoleAssignmentExecutionItem.Outcome outcome =
                                assignMatchedIdentity(tenantId, rule, identityId);

                        // Re-load the assignment to embed it in the audit item.
                        IdentityRoleAssignment assignment = assignmentRepository
                                .findByTenantIdAndIdentityIdAndRoleIdAndSourceTypeAndSourceKey(
                                        tenantId, identityId, rule.getRoleId(),
                                        IdentityRoleAssignment.SourceType.RULE, rule.getId().toString())
                                .orElse(null);

                        updateCountersForOutcome(counters, outcome);
                        saveExecutionItem(tenantId, executionId, rule, identityId, snapshot, assignment, outcome,
                                buildEvidence(snapshot, rule.getConditionGroups(), identityClass, true, null),
                                null);

                    } catch (Exception ex) {
                        counters.errorCount++;
                        saveExecutionItem(tenantId, executionId, rule, identityId, snapshot, null,
                                AccessRoleAssignmentExecutionItem.Outcome.ERROR,
                                buildEvidence(snapshot, rule.getConditionGroups(), identityClass, false, ex.getMessage()),
                                ex.getMessage());
                    }
                }
                pageNumber++;

            } while (identityIdPage.hasNext());
        }

        // Revoke role from identities that no longer match the rule.
        counters.rolesRevoked += revokeStaleAssignments(tenantId, rule, executionId, allMatchedIdentityIds);
        return counters;
    }

    /**
     * Assigns the role defined in the rule to a single identity.
     *
     * <p>Three possible outcomes:
     * <ul>
     *   <li>{@code ALREADY_ASSIGNED} – the identity already has an active assignment from this
     *       rule; nothing to do.</li>
     *   <li>{@code REACTIVATED} – the identity had a previously revoked assignment which was
     *       re-activated.</li>
     *   <li>{@code ASSIGNED} – a brand-new assignment was created.</li>
     * </ul>
     *
     * <p>Example:
     * <pre>
     *   Rule assigns role "Finance-Reader" sourced from rule "ABC".
     *   Identity "alice" already has an ACTIVE assignment from rule "ABC"
     *   → outcome: ALREADY_ASSIGNED (no DB write needed)
     *
     *   Identity "bob" had a REVOKED assignment from rule "ABC" (rule was disabled before)
     *   → outcome: REACTIVATED (the existing record is updated to ACTIVE)
     *
     *   Identity "carol" has no prior assignment from rule "ABC"
     *   → outcome: ASSIGNED (new record inserted)
     * </pre>
     *
     * @param tenantId   the tenant scope
     * @param rule       the rule whose role should be assigned
     * @param identityId the identity receiving the role
     * @return the outcome of the assignment attempt
     */
    private AccessRoleAssignmentExecutionItem.Outcome assignMatchedIdentity(String tenantId,
                                                                            AccessRoleAssignmentRule rule,
                                                                            UUID identityId) {
        String sourceKey = rule.getId().toString();
        Optional<IdentityRoleAssignment> existing = assignmentRepository
                .findByTenantIdAndIdentityIdAndRoleIdAndSourceTypeAndSourceKey(
                        tenantId, identityId, rule.getRoleId(),
                        IdentityRoleAssignment.SourceType.RULE, sourceKey);

        IdentityRoleAssignment assignment = assignmentService.assignRole(
                tenantId, identityId, rule.getRoleId(),
                IdentityRoleAssignment.SourceType.RULE, rule.getId(), sourceKey);

        if (existing.isPresent() && existing.get().getStatus() == IdentityRoleAssignment.Status.ACTIVE) {
            return AccessRoleAssignmentExecutionItem.Outcome.ALREADY_ASSIGNED;
        }

        return existing.isPresent() || assignment.getStatus() == IdentityRoleAssignment.Status.REVOKED
                ? AccessRoleAssignmentExecutionItem.Outcome.REACTIVATED
                : AccessRoleAssignmentExecutionItem.Outcome.ASSIGNED;
    }

    /**
     * Revokes the rule's role from identities that previously had it but are no longer matched.
     *
     * <p>This is how the system keeps assignments in sync when an identity's attributes change.
     * For example, if an employee moves to a different department and no longer satisfies the rule
     * conditions, their role assignment from that rule is revoked automatically.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>Load all ACTIVE assignments that were created by this rule.</li>
     *   <li>Skip identities that are still in the matched set (they were just processed above).</li>
     *   <li>For identities no longer matched, call {@code revokeRole} and record an execution item
     *       with outcome {@code REVOKED}.</li>
     * </ol>
     *
     * @param tenantId           the tenant scope
     * @param rule               the rule whose stale assignments should be revoked
     * @param executionId        the parent execution ID for linking audit items
     * @param matchedIdentityIds the set of identity IDs that currently match the rule
     * @return the number of assignments revoked
     */
    private int revokeStaleAssignments(String tenantId,
                                       AccessRoleAssignmentRule rule,
                                       UUID executionId,
                                       Set<UUID> matchedIdentityIds) {
        List<IdentityRoleAssignment> activeAssignments =
                assignmentRepository.findByTenantIdAndAssignmentRuleIdAndStatus(
                        tenantId, rule.getId(), IdentityRoleAssignment.Status.ACTIVE);

        int revoked = 0;
        for (IdentityRoleAssignment assignment : activeAssignments) {
            if (matchedIdentityIds.contains(assignment.getIdentityId())) {
                continue; // Still matches → keep the assignment.
            }

            Optional<IdentityRoleAssignment> revokedAssignment = assignmentService.revokeRole(
                    tenantId, assignment.getIdentityId(), rule.getRoleId(),
                    IdentityRoleAssignment.SourceType.RULE, rule.getId(), rule.getId().toString());

            if (revokedAssignment.isPresent()) {
                revoked++;
                saveExecutionItem(tenantId, executionId, rule, assignment.getIdentityId(), null,
                        revokedAssignment.get(),
                        AccessRoleAssignmentExecutionItem.Outcome.REVOKED,
                        Map.of("overallMatch", false,
                               "reason", "Identity no longer matches the access-role assignment rule"),
                        null);
            }
        }
        return revoked;
    }

    /**
     * Increments the appropriate counter inside {@code counters} based on the item outcome.
     *
     * <p>This keeps the {@link ExecutionCounters} up to date so that after the evaluation loop
     * finishes, the totals can be written back to the execution record.
     *
     * @param counters the mutable counters object to update
     * @param outcome  the outcome of processing one identity
     */
    private void updateCountersForOutcome(ExecutionCounters counters,
                                          AccessRoleAssignmentExecutionItem.Outcome outcome) {
        switch (outcome) {
            case ASSIGNED         -> counters.rolesAssigned++;
            case REACTIVATED      -> counters.rolesReactivated++;
            case ALREADY_ASSIGNED -> counters.skippedAlreadyAssigned++;
            case SKIPPED_NOT_MATCHED -> counters.skippedNotMatched++;
            case ERROR            -> counters.errorCount++;
            case REVOKED          -> counters.rolesRevoked++;
        }
    }

    /**
     * Builds and persists a single {@link AccessRoleAssignmentExecutionItem} audit record.
     *
     * <p>Each item represents what happened to one identity during one execution: was the role
     * assigned, already active, revoked, or did an error occur? The {@code evidence} map contains
     * human-readable details about which conditions matched and what field values were found,
     * making it easy to debug rule behaviour in the UI.
     *
     * @param tenantId     the tenant scope
     * @param executionId  the parent execution ID
     * @param rule         the rule being executed
     * @param identityId   the identity that was processed
     * @param snapshot     snapshot of the identity's field values (may be {@code null} for revocations)
     * @param assignment   the resulting assignment record (may be {@code null} on errors)
     * @param outcome      what happened to this identity
     * @param evidence     structured map documenting which conditions matched and the actual field values
     * @param errorMessage the error message if the outcome is {@code ERROR}; otherwise {@code null}
     */
    private void saveExecutionItem(String tenantId,
                                   UUID executionId,
                                   AccessRoleAssignmentRule rule,
                                   UUID identityId,
                                   IdentitySnapshot snapshot,
                                   IdentityRoleAssignment assignment,
                                   AccessRoleAssignmentExecutionItem.Outcome outcome,
                                   Map<String, Object> evidence,
                                   String errorMessage) {
        AccessRoleAssignmentExecutionItem item = new AccessRoleAssignmentExecutionItem();
        item.setTenantId(tenantId);
        item.setExecutionId(executionId);
        item.setRuleId(rule.getId());
        item.setRoleId(rule.getRoleId());
        item.setIdentityId(identityId);
        item.setIdentityDisplayName(snapshot != null ? snapshot.displayName() : null);
        item.setIdentityType(snapshot != null ? snapshot.identityType() : null);
        item.setAssignmentId(assignment != null ? assignment.getId() : null);
        item.setOutcome(outcome);
        item.setMatchEvidence(evidence);
        item.setErrorMessage(errorMessage);
        executionItemRepository.save(item);
    }

    /**
     * Creates a new {@link AccessRoleAssignmentExecution} entity in {@code RUNNING} status.
     *
     * <p>The execution is a "header" record that tracks overall progress and counters.
     * Individual identity outcomes are stored as child {@link AccessRoleAssignmentExecutionItem}
     * records linked by {@code executionId}.
     *
     * @param rule        the rule being executed
     * @param role        the access role targeted by the rule (may be {@code null} if not found)
     * @param triggerType how the execution was triggered
     * @param triggeredBy the actor that triggered it
     * @return a new, unsaved execution entity
     */
    private AccessRoleAssignmentExecution createExecution(AccessRoleAssignmentRule rule,
                                                          AccessRole role,
                                                          AccessRoleAssignmentExecution.TriggerType triggerType,
                                                          String triggeredBy) {
        AccessRoleAssignmentExecution execution = new AccessRoleAssignmentExecution();
        execution.setTenantId(rule.getTenantId());
        execution.setRuleId(rule.getId());
        execution.setRuleName(rule.getName());
        execution.setRoleId(rule.getRoleId());
        execution.setRoleName(role != null ? resolveRoleDisplayName(role) : null);
        execution.setRoleType(role != null ? role.getType() : null);
        execution.setTriggerType(triggerType != null
                ? triggerType
                : AccessRoleAssignmentExecution.TriggerType.MANUAL);
        execution.setTriggeredBy(triggeredBy);
        execution.setConditionGroupsSnapshot(
                rule.getConditionGroups() != null ? rule.getConditionGroups() : List.of());
        execution.setIdentityTypes(resolveIdentityTypes(rule));
        execution.setStatus(AccessRoleAssignmentExecution.ExecutionStatus.RUNNING);
        execution.setStartedAt(Instant.now().toEpochMilli());
        return execution;
    }

    /**
     * Copies the aggregated counters from an {@link ExecutionCounters} object into the execution
     * entity so they can be persisted to the database.
     *
     * @param execution the execution record to update
     * @param counters  the counters accumulated during evaluation
     */
    private void applyCounters(AccessRoleAssignmentExecution execution, ExecutionCounters counters) {
        execution.setIdentitiesMatched(counters.identitiesMatched());
        execution.setRolesAssigned(counters.rolesAssigned());
        execution.setRolesReactivated(counters.rolesReactivated());
        execution.setRolesRevoked(counters.rolesRevoked());
        execution.setSkippedAlreadyAssigned(counters.skippedAlreadyAssigned());
        execution.setSkippedNotMatched(counters.skippedNotMatched());
        execution.setErrorCount(counters.errorCount());
    }

    // =========================================================================
    // Criteria API – dynamic querying
    // =========================================================================

    /**
     * Finds identity IDs that satisfy the rule's condition groups, using JPA Criteria API.
     *
     * <h3>What is happening here – step by step</h3>
     * <ol>
     *   <li><b>Count query</b> – first we count how many rows match so we can return a proper
     *       {@link Page} object with total-count metadata. If the count is 0 we skip the data
     *       query entirely.</li>
     *   <li><b>Data query</b> – we select only the {@code id} column (not the full entity),
     *       apply the same WHERE clause, and use {@code OFFSET / LIMIT} for pagination.</li>
     * </ol>
     *
     * <h3>Why two separate queries?</h3>
     * <p>Spring's built-in repository paging also does this. The count query is cheap (no data
     * transfer from DB), the data query returns just UUIDs (not full entity objects), which
     * keeps memory usage low when processing hundreds of thousands of identities.
     *
     * <h3>Example</h3>
     * <p>Rule condition: {@code department = "Finance" AND status = ACTIVE}
     * <pre>
     *   -- Count query generated:
     *   SELECT COUNT(*) FROM employee WHERE tenant_id = ? AND status = 'ACTIVE' AND department = 'FINANCE'
     *
     *   -- Data query (page 0, size 500):
     *   SELECT id FROM employee WHERE tenant_id = ? AND status = 'ACTIVE' AND department = 'FINANCE'
     *   OFFSET 0 LIMIT 500
     * </pre>
     *
     * @param tenantId        the tenant scope
     * @param identityClass   the entity class to query (Employee, NonEmployee, or NonHumanIdentity)
     * @param conditionGroups the rule's condition groups (parsed from JSON/map format)
     * @param pageable        the page/size request
     * @param <T>             the identity type
     * @return a page of matching identity IDs
     */
    private <T extends Identity> Page<UUID> findMatchingIdentityIds(String tenantId,
                                                                    Class<T> identityClass,
                                                                    List<Map<String, Object>> conditionGroups,
                                                                    Pageable pageable) {
        // --- Step 1: Count query ---
        // getCriteriaBuilder() is the entry point for any Criteria API query.
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // createQuery(Long.class) → tells the builder the query will return a Long (the count).
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);

        // from(identityClass) → like "FROM Employee e" in JPQL; countRoot is "e".
        Root<T> countRoot = countQuery.from(identityClass);

        // select(cb.count(countRoot)) → SELECT COUNT(e)
        countQuery.select(cb.count(countRoot));

        // where(...) → applies all conditions (tenant, status, rule conditions).
        countQuery.where(buildWhereClause(cb, countRoot, tenantId, identityClass, conditionGroups));

        // getSingleResult() executes and retrieves the single Long value.
        long total = entityManager.createQuery(countQuery).getSingleResult();

        if (total == 0) {
            return Page.empty(pageable);
        }

        // --- Step 2: Data query (IDs only) ---
        CriteriaQuery<UUID> dataQuery = cb.createQuery(UUID.class);
        Root<T> dataRoot = dataQuery.from(identityClass);

        // select(dataRoot.get("id")) → SELECT e.id (only the UUID, not the full entity)
        dataQuery.select(dataRoot.get("id"));
        dataQuery.where(buildWhereClause(cb, dataRoot, tenantId, identityClass, conditionGroups));

        // setFirstResult = OFFSET (skip this many rows)
        // setMaxResults  = LIMIT  (return at most this many rows)
        List<UUID> ids = entityManager.createQuery(dataQuery)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(ids, pageable, total);
    }

    /**
     * Builds the top-level WHERE clause combining three mandatory predicates:
     * <ol>
     *   <li>Tenant isolation – ensures we only touch rows belonging to this tenant.</li>
     *   <li>Status filter – only ACTIVE identities are candidates for role assignment.</li>
     *   <li>Rule conditions – the business logic expressed in the rule's condition groups.</li>
     * </ol>
     *
     * <p>All three are combined with {@code AND}, so an identity must satisfy all of them.
     *
     * <h3>Example (pseudo-SQL)</h3>
     * <pre>
     *   WHERE tenant_id = 'acme'
     *     AND status = 'ACTIVE'
     *     AND (department = 'Finance' OR location = 'NYC')
     * </pre>
     *
     * @param cb              the Criteria builder
     * @param root            the query root (FROM clause)
     * @param tenantId        the tenant value to filter by
     * @param identityClass   the entity class (used for field resolution)
     * @param conditionGroups the rule conditions
     * @param <T>             the identity type
     * @return a single combined predicate
     */
    private <T extends Identity> Predicate buildWhereClause(CriteriaBuilder cb,
                                                            Root<T> root,
                                                            String tenantId,
                                                            Class<T> identityClass,
                                                            List<Map<String, Object>> conditionGroups) {
        // cb.equal(root.get("tenantId"), tenantId) → WHERE tenant_id = 'acme'
        Predicate tenantPredicate = cb.equal(root.get("tenantId"), tenantId);

        // cb.equal(root.get("status"), IdentityStatus.ACTIVE) → WHERE status = 'ACTIVE'
        Predicate statusPredicate = cb.equal(root.get("status"), IdentityStatus.ACTIVE);

        // The rule conditions (may be complex nested AND/OR groups)
        Predicate conditionsPredicate = buildGroupedPredicate(cb, root, identityClass, conditionGroups);

        // cb.and(...) → combines all with AND
        return cb.and(tenantPredicate, statusPredicate, conditionsPredicate);
    }

    /**
     * Converts a list of condition groups (deserialized from the rule's JSON configuration) into
     * a single JPA {@link Predicate} that can be passed to the query's WHERE clause.
     *
     * <h3>What is a condition group?</h3>
     * <p>A condition group is a logical unit containing one or more individual conditions plus
     * optional nested sub-groups. Groups are combined between each other using an
     * {@code interGroupOperator} (AND or OR).
     *
     * <h3>Example rule JSON</h3>
     * <pre>
     * conditionGroups: [
     *   { groupOperator: "AND", interGroupOperator: "OR",
     *     conditions: [
     *       { field: "department", operator: "EQUALS", value: "Finance" }
     *     ]
     *   },
     *   { groupOperator: "AND", interGroupOperator: null,
     *     conditions: [
     *       { field: "location", operator: "EQUALS", value: "NYC" }
     *     ]
     *   }
     * ]
     * </pre>
     * <p>This produces: {@code (department = 'FINANCE') OR (location = 'NYC')}
     *
     * <h3>Algorithm</h3>
     * <ol>
     *   <li>Parse every group into a {@link GroupedCondition} record.</li>
     *   <li>Build a predicate for the first group.</li>
     *   <li>For each subsequent group, use the <em>previous</em> group's {@code interGroupOperator}
     *       to combine with the running accumulator.</li>
     * </ol>
     *
     * <p>If there are no condition groups, returns {@code cb.conjunction()} (always true),
     * meaning all active identities of this type will match.
     *
     * @param cb              the Criteria builder
     * @param root            the query root
     * @param identityClass   the entity class
     * @param conditionGroups list of group maps
     * @param <T>             the identity type
     * @return the combined predicate for all groups
     */
    @SuppressWarnings("unchecked")
    private <T extends Identity> Predicate buildGroupedPredicate(CriteriaBuilder cb,
                                                                 Root<T> root,
                                                                 Class<T> identityClass,
                                                                 List<Map<String, Object>> conditionGroups) {
        if (conditionGroups == null || conditionGroups.isEmpty()) {
            // cb.conjunction() is a predicate that always evaluates to TRUE.
            // Equivalent to writing no WHERE clause.
            return cb.conjunction();
        }

        List<GroupedCondition> groups = conditionGroups.stream()
                .map(group -> parseGroupedCondition((Map<String, Object>) group))
                .collect(Collectors.toList());

        if (groups.isEmpty()) {
            return cb.conjunction();
        }

        // Start the accumulator with the first group's predicate.
        Predicate accumulator = buildSingleGroupPredicate(cb, root, identityClass, groups.get(0));

        // Combine subsequent groups using the interGroupOperator of the *previous* group.
        // This follows the convention where the operator between group[i-1] and group[i]
        // is stored on group[i-1].
        for (int i = 1; i < groups.size(); i++) {
            Predicate current = buildSingleGroupPredicate(cb, root, identityClass, groups.get(i));
            accumulator = "OR".equals(groups.get(i - 1).interGroupOperator())
                    ? cb.or(accumulator, current)   // combines with OR
                    : cb.and(accumulator, current);  // combines with AND (default)
        }

        return accumulator;
    }

    /**
     * Builds a single {@link Predicate} for one condition group, combining:
     * <ul>
     *   <li>All individual conditions within the group using {@code groupOperator} (AND/OR).</li>
     *   <li>Any nested sub-groups, recursively.</li>
     * </ul>
     * If the group is marked {@code negated = true}, the entire result is wrapped with
     * {@code cb.not(...)}, which inverts the predicate.
     *
     * <h3>Example</h3>
     * <pre>
     * Group: { groupOperator: "OR", negated: true, conditions: [
     *   { field: "department", operator: "EQUALS", value: "HR" },
     *   { field: "department", operator: "EQUALS", value: "Legal" }
     * ]}
     * Result SQL: NOT (department = 'HR' OR department = 'Legal')
     * </pre>
     *
     * @param cb            the Criteria builder
     * @param root          the query root
     * @param identityClass the entity class
     * @param group         the parsed group
     * @param <T>           the identity type
     * @return the predicate for this single group
     */
    private <T extends Identity> Predicate buildSingleGroupPredicate(CriteriaBuilder cb,
                                                                     Root<T> root,
                                                                     Class<T> identityClass,
                                                                     GroupedCondition group) {
        List<Predicate> predicates = new ArrayList<>();

        // Build a predicate for each individual condition in this group.
        for (Map<String, Object> condition : group.conditions()) {
            predicates.add(buildSingleConditionPredicate(cb, root, identityClass, condition));
        }

        // Recursively build predicates for any sub-groups.
        if (!group.subGroups().isEmpty()) {
            Predicate subAccumulator =
                    buildSingleGroupPredicate(cb, root, identityClass, group.subGroups().get(0));
            for (int i = 1; i < group.subGroups().size(); i++) {
                Predicate current =
                        buildSingleGroupPredicate(cb, root, identityClass, group.subGroups().get(i));
                subAccumulator = "OR".equals(group.subGroups().get(i - 1).interGroupOperator())
                        ? cb.or(subAccumulator, current)
                        : cb.and(subAccumulator, current);
            }
            predicates.add(subAccumulator);
        }

        // Edge case: if no predicates were produced, return a trivially true/false value.
        if (predicates.isEmpty()) {
            // negated group with no conditions → disjunction (always false) means "exclude all"
            return group.negated() ? cb.disjunction() : cb.conjunction();
        }

        // Combine all predicates with the group's own operator.
        Predicate combined = "OR".equals(group.groupOperator())
                ? cb.or(predicates.toArray(new Predicate[0]))
                : cb.and(predicates.toArray(new Predicate[0]));

        // Apply negation if requested.
        return group.negated() ? cb.not(combined) : combined;
    }

    /**
     * Translates a single condition map (field + operator + value) into a JPA {@link Predicate}.
     *
     * <p>The condition map comes directly from the rule's JSON configuration. Example:
     * <pre>
     *   { "field": "department", "operator": "EQUALS", "value": "Finance" }
     * </pre>
     *
     * <p>The field name is first resolved through {@link IdentityFieldRegistry} to handle
     * differences between the logical name in the rule and the actual JPA attribute name.
     *
     * @param cb            the Criteria builder
     * @param root          the query root
     * @param identityClass the entity class (used by the field registry for resolution)
     * @param condition     the raw condition map
     * @param <T>           the identity type
     * @return the predicate for this single condition
     */
    private <T extends Identity> Predicate buildSingleConditionPredicate(CriteriaBuilder cb,
                                                                         Root<T> root,
                                                                         Class<T> identityClass,
                                                                         Map<String, Object> condition) {
        String rawField = stringValue(condition.get("field"));
        RuleOperator operator = RuleOperator.fromString(stringValue(condition.get("operator")));

        // Translate logical field name → actual JPA entity field name.
        String resolvedField = fieldRegistry.resolveField(rawField, identityClass);

        return buildFieldPredicate(cb, root, resolvedField, operator,
                condition.get("value"), condition.get("values"));
    }

    /**
     * Builds the actual database predicate for a single field comparison, handling all supported
     * {@link RuleOperator} values.
     *
     * <h3>Criteria API building blocks used here</h3>
     * <ul>
     *   <li>{@code root.get(field).as(String.class)} – gets the column as a String expression.
     *       This is like writing the column name in SQL, but type-safe.</li>
     *   <li>{@code cb.upper(path)} – wraps the expression in an SQL {@code UPPER()} function,
     *       enabling case-insensitive comparison.</li>
     *   <li>{@code cb.equal(a, b)} → {@code a = b}</li>
     *   <li>{@code cb.notEqual(a, b)} → {@code a != b}</li>
     *   <li>{@code cb.like(path, pattern, escape)} → {@code path LIKE 'pattern' ESCAPE '\'}</li>
     *   <li>{@code expression.in(list)} → {@code expression IN (v1, v2, ...)}</li>
     *   <li>{@code cb.not(pred)} → {@code NOT (pred)}</li>
     * </ul>
     *
     * <h3>Why UPPER() on both sides?</h3>
     * <p>All comparisons are done case-insensitively by converting both the database value and
     * the rule value to uppercase. This means {@code department = "finance"} matches an employee
     * whose {@code department} is {@code "Finance"} or {@code "FINANCE"}.
     *
     * <h3>Why escape LIKE patterns?</h3>
     * <p>SQL LIKE treats {@code %} and {@code _} as wildcards. If a user's rule value contains
     * a literal {@code %}, it must be escaped to prevent it from acting as a wildcard.
     * {@link LikePatternEscaper#escape} handles this.
     *
     * <h3>Example inputs and resulting SQL</h3>
     * <pre>
     *   field="department", operator=EQUALS, value="Finance"
     *   → UPPER(department) = 'FINANCE'
     *
     *   field="email", operator=CONTAINS, value="@acme.com"
     *   → UPPER(email) LIKE '%@ACME.COM%' ESCAPE '\'
     *
     *   field="location", operator=IN, values=["NYC","LON"]
     *   → UPPER(location) IN ('NYC', 'LON')
     * </pre>
     *
     * @param cb       the Criteria builder
     * @param root     the query root
     * @param field    the resolved JPA attribute name
     * @param operator the comparison operator
     * @param value    single value (used for most operators; also as fallback for IN/NOT_IN)
     * @param values   list value (preferred for IN/NOT_IN)
     * @return the predicate
     */
    private Predicate buildFieldPredicate(CriteriaBuilder cb,
                                          Root<?> root,
                                          String field,
                                          RuleOperator operator,
                                          Object value,
                                          Object values) {
        // Cast the field path to String and wrap with UPPER() for case-insensitive comparison.
        Expression<String> path = root.get(field).as(String.class);
        String strValue = value != null ? value.toString() : "";

        return switch (operator) {
            // UPPER(field) = UPPER(value)
            case EQUALS ->
                    cb.equal(cb.upper(path), strValue.toUpperCase(Locale.ROOT));

            // UPPER(field) != UPPER(value)
            case NOT_EQUALS ->
                    cb.notEqual(cb.upper(path), strValue.toUpperCase(Locale.ROOT));

            // UPPER(field) LIKE '%VALUE%' ESCAPE '\'
            case CONTAINS ->
                    cb.like(cb.upper(path),
                            "%" + LikePatternEscaper.escape(strValue).toUpperCase(Locale.ROOT) + "%",
                            '\\');

            // UPPER(field) LIKE 'VALUE%' ESCAPE '\'
            case STARTS_WITH ->
                    cb.like(cb.upper(path),
                            LikePatternEscaper.escape(strValue).toUpperCase(Locale.ROOT) + "%",
                            '\\');

            // UPPER(field) LIKE '%VALUE' ESCAPE '\'
            case ENDS_WITH ->
                    cb.like(cb.upper(path),
                            "%" + LikePatternEscaper.escape(strValue).toUpperCase(Locale.ROOT),
                            '\\');

            // UPPER(field) IN ('V1', 'V2', ...)
            case IN ->
                    cb.upper(path).in(resolveListValues(value, values).stream()
                            .map(item -> item.toUpperCase(Locale.ROOT))
                            .toList());

            // UPPER(field) NOT IN ('V1', 'V2', ...)
            case NOT_IN ->
                    cb.not(cb.upper(path).in(resolveListValues(value, values).stream()
                            .map(item -> item.toUpperCase(Locale.ROOT))
                            .toList()));
        };
    }

    /**
     * Parses a raw condition-group map (deserialized from JSON) into a strongly-typed
     * {@link GroupedCondition} record.
     *
     * <p>The map structure mirrors the rule JSON format:
     * <pre>
     * {
     *   "groupOperator": "AND",       // how conditions inside this group are combined
     *   "interGroupOperator": "OR",   // how this group connects to the next sibling group
     *   "negated": false,             // if true, the entire group's result is inverted
     *   "conditions": [ ... ],        // list of individual condition maps
     *   "conditionGroups": [ ... ]    // optional nested sub-groups
     * }
     * </pre>
     *
     * @param groupMap the raw map representing one condition group
     * @return a parsed {@link GroupedCondition} record
     */
    @SuppressWarnings("unchecked")
    private GroupedCondition parseGroupedCondition(Map<String, Object> groupMap) {
        String groupOperator = normalizeLogicalOperator(
                groupMap.get("groupOperator"),
                AccessRoleAssignmentRuleConstants.DEFAULT_GROUP_OPERATOR);

        String interGroupOperator = normalizeLogicalOperator(
                groupMap.get("interGroupOperator"),
                AccessRoleAssignmentRuleConstants.DEFAULT_INTER_GROUP_OPERATOR);

        boolean negated = groupMap.get("negated") instanceof Boolean value && value;

        // Collect individual conditions.
        List<Map<String, Object>> conditions = new ArrayList<>();
        Object conditionsObj = groupMap.get("conditions");
        if (conditionsObj instanceof List<?> conditionList) {
            for (Object item : conditionList) {
                if (item instanceof Map<?, ?> condition) {
                    conditions.add((Map<String, Object>) condition);
                }
            }
        }

        // Collect nested sub-groups (recursively parsed).
        List<GroupedCondition> subGroups = new ArrayList<>();
        Object subGroupsObj = groupMap.get("conditionGroups");
        if (subGroupsObj instanceof List<?> subGroupList) {
            for (Object item : subGroupList) {
                if (item instanceof Map<?, ?> subGroup) {
                    subGroups.add(parseGroupedCondition((Map<String, Object>) subGroup));
                }
            }
        }

        return new GroupedCondition(groupOperator, interGroupOperator, negated, conditions, subGroups);
    }

    /**
     * Loads a lightweight "snapshot" of identity field values for a list of identity IDs.
     *
     * <p>Instead of loading full entity objects (which could be large), this method runs a
     * Criteria API {@link Tuple} query that selects only the specific fields registered in
     * {@link IdentityFieldRegistry} for the given identity class.
     *
     * <h3>What is a Tuple query?</h3>
     * <p>A Tuple query is like a SQL query that returns multiple named columns in one row.
     * For example, instead of {@code SELECT * FROM employee}, you get
     * {@code SELECT id, display_name, department FROM employee}. Each result row is a
     * {@link Tuple} where you retrieve values by alias name: {@code tuple.get("displayName")}.
     *
     * <h3>Why use Tuple instead of loading the entity?</h3>
     * <p>Loading full entities activates lazy-loading, triggers Hibernate session management,
     * and brings all columns into memory — including blob fields. Tuple queries are lightweight
     * projections that only fetch what we need for audit/evidence display.
     *
     * <h3>Example</h3>
     * <pre>
     *   Input: identityIds = [uuid1, uuid2], identityClass = Employee.class
     *   Fields from registry: ["displayName", "department", "location"]
     *
     *   SQL generated:
     *   SELECT e.id, e.display_name, e.department, e.location
     *   FROM employee e
     *   WHERE e.tenant_id = 'acme' AND e.id IN (uuid1, uuid2)
     *
     *   Output: Map { uuid1 → IdentitySnapshot(id=uuid1, displayName="Alice", ...) }
     * </pre>
     *
     * @param tenantId      the tenant scope
     * @param identityIds   the IDs of identities to load
     * @param identityClass the entity class to query
     * @param <T>           the identity type
     * @return map from identity ID to its field snapshot
     */
    private <T extends Identity> Map<UUID, IdentitySnapshot> loadIdentitySnapshots(
            String tenantId,
            List<UUID> identityIds,
            Class<T> identityClass) {

        if (identityIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> fields = fieldRegistry.getFieldsForClass(identityClass);

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // createTupleQuery() → a query that returns multiple named columns per row.
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<T> root = query.from(identityClass);

        // Build the SELECT list: always include "id", then add each registered field.
        List<Selection<?>> selections = new ArrayList<>();
        selections.add(root.get("id").alias("id")); // alias gives each column a name

        for (String field : fields) {
            try {
                selections.add(root.get(field).alias(field));
            } catch (IllegalArgumentException ignored) {
                // Field might not exist on this particular identity subtype; skip silently.
            }
        }

        // multiselect(...) → SELECT id, field1, field2, ... (multiple columns)
        query.multiselect(selections);

        // WHERE tenant_id = ? AND id IN (uuid1, uuid2, ...)
        query.where(
                cb.equal(root.get("tenantId"), tenantId),
                root.get("id").in(identityIds));

        Map<UUID, IdentitySnapshot> snapshots = new HashMap<>();
        for (Tuple tuple : entityManager.createQuery(query).getResultList()) {
            UUID id = tuple.get("id", UUID.class); // retrieve by alias name

            Map<String, Object> fieldValues = new LinkedHashMap<>();
            for (String field : fields) {
                try {
                    Object fieldValue = tuple.get(field);
                    if (fieldValue != null) {
                        fieldValues.put(field, fieldValue.toString());
                    }
                } catch (IllegalArgumentException ignored) {
                    // Alias not in result set; skip.
                }
            }

            snapshots.put(id, new IdentitySnapshot(
                    id,
                    resolveDisplayName(fieldValues),
                    resolveIdentityType(identityClass),
                    fieldValues));
        }
        return snapshots;
    }

    // =========================================================================
    // Evidence building – for audit/debug output
    // =========================================================================

    /**
     * Builds a structured evidence map that explains <em>why</em> an identity matched (or
     * did not match) a rule.
     *
     * <p>The evidence is stored on the {@link AccessRoleAssignmentExecutionItem} and can be
     * displayed in an admin UI to let users understand rule behaviour.
     *
     * <h3>Example output structure</h3>
     * <pre>
     * {
     *   "overallMatch": true,
     *   "fields": { "department": "Finance", "location": "NYC" },
     *   "conditionGroups": [
     *     { "groupIndex": 0, "groupOperator": "AND", "conditions": [
     *       { "field": "department", "operator": "EQUALS",
     *         "expectedValue": "Finance", "actualValue": "Finance", "matched": true }
     *     ]}
     *   ]
     * }
     * </pre>
     *
     * @param snapshot        the identity's field values (may be {@code null} for revocations)
     * @param conditionGroups the rule's condition groups
     * @param identityClass   the identity type (for field resolution)
     * @param matched         whether the identity overall matched the rule
     * @param errorMessage    optional error message to embed; {@code null} if no error
     * @return the evidence map
     */
    private Map<String, Object> buildEvidence(IdentitySnapshot snapshot,
                                              List<Map<String, Object>> conditionGroups,
                                              Class<? extends Identity> identityClass,
                                              boolean matched,
                                              String errorMessage) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("overallMatch", matched);
        if (errorMessage != null) {
            evidence.put("error", errorMessage);
        }
        evidence.put("fields", snapshot != null ? snapshot.fieldValues() : Map.of());
        evidence.put("conditionGroups",
                buildGroupEvidence(conditionGroups,
                        snapshot != null ? snapshot.fieldValues() : Map.of(),
                        identityClass));
        return evidence;
    }

    /**
     * Converts a list of condition groups to their evidence representation.
     *
     * @param conditionGroups the rule condition groups
     * @param fieldValues     the identity's actual field values
     * @param identityClass   the identity type
     * @return list of evidence maps, one per group
     */
    private List<Map<String, Object>> buildGroupEvidence(List<Map<String, Object>> conditionGroups,
                                                         Map<String, Object> fieldValues,
                                                         Class<? extends Identity> identityClass) {
        if (conditionGroups == null || conditionGroups.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (int i = 0; i < conditionGroups.size(); i++) {
            results.add(buildSingleGroupEvidence(conditionGroups.get(i), i, fieldValues, identityClass));
        }
        return results;
    }

    /**
     * Builds an evidence map for one condition group, recursively handling sub-groups.
     *
     * <p>The evidence shows the group's configuration and for each condition: the expected value,
     * the actual value found on the identity, and whether that condition evaluated to true.
     *
     * @param group         the raw group map
     * @param index         the 0-based index of this group among its siblings (for display)
     * @param fieldValues   the identity's actual field values
     * @param identityClass the identity type
     * @return evidence map for this group
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> buildSingleGroupEvidence(Map<String, Object> group,
                                                         int index,
                                                         Map<String, Object> fieldValues,
                                                         Class<? extends Identity> identityClass) {
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("groupIndex", index);
        evidence.put("groupOperator",
                normalizeLogicalOperator(group.get("groupOperator"),
                        AccessRoleAssignmentRuleConstants.DEFAULT_GROUP_OPERATOR));
        evidence.put("interGroupOperator",
                normalizeLogicalOperator(group.get("interGroupOperator"),
                        AccessRoleAssignmentRuleConstants.DEFAULT_INTER_GROUP_OPERATOR));
        evidence.put("negated", group.get("negated") instanceof Boolean value && value);

        // Build per-condition evidence.
        List<Map<String, Object>> conditionEvidence = new ArrayList<>();
        Object conditionsObj = group.get("conditions");
        if (conditionsObj instanceof List<?> conditions) {
            for (Object item : conditions) {
                if (item instanceof Map<?, ?> condition) {
                    conditionEvidence.add(buildConditionEvidence(
                            (Map<String, Object>) condition, fieldValues, identityClass));
                }
            }
        }
        evidence.put("conditions", conditionEvidence);

        // Build per-sub-group evidence (recursive).
        List<Map<String, Object>> subGroupEvidence = new ArrayList<>();
        Object subGroupsObj = group.get("conditionGroups");
        if (subGroupsObj instanceof List<?> subGroups) {
            int childIndex = 0;
            for (Object item : subGroups) {
                if (item instanceof Map<?, ?> subGroup) {
                    subGroupEvidence.add(buildSingleGroupEvidence(
                            (Map<String, Object>) subGroup, childIndex++, fieldValues, identityClass));
                }
            }
        }
        if (!subGroupEvidence.isEmpty()) {
            evidence.put("conditionGroups", subGroupEvidence);
        }

        return evidence;
    }

    /**
     * Builds an evidence entry for one individual condition, showing the expected vs actual value
     * and whether the condition matched in-memory.
     *
     * <p>Note: this is an in-memory re-evaluation of the same logic that was applied in the
     * database query. It exists so the audit log can show per-condition match results without
     * running another database query.
     *
     * @param condition     the raw condition map
     * @param fieldValues   the identity's actual field values (already loaded)
     * @param identityClass the identity type (for field resolution)
     * @return evidence map with field, operator, expectedValue, actualValue, and matched
     */
    private Map<String, Object> buildConditionEvidence(Map<String, Object> condition,
                                                       Map<String, Object> fieldValues,
                                                       Class<? extends Identity> identityClass) {
        String resolvedField = fieldRegistry.resolveField(
                stringValue(condition.get("field")), identityClass);
        String operator = stringValue(condition.get("operator"));
        Object actualValue = fieldValues.get(resolvedField);
        Object expectedValue = condition.get("values") != null
                ? condition.get("values")
                : condition.get("value");

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("field", resolvedField);
        evidence.put("operator", operator);
        evidence.put("expectedValue", expectedValue);
        evidence.put("actualValue", actualValue);
        evidence.put("matched", evaluateConditionInMemory(
                actualValue, operator, condition.get("value"), condition.get("values")));
        return evidence;
    }

    /**
     * Evaluates a single condition against an identity's actual field value, entirely in memory
     * (no database call).
     *
     * <p>This mirrors the SQL predicates built by {@link #buildFieldPredicate} but operates on
     * already-loaded Java strings. It is used exclusively for generating evidence output.
     *
     * <h3>Example</h3>
     * <pre>
     *   actualValue = "Finance"
     *   operator    = "EQUALS"
     *   expectedValue = "finance"
     *   → returns true  (case-insensitive comparison)
     *
     *   actualValue = "alice@acme.com"
     *   operator    = "ENDS_WITH"
     *   expectedValue = "@acme.com"
     *   → returns true
     *
     *   actualValue = null
     *   operator    = "NOT_EQUALS"
     *   → returns true  (null is "not equal" to anything)
     * </pre>
     *
     * @param actualValue    the identity's current field value (may be {@code null})
     * @param operator       the operator string (e.g. "EQUALS", "CONTAINS")
     * @param expectedValue  single expected value
     * @param expectedValues multi-value expected list (for IN / NOT_IN)
     * @return {@code true} if the condition is satisfied
     */
    private boolean evaluateConditionInMemory(Object actualValue,
                                              String operator,
                                              Object expectedValue,
                                              Object expectedValues) {
        if (actualValue == null) {
            // Null fields only satisfy NOT_EQUALS and NOT_IN.
            return "NOT_EQUALS".equalsIgnoreCase(operator) || "NOT_IN".equalsIgnoreCase(operator);
        }

        String actual = actualValue.toString().toUpperCase(Locale.ROOT);
        String expected = expectedValue != null ? expectedValue.toString().toUpperCase(Locale.ROOT) : "";

        return switch (operator.toUpperCase(Locale.ROOT)) {
            case "EQUALS"      -> actual.equals(expected);
            case "NOT_EQUALS"  -> !actual.equals(expected);
            case "CONTAINS"    -> actual.contains(expected);
            case "STARTS_WITH" -> actual.startsWith(expected);
            case "ENDS_WITH"   -> actual.endsWith(expected);
            case "IN" -> resolveListValues(expectedValue, expectedValues).stream()
                    .map(item -> item.toUpperCase(Locale.ROOT))
                    .anyMatch(actual::equals);
            case "NOT_IN" -> resolveListValues(expectedValue, expectedValues).stream()
                    .map(item -> item.toUpperCase(Locale.ROOT))
                    .noneMatch(actual::equals);
            default -> false;
        };
    }

    // =========================================================================
    // Utility helpers
    // =========================================================================

    /**
     * Returns the list of identity types that the rule targets.
     *
     * <p>If the rule has no explicit identity types configured, defaults to
     * {@link AccessRoleAssignmentRuleConstants#DEFAULT_IDENTITY_TYPE} (typically "EMPLOYEE").
     * All values are normalized to uppercase and deduplicated.
     *
     * <p>Example:
     * <pre>
     *   rule.identityTypes = ["employee", "NON_EMPLOYEE"]
     *   → ["EMPLOYEE", "NON_EMPLOYEE"]
     * </pre>
     *
     * @param rule the rule to inspect
     * @return non-empty, deduplicated list of identity type strings
     */
    private List<String> resolveIdentityTypes(AccessRoleAssignmentRule rule) {
        if (rule.getIdentityTypes() == null || rule.getIdentityTypes().isEmpty()) {
            return List.of(AccessRoleAssignmentRuleConstants.DEFAULT_IDENTITY_TYPE);
        }
        return rule.getIdentityTypes().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    /**
     * Maps an identity type string to the corresponding JPA entity class.
     *
     * <p>This class is then used as the "FROM" table in Criteria API queries.
     *
     * <p>Example:
     * <pre>
     *   "EMPLOYEE"     → Employee.class
     *   "NON_EMPLOYEE" → NonEmployee.class
     *   "NON_HUMAN"    → NonHumanIdentity.class
     * </pre>
     *
     * @param identityType the identity type string (should be uppercase)
     * @return the corresponding entity class
     * @throws IllegalArgumentException if the identity type is not recognised
     */
    private Class<? extends Identity> resolveIdentityClass(String identityType) {
        return switch (identityType) {
            case "EMPLOYEE"     -> Employee.class;
            case "NON_EMPLOYEE" -> NonEmployee.class;
            case "NON_HUMAN"    -> NonHumanIdentity.class;
            default -> throw new IllegalArgumentException(
                    AccessRoleAssignmentRuleConstants.ERROR_INVALID_IDENTITY_TYPE + ": " + identityType);
        };
    }

    /**
     * Maps a JPA entity class back to its string identity type label.
     *
     * <p>This is the reverse of {@link #resolveIdentityClass(String)} and is used when storing
     * the identity type on snapshot/audit records.
     *
     * @param identityClass the entity class
     * @return "EMPLOYEE", "NON_EMPLOYEE", "NON_HUMAN", or "IDENTITY" if unknown
     */
    private String resolveIdentityType(Class<? extends Identity> identityClass) {
        if (Employee.class.isAssignableFrom(identityClass)) return "EMPLOYEE";
        if (NonEmployee.class.isAssignableFrom(identityClass)) return "NON_EMPLOYEE";
        if (NonHumanIdentity.class.isAssignableFrom(identityClass)) return "NON_HUMAN";
        return "IDENTITY";
    }

    /**
     * Updates execution scheduling metadata after a rule run attempt.
     *
     * <p>This records the latest evaluation timestamp for observability and recalculates
     * the next CRON firing time when applicable. For non-CRON or disabled rules,
     * {@code nextExecutionAt} is cleared.
     *
     * @param rule the rule to update and persist
     */
    private void updateRuleExecutionMetadata(AccessRoleAssignmentRule rule) {
        rule.setLastEvaluatedAt(Instant.now().toEpochMilli());
        applyNextExecution(rule);
        ruleRepository.save(rule);
    }

    /**
     * Calculates and sets {@code nextExecutionAt} on a CRON-scheduled rule after a run.
     *
     * <p>Three scheduling strategies are supported (in priority order):
     * <ol>
     *   <li><b>Cron expression</b> – e.g. {@code "0 0 8 * * MON-FRI"} (weekdays at 08:00).
     *       Uses Spring's {@link CronExpression} to compute the exact next firing time.</li>
     *   <li><b>Fixed date + time</b> – e.g. date {@code "2025-12-31"}, time {@code "09:00"}.
     *       The next execution is that specific datetime, provided it is still in the future.</li>
     *   <li><b>Daily time</b> – e.g. time {@code "14:00"} (no date).
     *       If today's occurrence has already passed, schedules for tomorrow at the same time.</li>
     * </ol>
     *
     * <p>If the rule is disabled or not of CRON type, {@code nextExecutionAt} is set to
     * {@code null} so no further scheduling occurs.
     *
     * @param rule the rule to update
     */
    private void applyNextExecution(AccessRoleAssignmentRule rule) {
        if (!rule.isEnabled() || rule.getScheduleType() != AccessRoleAssignmentRule.ScheduleType.CRON) {
            rule.setNextExecutionAt(null);
            return;
        }

        ZoneId zone = resolveTimezone(rule.getScheduleTimezone());

        // Priority 1: standard cron expression.
        if (rule.getCronExpression() != null && !rule.getCronExpression().isBlank()) {
            LocalDateTime next = CronExpression.parse(rule.getCronExpression())
                    .next(LocalDateTime.now(zone));
            rule.setNextExecutionAt(next != null ? next.atZone(zone).toInstant().toEpochMilli() : null);
            return;
        }

        // Priority 2: explicit date + time.
        if (rule.getScheduleTime() != null && !rule.getScheduleTime().isBlank()
                && rule.getScheduleDate() != null && !rule.getScheduleDate().isBlank()) {
            LocalDateTime scheduled = LocalDateTime.of(
                    parseScheduleDate(rule.getScheduleDate()),
                    parseScheduleTime(rule.getScheduleTime()));
            long epochMs = scheduled.atZone(zone).toInstant().toEpochMilli();
            // Only schedule if the target datetime is still in the future.
            rule.setNextExecutionAt(epochMs > Instant.now().toEpochMilli() ? epochMs : null);
            return;
        }

        // Priority 3: daily time (no specific date).
        if (rule.getScheduleTime() != null && !rule.getScheduleTime().isBlank()) {
            LocalDate today = LocalDate.now(zone);
            LocalDateTime candidate = LocalDateTime.of(today, parseScheduleTime(rule.getScheduleTime()));
            if (candidate.atZone(zone).toInstant().toEpochMilli() <= Instant.now().toEpochMilli()) {
                // Today's time has passed → schedule for tomorrow.
                candidate = LocalDateTime.of(today.plusDays(1), parseScheduleTime(rule.getScheduleTime()));
            }
            rule.setNextExecutionAt(candidate.atZone(zone).toInstant().toEpochMilli());
        }
    }

    /**
     * Resolves a timezone string to a {@link ZoneId}, defaulting to UTC if blank or null.
     *
     * @param timezone the timezone string (e.g. "America/New_York", "UTC+5:30")
     * @return the corresponding ZoneId
     * @throws java.time.zone.ZoneRulesException if the timezone string is invalid
     */
    private ZoneId resolveTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("UTC");
        }
        return ZoneId.of(timezone.trim());
    }

    /**
     * Parses a time string in {@code HH:mm} format into a {@link LocalTime}.
     *
     * @param scheduleTime the time string, e.g. {@code "08:30"}
     * @return the parsed LocalTime
     */
    private LocalTime parseScheduleTime(String scheduleTime) {
        return LocalTime.parse(scheduleTime, SCHEDULE_TIME_FORMAT);
    }

    /**
     * Parses a date string in {@code yyyy-MM-dd} format into a {@link LocalDate}.
     *
     * @param scheduleDate the date string, e.g. {@code "2025-12-31"}
     * @return the parsed LocalDate
     */
    private LocalDate parseScheduleDate(String scheduleDate) {
        return LocalDate.parse(scheduleDate, SCHEDULE_DATE_FORMAT);
    }

    /**
     * Resolves the list of string values to use for {@code IN} / {@code NOT_IN} predicates.
     *
     * <p>Priority:
     * <ol>
     *   <li>If {@code values} is a non-empty {@link List}, use it directly (each element
     *       is converted to a string).</li>
     *   <li>If {@code value} is a comma-separated string (e.g. {@code "NYC,LON,PAR"}),
     *       split it and trim each token.</li>
     *   <li>Otherwise return an empty list.</li>
     * </ol>
     *
     * <p>Example:
     * <pre>
     *   values = ["NYC", "LON"]  → ["NYC", "LON"]
     *   value  = "NYC,LON, PAR" → ["NYC", "LON", "PAR"]
     * </pre>
     *
     * @param value  a single value or comma-separated values string
     * @param values a list of values (takes priority over {@code value})
     * @return resolved list of strings
     */
    private List<String> resolveListValues(Object value, Object values) {
        if (values instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(Object::toString).toList();
        }
        if (value != null) {
            return Arrays.stream(value.toString().split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .toList();
        }
        return List.of();
    }

    /**
     * Determines the best human-readable display name for an identity from its field values.
     *
     * <p>Resolution priority:
     * <ol>
     *   <li>{@code displayName} field (if non-blank)</li>
     *   <li>{@code firstName} + {@code lastName} concatenated</li>
     *   <li>{@code username}</li>
     *   <li>{@code serviceName} (for non-human identities like service accounts)</li>
     *   <li>Fallback: "Unknown"</li>
     * </ol>
     *
     * @param fieldValues the identity's loaded field values
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
        return serviceName != null ? serviceName.toString() : "Unknown";
    }

    /**
     * Returns the display name for an {@link AccessRole}.
     *
     * <p>Uses {@code displayName} if set and non-blank; falls back to {@code name}.
     *
     * @param role the access role entity
     * @return the display name
     */
    private String resolveRoleDisplayName(AccessRole role) {
        return role.getDisplayName() != null && !role.getDisplayName().isBlank()
                ? role.getDisplayName()
                : role.getName();
    }

    /**
     * Normalizes a logical operator value ({@code "and"}, {@code "AND"}, {@code "or"}, etc.)
     * to either {@code "AND"} or {@code "OR"}.
     *
     * <p>If the input is null or blank, the supplied {@code defaultValue} is used instead.
     * Any value that is not {@code "OR"} after uppercasing is treated as {@code "AND"}.
     *
     * <p>Example:
     * <pre>
     *   normalizeLogicalOperator("or",  "AND") → "OR"
     *   normalizeLogicalOperator(null,  "AND") → "AND"
     *   normalizeLogicalOperator("xyz", "OR")  → "AND"  (unrecognised → defaults to AND)
     * </pre>
     *
     * @param value        the raw operator value (from the rule JSON)
     * @param defaultValue the value to use when {@code value} is blank/null
     * @return "AND" or "OR"
     */
    private String normalizeLogicalOperator(Object value, String defaultValue) {
        String operator = (value == null || value.toString().isBlank())
                ? defaultValue
                : value.toString().trim().toUpperCase(Locale.ROOT);
        return "OR".equals(operator) ? "OR" : "AND";
    }

    /**
     * Safely converts an object to its {@link String} representation.
     *
     * @param value the object to convert
     * @return {@code value.toString()} or {@code null} if {@code value} is {@code null}
     */
    private String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    /**
     * Creates a validated {@link Pageable} from page index and page size.
     *
     * <p>Negative page indices are clamped to 0. Page size is clamped to 1–200 via
     * {@link #normalizePageSize(int)}.
     *
     * @param page zero-based page index
     * @param size requested page size
     * @return a ready-to-use Pageable
     */
    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), normalizePageSize(size));
    }

    /**
     * Clamps the requested page size to a valid range.
     *
     * <ul>
     *   <li>If {@code size <= 0}, defaults to 20.</li>
     *   <li>If {@code size > 200}, caps at 200 to prevent oversized queries.</li>
     * </ul>
     *
     * @param size the requested page size
     * @return a page size in the range [1, 200]
     */
    private int normalizePageSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, 200);
    }

    /**
     * Validates that the tenant ID is present.
     *
     * @param tenantId the tenant ID to validate
     * @throws IllegalArgumentException if {@code tenantId} is null or blank
     */
    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
        }
    }

    private void validateStartedAtRange(Long startedFrom, Long startedTo) {
        if (startedFrom != null && startedTo != null && startedFrom > startedTo) {
            throw new IllegalArgumentException("startedFrom must be less than or equal to startedTo");
        }
    }


    // =========================================================================
    // Inner types
    // =========================================================================

    /**
     * Mutable counter bag accumulates statistics across the identity-evaluation loop.
     *
     * <p>Using a dedicated object (rather than many local variables) makes it easy to pass
     * counters through helper methods and write them back to the execution record at the end.
     *
     * <p>All fields start at 0 (Java default for {@code int}).
     */
    private static final class ExecutionCounters {
        /** Total number of identities that matched the rule's conditions. */
        private int identitiesMatched;

        /** Number of new role assignments created during this execution. */
        private int rolesAssigned;

        /** Number of previously revoked assignments that were re-activated. */
        private int rolesReactivated;

        /** Number of assignments revoked because the identity no longer matches. */
        private int rolesRevoked;

        /** Number of identities skipped because they already had an active assignment. */
        private int skippedAlreadyAssigned;

        /** Number of identities skipped because they did not match (informational; rarely used). */
        private int skippedNotMatched;

        /** Number of identities that caused an exception during processing. */
        private int errorCount;

        int identitiesMatched()    { return identitiesMatched; }
        int rolesAssigned()        { return rolesAssigned; }
        int rolesReactivated()     { return rolesReactivated; }
        int rolesRevoked()         { return rolesRevoked; }
        int skippedAlreadyAssigned() { return skippedAlreadyAssigned; }
        int skippedNotMatched()    { return skippedNotMatched; }
        int errorCount()           { return errorCount; }
    }

    /**
     * Immutable snapshot of an identity's field values at the time of rule evaluation.
     *
     * <p>Capturing a snapshot (instead of holding a reference to the entity) ensures that:
     * <ul>
     *   <li>The audit evidence reflects the state at evaluation time, not a later state.</li>
     *   <li>No JPA session is needed to access field values after the query is closed.</li>
     * </ul>
     *
     * @param id           the identity's UUID
     * @param displayName  the resolved human-readable name (see {@link #resolveDisplayName})
     * @param identityType the type string ("EMPLOYEE", "NON_EMPLOYEE", or "NON_HUMAN")
     * @param fieldValues  map of field name → string value for all registered fields
     */
    private record IdentitySnapshot(
            UUID id,
            String displayName,
            String identityType,
            Map<String, Object> fieldValues) {
    }

    /**
     * Strongly-typed representation of one condition group parsed from the rule's JSON config.
     *
     * <p>A condition group can contain:
     * <ul>
     *   <li>Individual conditions ({@code field, operator, value}).</li>
     *   <li>Nested sub-groups (recursive structure).</li>
     * </ul>
     *
     * @param groupOperator      how conditions <em>within</em> this group are combined ("AND"/"OR")
     * @param interGroupOperator how this group connects to the <em>next sibling</em> group
     * @param negated            if {@code true}, the entire group's result is negated (NOT)
     * @param conditions         individual condition maps within this group
     * @param subGroups          nested child groups
     */
    private record GroupedCondition(
            String groupOperator,
            String interGroupOperator,
            boolean negated,
            List<Map<String, Object>> conditions,
            List<GroupedCondition> subGroups) {
    }
}