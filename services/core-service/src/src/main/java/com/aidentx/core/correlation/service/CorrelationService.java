package com.aidentx.core.correlation.service;

import com.aidentx.core.onboarding.model.entity.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidentx.iam.contracts.dto.policy.CorrelationRuleDTO;
import com.aidentx.core.onboarding.model.entity.ManagedAccount;
import com.aidentx.core.onboarding.model.enums.ManagedAccountStatus;
import com.aidentx.core.onboarding.repository.ApplicationRepository;
import com.aidentx.core.onboarding.repository.ManagedAccountRepository;
import com.aidentx.dal.protos.identity.FindIdentitiesByNativeQueryRequest;
import com.aidentx.dal.protos.identity.FindIdentitiesByNativeQueryResponse;
import com.aidentx.dal.protos.identity.IdentityQueryServiceGrpc;
import com.aidentx.iam.events.AggregationCompletedEvent;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class CorrelationService {

    private final IUnifiedLogger logger;
    private final ManagedAccountRepository accountRepository;
    private final ApplicationRepository applicationRepository;
    private final CorrelationQueryBuilder queryBuilder;
    private final IdentityQueryServiceGrpc.IdentityQueryServiceBlockingStub identityQueryClient;
    private final ObjectMapper objectMapper;

    public CorrelationService(
            UnifiedLoggerFactory loggerFactory,
            ManagedAccountRepository accountRepository,
            ApplicationRepository applicationRepository,
            CorrelationQueryBuilder queryBuilder,
            IdentityQueryServiceGrpc.IdentityQueryServiceBlockingStub identityQueryClient,
            ObjectMapper objectMapper) {
        this.logger = loggerFactory.getLogger(CorrelationService.class);
        this.accountRepository = accountRepository;
        this.applicationRepository = applicationRepository;
        this.queryBuilder = queryBuilder;
        this.identityQueryClient = identityQueryClient;
        this.objectMapper = objectMapper;
    }

    // TODO
    // @KafkaListener(topics = "aggregation-completed-events", groupId =
    // "core-service-correlation-group")
    // public void handleAggregationCompleted(AggregationCompletedEvent event) {
    // try {
    // UUID resourceId = event.resourceId();
    // logger.info().message("Correlation process triggered by
    // AggregationCompletedEvent.")
    // .detail("resourceId", resourceId).log();

    // Application app = applicationRepository.findById(resourceId)
    // .orElseThrow(
    // () -> new EntityNotFoundException("Application not found for correlation: " +
    // resourceId));

    // // Fetch correlation rules from GovernancePolicyService
    // List<CorrelationRuleDTO> correlationRules = null;
    // if (app.getAssociatedSchemaId() != null) {
    // try {
    // correlationRules =
    // governancePolicyService.getCorrelationRules(app.getTenantId(),
    // app.getAssociatedSchemaId());
    // } catch (Exception e) {
    // logger.warn().message("Failed to fetch correlation rules from
    // GovernancePolicyService")
    // .detail("resourceId", resourceId)
    // .detail("tenantId", app.getTenantId())
    // .detail("error", e.getMessage())
    // .log();
    // }
    // }

    // if (correlationRules == null || correlationRules.isEmpty()) {
    // logger.warn().message("No correlation rules found for resource. Skipping
    // correlation process.")
    // .detail("resourceId", resourceId).log();
    // return;
    // }

    // // Use the helper method to perform correlation
    // performCorrelation(app, correlationRules);
    // } catch (Exception e) {
    // logger.error(e).message("Critical error during correlation process.")
    // .detail("resourceId", event.resourceId()).log();
    // }
    // }

    public void correlateAccount(ManagedAccount account, Application app, List<CorrelationRuleDTO> correlationRules) {
        // Use the first correlation rule for now (assuming single rule per application)
        if (correlationRules == null || correlationRules.isEmpty()) {
            logger.warn().message("No correlation rules provided for account. Marking as ORPHANED.")
                    .detail("accountId", account.getId()).log();
            account.setStatus(ManagedAccountStatus.ORPHANED);
            accountRepository.save(account);
            return;
        }

        CorrelationRuleDTO correlationRule = correlationRules.get(0);
        CorrelationQueryBuilder.SqlQueryFragment queryFragment = queryBuilder.buildSqlQueryFragment(
                objectMapper.valueToTree(correlationRule.ruleGroup()), account.getAttributes());

        if (queryFragment.sql().isBlank()) {
            logger.warn().message("Correlation logic produced an empty query for account. Marking as ORPHANED.")
                    .detail("accountId", account.getId()).log();
            account.setStatus(ManagedAccountStatus.ORPHANED);
            accountRepository.save(account);
            return;
        }

        FindIdentitiesByNativeQueryRequest grpcRequest = FindIdentitiesByNativeQueryRequest.newBuilder()
                .setTenantId(account.getTenantId())
                .setParameterizedWhereClause(queryFragment.sql())
                .addAllParameters(queryFragment.parameters())
                .build();

        FindIdentitiesByNativeQueryResponse response = identityQueryClient.findIdentitiesByNativeQuery(grpcRequest);

        if (response.getIdentitiesCount() == 1) {
            UUID identityId = UUID.fromString(response.getIdentities(0).getId());
            account.setIdentityId(identityId);
            account.setStatus(ManagedAccountStatus.ACTIVE);
            accountRepository.save(account);
            logger.info().message("Correlation successful for account.")
                    .detail("accountId", account.getId()).detail("identityId", identityId).log();
        } else if (response.getIdentitiesCount() > 1) {
            account.setStatus(ManagedAccountStatus.MULTI_MATCHED);
            accountRepository.save(account);
            logger.warn().message("Ambiguous correlation: multiple identities found for account.")
                    .detail("accountId", account.getId()).log();
        } else {
            account.setStatus(ManagedAccountStatus.ORPHANED);
            accountRepository.save(account);
            logger.warn().message("Account could not be correlated. Marked as ORPHANED.")
                    .detail("accountId", account.getId()).log();
        }
    }

    private void performCorrelation(Application app, List<CorrelationRuleDTO> correlationRules) {
        try (Stream<ManagedAccount> accountStream = accountRepository.findAllByTenantIdAndResourceIdAndStatus(
                app.getTenantId(), app.getId(), ManagedAccountStatus.UNMATCHED)) {

            accountStream.forEach(account -> {
                try {
                    correlateAccount(account, app, correlationRules);
                } catch (Exception e) {
                    logger.error(e).message("Failed to correlate individual account.")
                            .detail("accountId", account.getId())
                            .detail("resourceId", app.getId()).log();
                }
            });
        } catch (Exception e) {
            logger.error(e).message("Error during correlation process.")
                    .detail("resourceId", app.getId()).log();
        }
    }
}