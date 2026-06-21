package com.aidentx.iam.access_roles_management.service.import_jobs;
import com.aidentx.iam.access_roles_management.audit.RoleImportJobAuditConstants;
import com.aidentx.iam.access_roles_management.audit.RoleImportJobAuditEvent;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleImportConstants;
import com.aidentx.iam.access_roles_management.model.dto.request.RoleImportRowDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleImportJob;
import com.aidentx.iam.access_roles_management.model.entity.RoleImportJobItem;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemPhase;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemStatus;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportJobStatus;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.RoleImportJobItemRepository;
import com.aidentx.iam.access_roles_management.repository.RoleImportJobRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleService;
import com.aidentx.iam.access_roles_management.service.AccessRoleCompositionService;
import com.aidentx.iam.access_roles_management.service.AccessRoleService;
import com.aidentx.iam.access_roles_management.service.IdentityRoleAssignmentService;
import com.aidentx.iam.access_roles_management.audit.RoleImportJobAuditPublisher;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.organization_management.repository.OrgUnitRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class RoleImportJobWorkerTest {
    @Mock private RoleImportJobRepository jobRepository;
    @Mock private RoleImportJobItemRepository itemRepository;
    @Mock private AccessRoleService accessRoleService;
    @Mock private AccessRoleCompositionService compositionService;
    @Mock private AccessRoleAssignmentRuleService ruleService;
    @Mock private IdentityRoleAssignmentService identityRoleAssignmentService;
    @Mock private RoleImportJobAuditPublisher auditPublisher;
    @Mock private AccessRoleRepository roleRepository;
    @Mock private EntitlementRepository entitlementRepository;
    @Mock private IdentityRepository identityRepository;
    @Mock private OrgUnitRepository orgUnitRepository;
    @Mock private UnifiedLoggerFactory loggerFactory;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private IUnifiedLogger logger;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private RoleImportJobWorker worker;
    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(any(Class.class), anyString())).thenReturn(logger);
        worker = new RoleImportJobWorker(jobRepository, itemRepository, accessRoleService, compositionService,
                ruleService, identityRoleAssignmentService, auditPublisher, roleRepository, entitlementRepository,
                identityRepository, orgUnitRepository, objectMapper, new ImmediateTransactionManager(), loggerFactory, 100);
    }

    @Test
    void processAssignsImportedRoleToIdentityIdsWithManualSource() {
        UUID jobId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID identityOneId = UUID.randomUUID();
        UUID identityTwoId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        RoleImportJob job = job(jobId);
        job.setActorId(actorId.toString());
        job.setActorUsername("alice.admin");
        RoleImportJobItem item = item(jobId, itemId, 1, "IMP-ROLE-001");
        RoleImportRowDto row = new RoleImportRowDto();
        row.setExternalRef("IMP-ROLE-001");
        row.setName("Imported Assigned Role");
        row.setType("business");
        row.setRiskLevel("high");
        row.setOrganization(UUID.randomUUID());
        row.setIdentityIds(List.of(identityOneId, identityTwoId));
        item.setPayload(objectMapper.valueToTree(row));
        commonJobStubs(jobId, job, itemId, item);
        when(itemRepository.findByJobIdAndPhaseAndStatusOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.QUEUED), any(Pageable.class)))
                .thenReturn(List.of(item), List.of());
        when(itemRepository.findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.SUCCEEDED), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(item), List.of(), List.of(item), List.of(), List.of(item), List.of());
        when(accessRoleService.createRole(eq("tenant_001"), any(RoleImportRowDto.class))).thenAnswer(invocation -> {
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("alice.admin");
            return roleResponse(roleId);
        });
        Identity identityOne = identity(identityOneId);
        Identity identityTwo = identity(identityTwoId);
        when(identityRepository.findByTenantIdAndIdIn(eq("tenant_001"), any()))
                .thenReturn(List.of(identityOne, identityTwo));

        assertThatCode(() -> worker.process(jobId)).doesNotThrowAnyException();

        assertThat(item.getStatus()).isEqualTo(RoleImportItemStatus.SUCCEEDED);
        assertThat(item.getRoleId()).isEqualTo(roleId);
        assertThat(job.getStatus()).isEqualTo(RoleImportJobStatus.COMPLETED);
        verify(identityRoleAssignmentService).assignRole(
                "tenant_001", identityOneId, roleId, IdentityRoleAssignment.SourceType.MANUAL, null);
        verify(identityRoleAssignmentService).assignRole(
                "tenant_001", identityTwoId, roleId, IdentityRoleAssignment.SourceType.MANUAL, null);
        ArgumentCaptor<RoleImportJobAuditEvent> auditEventCaptor = ArgumentCaptor.forClass(RoleImportJobAuditEvent.class);
        verify(auditPublisher).publish(auditEventCaptor.capture());
        RoleImportJobAuditEvent auditEvent = auditEventCaptor.getValue();
        assertThat(auditEvent.jobId()).isEqualTo(jobId.toString());
        assertThat(auditEvent.actorId()).isEqualTo(actorId.toString());
        assertThat(auditEvent.actorUsername()).isEqualTo("alice.admin");
        assertThat(auditEvent.riskLevel()).isEqualTo("high");
        assertThat(auditEvent.summary().get(RoleImportJobAuditConstants.FIELD_ACTOR_USERNAME).asText()).isEqualTo("alice.admin");
        assertThat(auditEvent.summary().get(RoleImportJobAuditConstants.FIELD_RISK_LEVEL).asText()).isEqualTo("high");
        assertThat(auditEvent.summary().get(RoleImportJobAuditConstants.FIELD_CREATED_ROLES)).hasSize(1);
        assertThat(auditEvent.summary().get(RoleImportJobAuditConstants.FIELD_SUCCEEDED_ASSIGNMENTS)).hasSize(2);
        assertThat(auditEvent.summary().get(RoleImportJobAuditConstants.FIELD_ASSIGNMENTS)
                .get(RoleImportJobAuditConstants.FIELD_ATTEMPTED).asInt()).isEqualTo(2);
    }

    @Test
    void processCreatesRoleWithWarningWhenChildRoleIdIsMissing() {
        UUID jobId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID missingChildRoleId = UUID.fromString("702d765d-a0bd-47a6-a369-2c09949668e7");
        RoleImportJob job = job(jobId);
        RoleImportJobItem item = item(jobId, itemId, 1, "IMP-PARENT-001");
        RoleImportRowDto row = new RoleImportRowDto();
        row.setExternalRef("IMP-PARENT-001");
        row.setName("Imported Parent Role");
        row.setType("business");
        row.setOrganization(UUID.randomUUID());
        row.setChildRoleIds(List.of(missingChildRoleId));
        item.setPayload(objectMapper.valueToTree(row));
        commonJobStubs(jobId, job, itemId, item);
        when(itemRepository.findByJobIdAndPhaseAndStatusOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.QUEUED), any(Pageable.class)))
                .thenReturn(List.of(item), List.of());
        when(itemRepository.findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.SUCCEEDED), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(item), List.of(), List.of(item), List.of());
        when(roleRepository.findByIdIn(any(java.util.Set.class))).thenReturn(List.of());
        when(accessRoleService.createRole(eq("tenant_001"), any(RoleImportRowDto.class))).thenAnswer(invocation -> {
            RoleImportRowDto sanitized = invocation.getArgument(1);
            assertThat(sanitized.getChildRoleIds()).isEmpty();
            return roleResponse(roleId);
        });

        assertThatCode(() -> worker.process(jobId)).doesNotThrowAnyException();
        assertThat(item.getStatus()).isEqualTo(RoleImportItemStatus.SUCCEEDED);
        assertThat(item.getRoleId()).isEqualTo(roleId);
        assertThat(item.getErrorCode()).isNull();
        assertThat(item.getWarningMessage()).contains("childRoleIds not found", missingChildRoleId.toString());
        assertThat(item.getProcessedAt()).isNotNull();
        assertThat(job.getStatus()).isEqualTo(RoleImportJobStatus.COMPLETED);
        assertThat(job.getFailedCount()).isZero();
    }

    @Test
    void processCreatesRoleAndWarnsWhenEntitlementOrIdentityIsMissing() {
        UUID jobId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID missingEntitlementId = UUID.randomUUID();
        UUID missingIdentityId = UUID.randomUUID();
        RoleImportJob job = job(jobId);
        RoleImportJobItem item = item(jobId, itemId, 1, "IMP-ROLE-VALIDATION");
        RoleImportRowDto row = new RoleImportRowDto();
        row.setExternalRef("IMP-ROLE-VALIDATION");
        row.setName("Imported Invalid References Role");
        row.setType("business");
        row.setOrganization(UUID.randomUUID());
        row.setEntitlementIds(List.of(missingEntitlementId));
        row.setIdentityIds(List.of(missingIdentityId));
        item.setPayload(objectMapper.valueToTree(row));
        commonJobStubs(jobId, job, itemId, item);
        when(entitlementRepository.findAllById(any())).thenReturn(List.of());
        when(identityRepository.findByTenantIdAndIdIn(eq("tenant_001"), any())).thenReturn(List.of());
        when(itemRepository.findByJobIdAndPhaseAndStatusOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.QUEUED), any(Pageable.class)))
                .thenReturn(List.of(item), List.of());
        when(itemRepository.findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.SUCCEEDED), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(item), List.of(), List.of(item), List.of());
        when(accessRoleService.createRole(eq("tenant_001"), any(RoleImportRowDto.class))).thenAnswer(invocation -> {
            RoleImportRowDto sanitized = invocation.getArgument(1);
            assertThat(sanitized.getEntitlementIds()).isEmpty();
            assertThat(sanitized.getIdentityIds()).isEmpty();
            return roleResponse(roleId);
        });

        assertThatCode(() -> worker.process(jobId)).doesNotThrowAnyException();

        assertThat(item.getStatus()).isEqualTo(RoleImportItemStatus.SUCCEEDED);
        assertThat(item.getRoleId()).isEqualTo(roleId);
        assertThat(item.getErrorCode()).isNull();
        assertThat(item.getWarningMessage()).contains(
                "entitlementIds not found", missingEntitlementId.toString(),
                "identityIds not found", missingIdentityId.toString());
        verify(identityRoleAssignmentService, never()).assignRole(anyString(), any(UUID.class), any(UUID.class), any(), any());
    }

    @Test
    void processAggregatesAllMissingReferenceWarningsForOneRole() {
        UUID jobId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID missingOrganizationId = UUID.randomUUID();
        UUID missingOwnerId = UUID.randomUUID();
        UUID missingEntitlementId = UUID.randomUUID();
        UUID missingIdentityId = UUID.randomUUID();
        UUID missingChildRoleId = UUID.randomUUID();
        RoleImportJob job = job(jobId);
        RoleImportJobItem item = item(jobId, itemId, 1, "IMP-ALL-WARNINGS");
        RoleImportRowDto row = new RoleImportRowDto();
        row.setExternalRef("IMP-ALL-WARNINGS");
        row.setName("Imported Role With All Warnings");
        row.setOrganization(missingOrganizationId);
        row.setOwnerId(missingOwnerId);
        row.setEntitlementIds(List.of(missingEntitlementId));
        row.setIdentityIds(List.of(missingIdentityId));
        row.setChildRoleIds(List.of(missingChildRoleId));
        row.setChildRoleRefs(List.of("MISSING-CHILD-REF"));
        item.setPayload(objectMapper.valueToTree(row));
        commonJobStubs(jobId, job, itemId, item);
        when(orgUnitRepository.findByIdAndTenantId(missingOrganizationId, "tenant_001")).thenReturn(Optional.empty());
        when(entitlementRepository.findAllById(any())).thenReturn(List.of());
        when(identityRepository.findByTenantIdAndIdIn(eq("tenant_001"), any())).thenReturn(List.of());
        when(roleRepository.findByIdIn(any(java.util.Set.class))).thenReturn(List.of());
        when(itemRepository.findByJobIdAndExternalRefOrderByRowNumberAsc(jobId, "MISSING-CHILD-REF")).thenReturn(List.of());
        when(itemRepository.findByJobIdAndPhaseAndStatusOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.QUEUED), any(Pageable.class)))
                .thenReturn(List.of(item), List.of());
        when(itemRepository.findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.SUCCEEDED), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(item), List.of(), List.of(item), List.of());
        when(accessRoleService.createRole(eq("tenant_001"), any(RoleImportRowDto.class))).thenAnswer(invocation -> {
            RoleImportRowDto sanitized = invocation.getArgument(1);
            assertThat(sanitized.getOrganization()).isNull();
            assertThat(sanitized.getOwnerId()).isNull();
            assertThat(sanitized.getEntitlementIds()).isEmpty();
            assertThat(sanitized.getIdentityIds()).isEmpty();
            assertThat(sanitized.getChildRoleIds()).isEmpty();
            assertThat(sanitized.getChildRoleRefs()).isEmpty();
            return roleResponse(roleId);
        });

        assertThatCode(() -> worker.process(jobId)).doesNotThrowAnyException();

        assertThat(item.getStatus()).isEqualTo(RoleImportItemStatus.SUCCEEDED);
        assertThat(item.getRoleId()).isEqualTo(roleId);
        assertThat(item.getWarningMessage()).contains(
                "organization not found", missingOrganizationId.toString(),
                "ownerId not found", missingOwnerId.toString(),
                "entitlementIds not found", missingEntitlementId.toString(),
                "identityIds not found", missingIdentityId.toString(),
                "childRoleIds not found", missingChildRoleId.toString(),
                "childRoleRefs not found", "MISSING-CHILD-REF");
        assertThat(job.getStatus()).isEqualTo(RoleImportJobStatus.COMPLETED);
        assertThat(job.getFailedCount()).isZero();
    }

    @Test
    void processCreatesParentWithWarningWhenChildRoleRefAlreadyFailed() {
        UUID jobId = UUID.randomUUID();
        UUID childItemId = UUID.randomUUID();
        UUID parentItemId = UUID.randomUUID();
        UUID parentRoleId = UUID.randomUUID();
        RoleImportJob job = job(jobId);
        job.setTotalCount(3);

        RoleImportJobItem failedChild = item(jobId, childItemId, 1, "OPS-CHILD-9001");
        failedChild.setStatus(RoleImportItemStatus.FAILED);
        RoleImportRowDto childRow = new RoleImportRowDto();
        childRow.setExternalRef("OPS-CHILD-9001");
        childRow.setName("Operations Read Alpha");
        childRow.setType("business");
        childRow.setOrganization(UUID.randomUUID());
        failedChild.setPayload(objectMapper.valueToTree(childRow));

        RoleImportJobItem parent = item(jobId, parentItemId, 3, "OPS-PARENT-9001");
        RoleImportRowDto parentRow = new RoleImportRowDto();
        parentRow.setExternalRef("OPS-PARENT-9001");
        parentRow.setName("Operations Supervisor Alpha");
        parentRow.setType("business");
        parentRow.setOrganization(UUID.randomUUID());
        parentRow.setChildRoleRefs(List.of("OPS-CHILD-9001"));
        parent.setPayload(objectMapper.valueToTree(parentRow));

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(RoleImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findById(parentItemId)).thenReturn(Optional.of(parent));
        when(itemRepository.save(any(RoleImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByJobId(eq(jobId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(failedChild, parent)));
        when(itemRepository.findByJobIdAndExternalRefOrderByRowNumberAsc(jobId, "OPS-CHILD-9001"))
                .thenReturn(List.of(failedChild));
        when(itemRepository.findByJobIdAndPhaseAndStatusOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.QUEUED), any(Pageable.class)))
                .thenReturn(List.of(parent), List.of());
        when(itemRepository.findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.SUCCEEDED), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(parent), List.of(), List.of(parent), List.of());
        when(itemRepository.countByJobIdAndStatus(eq(jobId), any(RoleImportItemStatus.class)))
                .thenAnswer(invocation -> List.of(failedChild, parent).stream()
                        .filter(item -> item.getStatus() == invocation.getArgument(1))
                        .count());
        when(itemRepository.countByJobIdAndRoleIdIsNotNull(jobId))
                .thenAnswer(invocation -> parent.getRoleId() != null ? 1L : 0L);
        when(accessRoleService.createRole(eq("tenant_001"), any(RoleImportRowDto.class))).thenReturn(roleResponse(parentRoleId));

        assertThatCode(() -> worker.process(jobId)).doesNotThrowAnyException();

        assertThat(parent.getStatus()).isEqualTo(RoleImportItemStatus.SUCCEEDED);
        assertThat(parent.getRoleId()).isEqualTo(parentRoleId);
        assertThat(parent.getErrorCode()).isNull();
        assertThat(parent.getWarningMessage()).contains("childRoleRefs reference failed rows", "OPS-CHILD-9001", "row 1");
        assertThat(job.getSucceededCount()).isEqualTo(1);
        assertThat(job.getFailedCount()).isEqualTo(1);
        verify(compositionService, never()).addChildRoles(anyString(), any(UUID.class), any());
    }

    @Test
    void processCreatesParentAndChildThenWiresChildRoleRef() {
        UUID jobId = UUID.randomUUID();
        UUID parentItemId = UUID.randomUUID();
        UUID childItemId = UUID.randomUUID();
        UUID parentRoleId = UUID.randomUUID();
        UUID childRoleId = UUID.randomUUID();
        UUID missingEntitlementId = UUID.randomUUID();
        RoleImportJob job = job(jobId);
        job.setTotalCount(2);

        RoleImportJobItem parent = item(jobId, parentItemId, 1, "OPS-PARENT-BEFORE-CHILD");
        RoleImportRowDto parentRow = new RoleImportRowDto();
        parentRow.setExternalRef("OPS-PARENT-BEFORE-CHILD");
        parentRow.setName("Parent Before Child");
        parentRow.setType("business");
        parentRow.setOrganization(UUID.randomUUID());
        parentRow.setChildRoleRefs(List.of("OPS-CHILD-WILL-FAIL"));
        parent.setPayload(objectMapper.valueToTree(parentRow));

        RoleImportJobItem child = item(jobId, childItemId, 2, "OPS-CHILD-WILL-FAIL");
        RoleImportRowDto childRow = new RoleImportRowDto();
        childRow.setExternalRef("OPS-CHILD-WILL-FAIL");
        childRow.setName("Child Will Fail");
        childRow.setType("business");
        childRow.setOrganization(UUID.randomUUID());
        childRow.setEntitlementIds(List.of(missingEntitlementId));
        child.setPayload(objectMapper.valueToTree(childRow));

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(RoleImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findById(parentItemId)).thenReturn(Optional.of(parent));
        when(itemRepository.findById(childItemId)).thenReturn(Optional.of(child));
        when(itemRepository.save(any(RoleImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByJobId(eq(jobId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(parent, child)));
        when(itemRepository.findByJobIdAndExternalRefOrderByRowNumberAsc(jobId, "OPS-CHILD-WILL-FAIL"))
                .thenReturn(List.of(child));
        when(itemRepository.findByJobIdAndPhaseAndStatusOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.QUEUED), any(Pageable.class)))
                .thenReturn(List.of(parent, child), List.of());
        when(itemRepository.findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.SUCCEEDED), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(parent, child), List.of(), List.of(parent, child), List.of());
        when(entitlementRepository.findAllById(any())).thenReturn(List.of());
        when(itemRepository.countByJobIdAndStatus(eq(jobId), any(RoleImportItemStatus.class)))
                .thenAnswer(invocation -> List.of(parent, child).stream()
                        .filter(item -> item.getStatus() == invocation.getArgument(1))
                        .count());
        when(itemRepository.countByJobIdAndRoleIdIsNotNull(jobId))
                .thenAnswer(invocation -> List.of(parent, child).stream().filter(item -> item.getRoleId() != null).count());
        when(accessRoleService.createRole(eq("tenant_001"), any(RoleImportRowDto.class))).thenAnswer(invocation -> {
            RoleImportRowDto row = invocation.getArgument(1);
            return roleResponse("Parent Before Child".equals(row.getName()) ? parentRoleId : childRoleId);
        });
        when(compositionService.addChildRoles("tenant_001", parentRoleId, List.of(childRoleId)))
                .thenReturn(new com.aidentx.iam.access_roles_management.model.dto.response.AddChildRolesResultDto(List.of(childRoleId), List.of(), List.of()));

        assertThatCode(() -> worker.process(jobId)).doesNotThrowAnyException();

        assertThat(child.getStatus()).isEqualTo(RoleImportItemStatus.SUCCEEDED);
        assertThat(child.getRoleId()).isEqualTo(childRoleId);
        assertThat(child.getWarningMessage()).contains("entitlementIds not found", missingEntitlementId.toString());
        assertThat(parent.getStatus()).isEqualTo(RoleImportItemStatus.SUCCEEDED);
        assertThat(parent.getRoleId()).isEqualTo(parentRoleId);
        assertThat(parent.getErrorMessage()).isNull();
        assertThat(job.getFailedCount()).isZero();
        verify(compositionService).addChildRoles("tenant_001", parentRoleId, List.of(childRoleId));
    }

    @Test
    void processRecordsUnknownChildRoleRefAsWarningAndCompletes() {
        UUID jobId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        RoleImportJob job = job(jobId);
        RoleImportJobItem item = item(jobId, itemId, 1, "IMP-PARENT-001");
        item.setStatus(RoleImportItemStatus.SUCCEEDED);
        item.setRoleId(UUID.randomUUID());
        RoleImportRowDto row = new RoleImportRowDto();
        row.setExternalRef("IMP-PARENT-001");
        row.setName("Imported Parent Role");
        row.setType("business");
        row.setOrganization(UUID.randomUUID());
        row.setChildRoleRefs(List.of("IMP-MISSING-CHILD"));
        item.setPayload(objectMapper.valueToTree(row));
        commonJobStubs(jobId, job, itemId, item);
        when(itemRepository.findByJobIdAndPhaseAndStatusOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.QUEUED), any(Pageable.class)))
                .thenReturn(List.of());
        when(itemRepository.findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
                eq(jobId), eq(RoleImportItemPhase.CREATE), eq(RoleImportItemStatus.SUCCEEDED), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(item), List.of(), List.of(item), List.of());
        assertThatCode(() -> worker.process(jobId)).doesNotThrowAnyException();
        assertThat(item.getStatus()).isEqualTo(RoleImportItemStatus.SUCCEEDED);
        assertThat(item.getErrorCode()).isNull();
        assertThat(item.getErrorMessage()).isNull();
        assertThat(item.getWarningMessage()).contains("childRoleRefs not found or not created", "IMP-MISSING-CHILD");
        assertThat(job.getStatus()).isEqualTo(RoleImportJobStatus.COMPLETED);
        assertThat(job.getSucceededCount()).isEqualTo(1);
        assertThat(job.getFailedCount()).isZero();
        ArgumentCaptor<RoleImportJobAuditEvent> auditEventCaptor = ArgumentCaptor.forClass(RoleImportJobAuditEvent.class);
        verify(auditPublisher).publish(auditEventCaptor.capture());
        RoleImportJobAuditEvent auditEvent = auditEventCaptor.getValue();
        assertThat(auditEvent.summary().get(RoleImportJobAuditConstants.FIELD_CREATED_ROLES)).hasSize(1);
        assertThat(auditEvent.summary().get(RoleImportJobAuditConstants.FIELD_FAILED_ROWS)).isEmpty();
        verify(compositionService, never()).addChildRoles(anyString(), any(UUID.class), any());
    }
    private void commonJobStubs(UUID jobId, RoleImportJob job, UUID itemId, RoleImportJobItem item) {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(RoleImportJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(RoleImportJobItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(itemRepository.findByJobId(eq(jobId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(item)));
        when(itemRepository.countByJobIdAndStatus(eq(jobId), any(RoleImportItemStatus.class)))
                .thenAnswer(invocation -> item.getStatus() == invocation.getArgument(1) ? 1L : 0L);
        when(itemRepository.countByJobIdAndRoleIdIsNotNull(jobId))
                .thenAnswer(invocation -> item.getRoleId() != null ? 1L : 0L);
    }
    private static RoleImportJob job(UUID jobId) {
        RoleImportJob job = new RoleImportJob();
        job.setId(jobId);
        job.setTenantId("tenant_001");
        job.setActorId("tester");
        job.setFormat(AccessRoleImportConstants.FORMAT_JSON);
        job.setTotalCount(1);
        job.setStatus(RoleImportJobStatus.PENDING);
        return job;
    }
    private static RoleImportJobItem item(UUID jobId, UUID itemId, int rowNumber, String externalRef) {
        RoleImportJobItem item = new RoleImportJobItem();
        item.setId(itemId);
        item.setJobId(jobId);
        item.setTenantId("tenant_001");
        item.setRowNumber(rowNumber);
        item.setExternalRef(externalRef);
        item.setPhase(RoleImportItemPhase.CREATE);
        item.setStatus(RoleImportItemStatus.QUEUED);
        return item;
    }

    private static AccessRoleResponse roleResponse(UUID roleId) {
        AccessRoleResponse response = new AccessRoleResponse();
        response.setId(roleId);
        return response;
    }

    private static Identity identity(UUID identityId) {
        Identity identity = mock(Identity.class);
        when(identity.getId()).thenReturn(identityId);
        return identity;
    }

    private static final class ImmediateTransactionManager implements PlatformTransactionManager {
        @Override public TransactionStatus getTransaction(TransactionDefinition definition) { return new SimpleTransactionStatus(); }
        @Override public void commit(TransactionStatus status) { }
        @Override public void rollback(TransactionStatus status) { }
    }
}