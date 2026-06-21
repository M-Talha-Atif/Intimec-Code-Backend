package com.aidentx.iam.shadow_management.service.impl;

import com.aidentx.iam.shadow_management.exception.ShadowAlreadyLinkedException;
import com.aidentx.iam.shadow_management.exception.ShadowNotFoundException;
import com.aidentx.iam.shadow_management.exception.ShadowNotLinkedException;
import com.aidentx.iam.shadow_management.mapper.ShadowMapper;
import com.aidentx.iam.shadow_management.model.dto.*;
import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.iam.shadow_management.repository.ShadowSpecification;
import com.aidentx.iam.shadow_management.service.ShadowService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of ShadowService for managing Shadow objects.
 */
@Service
@Transactional
public class ShadowServiceImpl implements ShadowService {

    private final ShadowRepository shadowRepository;
    private final ShadowMapper shadowMapper;
    private final IUnifiedLogger logger;

    public ShadowServiceImpl(ShadowRepository shadowRepository,
            ShadowMapper shadowMapper,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.shadowRepository = shadowRepository;
        this.shadowMapper = shadowMapper;
        this.logger = unifiedLoggerFactory.getLogger(ShadowServiceImpl.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShadowResponse> listShadows(String resourceUid, String objectClass, String situation,
            Boolean linked, Integer page, Integer size, String sort, String tenantId) {
        logger.debug().message("Listing shadows for tenant")
                .detail("tenantId", tenantId)
                .detail("resourceUid", resourceUid)
                .detail("objectClass", objectClass)
                .detail("situation", situation)
                .detail("linked", linked)
                .log();

        Pageable pageable = createPageable(page, size, sort);
        Shadow.SynchronizationSituation syncSituation = null;
        if (situation != null) {
            try {
                syncSituation = Shadow.SynchronizationSituation.valueOf(situation);
            } catch (IllegalArgumentException e) {
                // Invalid situation, ignore filter
            }
        }
        
        Specification<Shadow> spec = ShadowSpecification.filterShadows(
                tenantId, resourceUid, objectClass, syncSituation, linked, null, null);

        Page<Shadow> shadows = shadowRepository.findAll(spec, pageable);
        return shadows.map(shadowMapper::toShadowResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ShadowResponse getShadow(UUID shadowId, String tenantId) {
        logger.debug().message("Getting shadow by ID")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        Shadow shadow = shadowRepository.findById(shadowId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new ShadowNotFoundException(shadowId));

        return shadowMapper.toShadowResponse(shadow);
    }

    @Override
    @Transactional
    public ShadowResponse createShadow(ShadowCreateRequest createRequest, String tenantId) {
        logger.debug().message("Creating shadow")
                .detail("tenantId", tenantId)
                .detail("resourceId", createRequest.getResourceId())
                .detail("resourceUid", createRequest.getResourceUid())
                .log();

        // Check if shadow already exists by resourceId + resourceUid + tenant
        if (shadowRepository.existsByResourceIdAndResourceUidAndTenantId(
                createRequest.getResourceId(), 
                createRequest.getResourceUid(), 
                tenantId)) {
            throw new com.aidentx.iam.shadow_management.exception.DuplicateShadowException(
                    createRequest.getResourceId(), 
                    createRequest.getResourceUid());
        }

        Shadow shadow = new Shadow();
        shadow.setTenantId(tenantId);
        shadow.setResourceId(createRequest.getResourceId());
        shadow.setResourceUid(createRequest.getResourceUid());
        shadow.setObjectClass(createRequest.getObjectClass());
        shadow.setKind(createRequest.getKind());

        // Set synchronization situation - always UNLINKED for new shadows
        shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.UNLINKED);

        // Set attributes
        if (createRequest.getAttributes() != null && !createRequest.getAttributes().isEmpty()) {
            shadow.setAttributes(createRequest.getAttributes());
        }

        Shadow savedShadow = shadowRepository.save(shadow);
        logger.info().message("Created shadow")
                .detail("shadowId", savedShadow.getId())
                .detail("tenantId", tenantId)
                .log();

        return shadowMapper.toShadowResponse(savedShadow);
    }

    @Override
    @Transactional
    public ShadowResponse updateShadow(UUID shadowId, ShadowUpdateRequest updateRequest, String tenantId) {
        logger.debug().message("Updating shadow")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        Shadow shadow = shadowRepository.findById(shadowId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new ShadowNotFoundException(shadowId));

        // Update fields
        if (updateRequest.getKind() != null) {
            shadow.setKind(updateRequest.getKind());
        }

        if (updateRequest.getSynchronizationSituation() != null) {
            try {
                shadow.setSynchronizationSituation(
                        Shadow.SynchronizationSituation.valueOf(updateRequest.getSynchronizationSituation()));
            } catch (IllegalArgumentException e) {
                // Keep existing situation if invalid
            }
        }

        // Update attributes
        if (updateRequest.getAttributes() != null) {
            shadow.setAttributes(updateRequest.getAttributes());
        }

        Shadow savedShadow = shadowRepository.save(shadow);
        logger.info().message("Updated shadow")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        return shadowMapper.toShadowResponse(savedShadow);
    }

    @Override
    @Transactional
    public boolean deleteShadow(UUID shadowId, String tenantId) {
        logger.debug().message("Deleting shadow")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        Shadow shadow = shadowRepository.findById(shadowId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElse(null);

        if (shadow == null) {
            return false;
        }

        shadowRepository.delete(shadow);
        logger.info().message("Deleted shadow")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();
        return true;
    }

    @Override
    @Transactional
    public ShadowResponse linkShadowToIdentity(UUID shadowId, UUID identityId, String tenantId) {
        logger.debug().message("Linking shadow to identity")
                .detail("shadowId", shadowId)
                .detail("identityId", identityId)
                .detail("tenantId", tenantId)
                .log();

        Shadow shadow = shadowRepository.findById(shadowId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new ShadowNotFoundException(shadowId));

        if (shadow.getLinkedIdentityId() != null) {
            throw new ShadowAlreadyLinkedException(shadowId, shadow.getLinkedIdentityId());
        }

        shadow.setLinkedIdentityId(identityId);
        shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.LINKED);

        Shadow savedShadow = shadowRepository.save(shadow);
        logger.info().message("Linked shadow to identity")
                .detail("shadowId", shadowId)
                .detail("identityId", identityId)
                .detail("tenantId", tenantId)
                .log();

        return shadowMapper.toShadowResponse(savedShadow);
    }

    @Override
    @Transactional
    public ShadowResponse unlinkShadowFromIdentity(UUID shadowId, String tenantId) {
        logger.debug().message("Unlinking shadow from identity")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        Shadow shadow = shadowRepository.findById(shadowId)
                .filter(s -> tenantId.equals(s.getTenantId()))
                .orElseThrow(() -> new ShadowNotFoundException(shadowId));

        if (shadow.getLinkedIdentityId() == null) {
            throw new ShadowNotLinkedException(shadowId);
        }

        shadow.setLinkedIdentityId(null);
        shadow.setSynchronizationSituation(Shadow.SynchronizationSituation.UNLINKED);

        Shadow savedShadow = shadowRepository.save(shadow);
        logger.info().message("Unlinked shadow from identity")
                .detail("shadowId", shadowId)
                .detail("tenantId", tenantId)
                .log();

        return shadowMapper.toShadowResponse(savedShadow);
    }

    @Override
    @Transactional(readOnly = true)
    public GetShadowsByIdentityResponse getShadowsByIdentity(UUID identityId, String resourceUid,
            String objectClass, String tenantId) {
        logger.debug().message("Getting shadows for identity")
                .detail("identityId", identityId)
                .detail("resourceUid", resourceUid)
                .detail("objectClass", objectClass)
                .detail("tenantId", tenantId)
                .log();

        List<Shadow> shadows = shadowRepository.findByLinkedIdentityIdWithFilters(
                identityId, tenantId, resourceUid, objectClass);

        List<ShadowResponse> shadowResponses = shadows.stream()
                .map(shadowMapper::toShadowResponse)
                .collect(Collectors.toList());

        return new GetShadowsByIdentityResponse(identityId, shadowResponses);
    }

    /**
     * Creates a Pageable object from page, size, and sort parameters.
     * Note: Sorting by nested JSONB attributes (e.g., "attributes.email") is not 
     * directly supported by JPA. Only top-level Shadow entity fields can be sorted.
     * The attributes field is stored as JSONB for efficient storage and retrieval.
     */
    private Pageable createPageable(Integer page, Integer size, String sort) {
        Sort sortObj = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            String[] sortParts = sort.split(",");
            if (sortParts.length == 2) {
                String sortField = sortParts[0];
                
                // Warn if trying to sort by JSONB attributes
                if (sortField.startsWith("attributes.")) {
                    logger.warn().message("Sorting by JSONB attributes not supported via JPA")
                            .detail("requestedSort", sortField)
                            .detail("fallback", "createdAt,desc")
                            .log();
                    // Fall back to default sort by creation time
                    sortObj = Sort.by(Sort.Direction.DESC, "createdAt");
                } else {
                    sortObj = Sort.by(Sort.Direction.fromString(sortParts[1]), sortField);
                }
            }
        }

        return PageRequest.of(page != null ? page : 0, size != null ? size : 50, sortObj);
    }
}