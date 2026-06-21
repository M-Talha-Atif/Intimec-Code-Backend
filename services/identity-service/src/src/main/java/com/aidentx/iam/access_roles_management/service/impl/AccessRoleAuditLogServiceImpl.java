package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditEvent;
import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditLogConstants;
import com.aidentx.iam.access_roles_management.mapper.AccessRoleAuditLogMapper;
import com.aidentx.iam.access_roles_management.model.dto.AccessRoleAuditHistoryDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAuditLogRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAuditLogService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link AccessRoleAuditLogService}.
 *
 * <p>This service persists append-only audit history entries for access role
 * operations and exposes tenant-scoped history queries for UI screens.</p>
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Map in-memory audit events to persistent audit entities</li>
 *   <li>Persist audit records safely without affecting business operations</li>
 *   <li>Query audit history using tenant-scoped filters</li>
 *   <li>Map persistent entities to UI-friendly DTOs</li>
 * </ul>
 */
@Service
public class AccessRoleAuditLogServiceImpl implements AccessRoleAuditLogService {

    private static final String DETAIL_ACTION = "action";
    private static final String DETAIL_ROLE_ID = "roleId";
      private static final String DETAIL_RULE_ID = "ruleId";
    private static final String DETAIL_TENANT_ID = "tenantId";
      private static final String DETAIL_SEARCH = "search";
    private static final String DETAIL_EXCEPTION_TYPE = "exceptionType";
    private static final String DETAIL_EXCEPTION_MESSAGE = "exceptionMessage";

    private final AccessRoleAuditLogRepository repository;
    private final AccessRoleAuditLogMapper mapper;
    private final IUnifiedLogger logger;

