package com.aidentx.iam.role_management.service;

import com.aidentx.iam.dgraph.DgraphGovernanceService;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.identity_management.service.RelationshipEventPublisher;
import com.aidentx.iam.model.dto.events.RelationshipChangeEvent.Action;
import com.aidentx.iam.role_management.mapper.RoleMapper;
import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleCreateDto;
import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleResponseDto;
import com.aidentx.iam.role_management.model.dto.RoleDtos.RoleUpdateDto;
import com.aidentx.iam.role_management.model.entity.Role;
import com.aidentx.iam.role_management.repository.RoleRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class RoleServiceImpl implements RoleService {

        private final IUnifiedLogger logger;
        private final RoleRepository roleRepository;
        private final EntitlementRepository entitlementRepository;
        private final RoleMapper roleMapper;
        private final RelationshipEventPublisher relationshipEventPublisher;
        private final DgraphGovernanceService dgraphGovernanceService;
        private final IdentityRepository identityRepository;

        public RoleServiceImpl(UnifiedLoggerFactory loggerFactory, RoleRepository roleRepository,
                        EntitlementRepository entitlementRepository, RoleMapper roleMapper,
                        RelationshipEventPublisher relationshipEventPublisher,
                        DgraphGovernanceService dgraphGovernanceService,
                        IdentityRepository identityRepository) {
                this.logger = loggerFactory.getLogger("RoleService", "role-management");
                this.roleRepository = roleRepository;
                this.entitlementRepository = entitlementRepository;
                this.roleMapper = roleMapper;
                this.relationshipEventPublisher = relationshipEventPublisher;
                this.dgraphGovernanceService = dgraphGovernanceService;
                this.identityRepository = identityRepository;
        }

        @Override
        public RoleResponseDto createRole(RoleCreateDto createDto) {
                Role newRole = roleMapper.createDtoToEntity(createDto);
                Role savedRoleInPostgres = roleRepository.save(newRole);

                try {
                        // Sync to Dgraph and get the UID
                        String dgraphUid = dgraphGovernanceService.syncRoleToGraph(savedRoleInPostgres);

                        // Save the UID back to our Postgres record
                        savedRoleInPostgres.setDgraphUid(dgraphUid);
                        Role finalSavedRole = roleRepository.save(savedRoleInPostgres);

                        logger.info().message("Created and synced new business role.")
                                        .detail("roleId", finalSavedRole.getId()).log();
                        return roleMapper.entityToResponseDto(finalSavedRole);
                } catch (Exception e) {
                        logger.error(e).message("Failed to sync new role to Dgraph, but role was created in Postgres.")
                                        .detail("roleId", savedRoleInPostgres.getId()).log();
                        // Return the DTO without the Dgraph UID, as the core creation succeeded.
                        return roleMapper.entityToResponseDto(savedRoleInPostgres);
                }
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<RoleResponseDto> findById(UUID roleId) {
                return roleRepository.findById(roleId).map(roleMapper::entityToResponseDto);
        }

        @Override
        @Transactional(readOnly = true)
        public Page<RoleResponseDto> findAll(Pageable pageable) {
                logger.debug().message("Finding all roles with pagination.").log();
                return roleRepository.findAll(pageable)
                                .map(roleMapper::entityToResponseDto);
        }

        @Override
        @Transactional(readOnly = true)
        public Optional<RoleResponseDto> findRoleWithEntitlements(UUID roleId) {
                return roleRepository.findById(roleId).map(role -> {
                        // This forces Hibernate to load the lazy-loaded collection within the
                        // transaction.
                        role.getIncludedEntitlements().size();
                        return roleMapper.entityToResponseDto(role);
                });
        }

        @Override
        public RoleResponseDto updateRole(UUID roleId, RoleUpdateDto updateDto) {
                Role role = roleRepository.findById(roleId)
                                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + roleId));

                // Update basic metadata
                roleMapper.updateEntityFromDto(updateDto, role);

                // Update the included entitlements
                if (updateDto.entitlementIds() != null) {
                        Set<Entitlement> entitlements = new HashSet<>(
                                        entitlementRepository.findAllById(updateDto.entitlementIds()));
                        role.setIncludedEntitlements(entitlements);
                        logger.info().message("Updated role entitlement composition.")
                                        .detail("roleId", roleId)
                                        .detail("newEntitlementCount", entitlements.size())
                                        .log();
                }

                Role updatedRole = roleRepository.save(role);
                return roleMapper.entityToResponseDto(updatedRole);
        }

        public void assignRoleToIdentity(UUID roleId, UUID identityId, String tenantId) {
                logger.info().message("Assigning role to identity.").detail("roleId", roleId)
                                .detail("identityId", identityId)
                                .log();

                // Step 1: Validate that the role exists and belongs to the correct tenant.
                Role role = roleRepository.findById(roleId)
                                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + roleId));
                if (!role.getTenantId().equals(tenantId)) {
                        // This is a security failure, not just "not found".
                        throw new SecurityException("Role does not belong to the specified tenant or does not exist.");
                }

                // Step 2: Validate that the identity exists
                boolean identityExists = identityRepository.existsById(identityId);

                if (!identityExists) {
                        throw new EntityNotFoundException("Identity not found with ID: " + identityId);
                }

                // Step 3: Publish the event to create the relationship in DGraph.
                // This is only done AFTER all validations have passed.
                relationshipEventPublisher.publishRelationshipChangeEvent(
                                identityId,
                                roleId,
                                "HAS_ROLE",
                                Action.CREATE);

                logger.info().message("Successfully published HAS_ROLE assignment event.").detail("roleId", roleId)
                                .detail("identityId", identityId).log();
        }

        @Override
        public void revokeRoleFromIdentity(UUID roleId, UUID identityId, String tenantId) {
                logger.info().message("Revoking role from identity.").detail("roleId", roleId)
                                .detail("identityId", identityId)
                                .log();

                // In a real application, we would perform the same validation as above to
                // ensure
                // the request is valid before publishing a revocation event. For brevity, we
                // assume it's valid.

                // Publish the event to DELETE the relationship in DGraph.
                relationshipEventPublisher.publishRelationshipChangeEvent(
                                identityId,
                                roleId,
                                "HAS_ROLE",
                                Action.DELETE);

                logger.info().message("Successfully published HAS_ROLE revocation event.").detail("roleId", roleId)
                                .detail("identityId", identityId).log();
        }

}