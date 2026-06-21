package com.aidentx.iam.identity_management.service;

import java.util.List;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidentx.dal.TransformationService;
import com.aidentx.iam.events.ReconciliationRequiredEvent;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.IdentityShard;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.identity_management.repository.IdentityShardRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

/**
 * Service responsible for reconciling identity data from multiple sources based
 * on governance policies.
 */
@Service
public class IdentityReconciliationService {

    private final IUnifiedLogger logger;
    private final IdentityRepository identityRepository;
    private final IdentityShardRepository identityShardRepository;
    private final DgraphSyncService dgraphSyncService;
    private final TransformationService transformationService;
    private final IdentityService identityService;

    public IdentityReconciliationService(
            UnifiedLoggerFactory loggerFactory,
            IdentityRepository identityRepository,
            IdentityShardRepository identityShardRepository,
            DgraphSyncService dgraphSyncService,
            TransformationService transformationService,
            IdentityService identityService) {
        this.logger = loggerFactory.getLogger(IdentityReconciliationService.class);
        this.identityRepository = identityRepository;
        this.identityShardRepository = identityShardRepository;
        this.dgraphSyncService = dgraphSyncService;
        this.transformationService = transformationService;
        this.identityService = identityService;
    }

    @KafkaListener(topics = "identity-reconciliation-events", groupId = "identity-service-reconciliation-group")
    @Transactional
    public void handleReconciliationRequest(ReconciliationRequiredEvent event) {
        UUID identityId = event.identityId();
        String tenantId = event.tenantId();
        logger.info().message("Reconciliation process triggered for identity.").detail("identityId", identityId)
                .detail("tenantId", tenantId).log();

        try {
            Identity identity = identityRepository.findById(identityId)
                    .orElseThrow(() -> new IllegalArgumentException("Identity not found for reconciliation."));

            // shards are now defined as the 'latest' pushed data per source
            List<IdentityShard> shards = identityShardRepository.findByIdentityId(identityId);

            // this can't happen, but just in case
            if (shards.isEmpty()) {
                logger.warn().message("No identity shards found for reconciliation.").detail("identityId", identityId)
                        .log();
                return;
            }
            // should fetch the policy(rules) for transformations and priorities, and
            // construct the golden record based on it, which should have the state 'ACTIVE'

        } catch (Exception e) {
            logger.error().message("Error during identity reconciliation process.")
                    .detail("identityId", identityId)
                    .detail("tenantId", tenantId)
                    .log();
        }
    }

}