package com.aidentx.iam.identity_management.service.impl;

import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.events.IdentityEventPublisher;
import com.aidentx.iam.identity_management.model.dto.IdentityResponseDto;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.EmployeeRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrArrayValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrBooleanValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrDateValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrNumberValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrObjectValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrStringValueRepository;
import com.aidentx.iam.identity_management.repository.ExtendedAttributeDefinitionRepository;
import com.aidentx.iam.identity_management.repository.HumanIdentityRepository;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.identity_management.repository.IdentityShardRepository;
import com.aidentx.iam.identity_management.repository.ShardRepository;
import com.aidentx.iam.identity_management.service.ExtendedAttributeService;
import com.aidentx.iam.identity_management.service.IdentitySchemaService;
import com.aidentx.iam.identity_management.service.strategy.EmployeeCreationStrategy;
import com.aidentx.iam.identity_management.service.strategy.NonEmployeeCreationStrategy;
import com.aidentx.iam.identity_management.service.strategy.NonHumanCreationStrategy;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.shared.dgraph.DgraphService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityServiceImplResponseEnrichmentTest {

    private static final String TENANT_ID = "tenant-1";

    @Mock private IdentityRepository identityRepository;
    @Mock private HumanIdentityRepository humanIdentityRepository;
    @Mock private IdentityShardRepository identityShardRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ExtendedAttributeService extendedAttributeService;
    @Mock private IdentitySchemaService identitySchemaService;
    @Mock private ExtendedAttributeDefinitionRepository extendedAttributeDefinitionRepository;
    @Mock private ExtAttrStringValueRepository extAttrStringValueRepository;
    @Mock private ExtAttrBooleanValueRepository extAttrBooleanValueRepository;
    @Mock private ExtAttrNumberValueRepository extAttrNumberValueRepository;
    @Mock private ExtAttrDateValueRepository extAttrDateValueRepository;
    @Mock private ExtAttrObjectValueRepository extAttrObjectValueRepository;
    @Mock private ExtAttrArrayValueRepository extAttrArrayValueRepository;
    @Mock private EmployeeCreationStrategy employeeCreationStrategy;
    @Mock private NonEmployeeCreationStrategy nonEmployeeCreationStrategy;
    @Mock private NonHumanCreationStrategy nonHumanCreationStrategy;
    @Mock private ShardRepository shardRepository;
    @Mock private IdentityEventPublisher identityEventPublisher;
    @Mock private AccessRoleRepository accessRoleRepository;
    @Mock private IdentityRoleAssignmentRepository identityRoleAssignmentRepository;
    @Mock private EntitlementAssignmentRepository entitlementAssignmentRepository;
    @Mock private DgraphService dgraphService;
    @Mock private UnifiedLoggerFactory loggerFactory;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private IUnifiedLogger logger;

    private IdentityServiceImpl service;

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(IdentityServiceImpl.class)).thenReturn(logger);
        service = new IdentityServiceImpl(
                loggerFactory,
                identityRepository,
                humanIdentityRepository,
                identityShardRepository,
                employeeRepository,
                extendedAttributeService,
                identitySchemaService,
                extendedAttributeDefinitionRepository,
                extAttrStringValueRepository,
                extAttrBooleanValueRepository,
                extAttrNumberValueRepository,
                extAttrDateValueRepository,
                extAttrObjectValueRepository,
                extAttrArrayValueRepository,
                employeeCreationStrategy,
                nonEmployeeCreationStrategy,
                nonHumanCreationStrategy,
                new ObjectMapper(),
                shardRepository,
                identityEventPublisher,
                accessRoleRepository,
                identityRoleAssignmentRepository,
                entitlementAssignmentRepository,
                dgraphService);
    }

    @Test
    void findByTenantIdAndIdIncludesRiskLevelAndTotalAccessFromAllEntitlementAssignmentRows() {
        UUID identityId = UUID.randomUUID();
        Employee employee = employee(identityId, "medium");

        when(identityRepository.findByTenantIdAndId(TENANT_ID, identityId)).thenReturn(Optional.of(employee));
        when(extendedAttributeService.findExtendedAttributesForIdentity(identityId, TENANT_ID, IdentityType.EMPLOYEE))
                .thenReturn(Map.of());
        when(entitlementAssignmentRepository.countAssignmentRowsByTenantIdAndIdentityIdIn(eq(TENANT_ID), any()))
                .thenReturn(List.<Object[]>of(new Object[]{identityId, 5L}));

        IdentityResponseDto result = service.findByTenantIdAndId(TENANT_ID, identityId).orElseThrow();

        assertThat(result.riskLevel()).isEqualTo("medium");
        assertThat(result.totalAccess()).isEqualTo(5L);
        assertThat(result.coreAttributes()).containsEntry("riskLevel", "medium");
        assertThat(result.coreAttributes()).containsEntry("totalAccess", 5L);
        verifyNoInteractions(dgraphService, identityRoleAssignmentRepository);
    }

    private Employee employee(UUID identityId, String riskLevel) {
        Employee employee = new Employee();
        employee.setId(identityId);
        employee.setTenantId(TENANT_ID);
        employee.setIdentityType(IdentityType.EMPLOYEE);
        employee.setEmployeeId("emp-" + identityId);
        employee.setUsername("user-" + identityId);
        employee.setEmail("user-" + identityId + "@example.com");
        employee.setFirstName("Risky");
        employee.setLastName("User");
        employee.setDisplayName("Risky User");
        employee.setRiskLevel(riskLevel);
        return employee;
    }
}