    /**
     * Constructs the audit log service.
     *
     * @param repository persistent repository for access role audit logs
     * @param mapper mapper for event/entity/dto conversion
     * @param loggerFactory unified logger factory
     */
    public AccessRoleAuditLogServiceImpl(
            AccessRoleAuditLogRepository repository,
            AccessRoleAuditLogMapper mapper,
            UnifiedLoggerFactory loggerFactory) {
        this.repository = repository;
        this.mapper = mapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAuditLogConstants.LOG_CATEGORY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void persist(AccessRoleAuditEvent event) {
        try {
            logger.info()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_PERSISTING_AUDIT_LOG)
                    .details(Map.of(
                            DETAIL_ACTION, event.action(),
                            DETAIL_ROLE_ID, event.entityId() != null ? event.entityId() : "null",
                            DETAIL_TENANT_ID, event.tenantId() != null ? event.tenantId() : "null"
                    ))
                    .log();

            AccessRoleAuditLog entity = mapper.eventToEntity(event);

            logger.debug()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_COMPUTED_CHANGED_FIELDS)
                    .details(Map.of(
                            DETAIL_ACTION, event.action(),
                            DETAIL_ROLE_ID, event.entityId() != null ? event.entityId() : "null",
                            "changedFieldCount", String.valueOf(entity.getChangedFields() != null ? entity.getChangedFields().size() : 0)
                    ))
                    .log();

            repository.save(entity);

            logger.info()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_AUDIT_LOG_PERSISTED)
                    .details(Map.of(
                            DETAIL_ACTION, entity.getAction(),
                            DETAIL_ROLE_ID, entity.getRoleId() != null ? entity.getRoleId().toString() : "null",
                            DETAIL_TENANT_ID, entity.getTenantId() != null ? entity.getTenantId() : "null"
                    ))
                    .log();
        } catch (Exception e) {
            logger.error()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_AUDIT_LOG_PERSIST_FAILED)
                    .details(Map.of(
                            DETAIL_ACTION, event.action() != null ? event.action() : "null",
                            DETAIL_ROLE_ID, event.entityId() != null ? event.entityId() : "null",
                            DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName(),
                            DETAIL_EXCEPTION_MESSAGE, e.getMessage() != null ? e.getMessage() : "null"
                    ))
                    .log();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void persist(AccessRoleAuditLog auditLog) {
        try {
            logger.info()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_PERSISTING_AUDIT_LOG)
                    .details(Map.of(
                            DETAIL_ACTION, auditLog.getAction() != null ? auditLog.getAction() : "null",
                            DETAIL_ROLE_ID, auditLog.getRoleId() != null ? auditLog.getRoleId().toString() : "null",
                            DETAIL_TENANT_ID, auditLog.getTenantId() != null ? auditLog.getTenantId() : "null"
                    ))
                    .log();

            repository.save(auditLog);

            logger.info()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_AUDIT_LOG_PERSISTED)
                    .details(Map.of(
                            DETAIL_ACTION, auditLog.getAction() != null ? auditLog.getAction() : "null",
                            DETAIL_ROLE_ID, auditLog.getRoleId() != null ? auditLog.getRoleId().toString() : "null",
                            DETAIL_TENANT_ID, auditLog.getTenantId() != null ? auditLog.getTenantId() : "null"
                    ))
                    .log();
        } catch (Exception e) {
            logger.error()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_AUDIT_LOG_PERSIST_FAILED)
                    .details(Map.of(
                            DETAIL_ACTION, auditLog.getAction() != null ? auditLog.getAction() : "null",
                            DETAIL_ROLE_ID, auditLog.getRoleId() != null ? auditLog.getRoleId().toString() : "null",
                            DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName(),
                            DETAIL_EXCEPTION_MESSAGE, e.getMessage() != null ? e.getMessage() : "null"
                    ))
                    .log();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AccessRoleAuditLog> findByRoleId(String tenantId, UUID roleId, Pageable pageable) {
        return repository.findByTenantIdAndRoleId(tenantId, roleId, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AccessRoleAuditLog> findByRuleId(String tenantId, UUID ruleId, Pageable pageable) {
        return repository.findByTenantIdAndRuleId(tenantId, ruleId, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AccessRoleAuditLog> findByAction(String tenantId, String action, Pageable pageable) {
        return repository.findByTenantIdAndAction(tenantId, action, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AccessRoleAuditLog> findByActor(String tenantId, String actorUsername, Pageable pageable) {
        return repository.findByTenantIdAndActorUsername(tenantId, actorUsername, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AccessRoleAuditLog> findByDateRange(String tenantId, Long from, Long to, Pageable pageable) {
        return repository.findByTenantIdAndCreatedAtBetween(tenantId, from, to, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AccessRoleAuditLog> findByRoleIdAndAction(String tenantId, UUID roleId, String action, Pageable pageable) {
        return repository.findByTenantIdAndRoleIdAndAction(tenantId, roleId, action, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AccessRoleAuditLog> findByRuleIdAndAction(String tenantId, UUID ruleId, String action, Pageable pageable) {
        return repository.findByTenantIdAndRuleIdAndAction(tenantId, ruleId, action, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AccessRoleAuditHistoryDto> findAuditHistory(String tenantId,
                                                           UUID roleId,
                                                           String action,
                                                           String search,
                                                           Pageable pageable) {
        try {
            String normalizedAction = normalizeFilter(action);
            String normalizedSearch = normalizeFilter(search);

            logger.info()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_QUERY_AUDIT_HISTORY)
                    .details(Map.of(
                            DETAIL_TENANT_ID, tenantId,
                            DETAIL_ROLE_ID, roleId.toString(),
                            DETAIL_ACTION, normalizedAction != null ? normalizedAction : "ALL",
                            DETAIL_SEARCH, normalizedSearch != null ? normalizedSearch : "ALL",
                            "page", String.valueOf(pageable.getPageNumber()),
                            "size", String.valueOf(pageable.getPageSize())
                    ))
                    .log();

            Page<AccessRoleAuditLog> result = findRoleAuditLogs(
                    tenantId,
                    roleId,
                    normalizedAction,
                    normalizedSearch,
                    pageable);

            Page<AccessRoleAuditHistoryDto> dtoPage = result.map(mapper::entityToHistoryDto);

            logger.info()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_QUERY_AUDIT_HISTORY_DONE)
                    .details(Map.of(
                            DETAIL_TENANT_ID, tenantId,
                            DETAIL_ROLE_ID, roleId.toString(),
                            DETAIL_ACTION, normalizedAction != null ? normalizedAction : "ALL",
                            DETAIL_SEARCH, normalizedSearch != null ? normalizedSearch : "ALL",
                            "resultCount", String.valueOf(dtoPage.getNumberOfElements())
                    ))
                    .log();

            return dtoPage;
        } catch (Exception e) {
            logger.error()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_QUERY_AUDIT_HISTORY_FAILED)
                    .details(Map.of(
                            DETAIL_TENANT_ID, tenantId,
                            DETAIL_ROLE_ID, roleId.toString(),
                            DETAIL_ACTION, action != null ? action : "ALL",
                            DETAIL_SEARCH, search != null ? search : "ALL",
                            DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName(),
                            DETAIL_EXCEPTION_MESSAGE, e.getMessage() != null ? e.getMessage() : "null"
                    ))
                    .log();
            throw e;
        }
    }

    private Page<AccessRoleAuditLog> findRoleAuditLogs(String tenantId,
                                                       UUID roleId,
                                                       String action,
                                                       String search,
                                                       Pageable pageable) {
        if (action != null && search != null) {
            return repository.searchByTenantIdAndRoleIdAndActionAndActor(tenantId, roleId, action, search, pageable);
        }
        if (search != null) {
            return repository.searchByTenantIdAndRoleIdAndActor(tenantId, roleId, search, pageable);
        }
        if (action != null) {
            return repository.findByTenantIdAndRoleIdAndAction(tenantId, roleId, action, pageable);
        }
        return repository.findByTenantIdAndRoleId(tenantId, roleId, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<AccessRoleAuditHistoryDto> findRuleAuditHistory(String tenantId,
                                                               UUID ruleId,
                                                               String action,
                                                               String search,
                                                               Pageable pageable) {
        try {
            String normalizedAction = normalizeFilter(action);
            String normalizedSearch = normalizeFilter(search);

            logger.info()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_QUERY_AUDIT_HISTORY)
                    .details(Map.of(
                            DETAIL_TENANT_ID, tenantId,
                            DETAIL_RULE_ID, ruleId.toString(),
                            DETAIL_ACTION, normalizedAction != null ? normalizedAction : "ALL",
                            DETAIL_SEARCH, normalizedSearch != null ? normalizedSearch : "ALL",
                            "page", String.valueOf(pageable.getPageNumber()),
                            "size", String.valueOf(pageable.getPageSize())
                    ))
                    .log();

            Page<AccessRoleAuditLog> result = findRuleAuditLogs(
                    tenantId,
                    ruleId,
                    normalizedAction,
                    normalizedSearch,
                    pageable);

            Page<AccessRoleAuditHistoryDto> dtoPage = result.map(mapper::entityToHistoryDto);

            logger.info()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_QUERY_AUDIT_HISTORY_DONE)
                    .details(Map.of(
                            DETAIL_TENANT_ID, tenantId,
                            DETAIL_RULE_ID, ruleId.toString(),
                            DETAIL_ACTION, normalizedAction != null ? normalizedAction : "ALL",
                            DETAIL_SEARCH, normalizedSearch != null ? normalizedSearch : "ALL",
                            "resultCount", String.valueOf(dtoPage.getNumberOfElements())
                    ))
                    .log();

            return dtoPage;
        } catch (Exception e) {
            logger.error()
                    .category(AccessRoleAuditLogConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditLogConstants.LOG_QUERY_AUDIT_HISTORY_FAILED)
                    .details(Map.of(
                            DETAIL_TENANT_ID, tenantId,
                            DETAIL_RULE_ID, ruleId.toString(),
                            DETAIL_ACTION, action != null ? action : "ALL",
                            DETAIL_SEARCH, search != null ? search : "ALL",
                            DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName(),
                            DETAIL_EXCEPTION_MESSAGE, e.getMessage() != null ? e.getMessage() : "null"
                    ))
                    .log();
            throw e;
        }
    }

    private Page<AccessRoleAuditLog> findRuleAuditLogs(String tenantId,
                                                       UUID ruleId,
                                                       String action,
                                                       String search,
                                                       Pageable pageable) {
        if (action != null && search != null) {
            return repository.searchByTenantIdAndRuleIdAndActionAndActor(tenantId, ruleId, action, search, pageable);
        }
        if (search != null) {
            return repository.searchByTenantIdAndRuleIdAndActor(tenantId, ruleId, search, pageable);
        }
        if (action != null) {
            return repository.findByTenantIdAndRuleIdAndAction(tenantId, ruleId, action, pageable);
        }
        return repository.findByTenantIdAndRuleId(tenantId, ruleId, pageable);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

