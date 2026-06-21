package com.aidentx.core.onboarding.service;

import com.aidentx.core.onboarding.model.entity.Application;
import com.aidentx.core.onboarding.repository.ApplicationRepository;
import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import com.aidentx.iam.contracts.grpc.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PolicyOrchestrationService {

    private final IUnifiedLogger logger;
    private final GovernancePolicyServiceGrpc.GovernancePolicyServiceBlockingStub governancePolicyClient;
    private final ApplicationRepository applicationRepository;
    private final ObjectMapper objectMapper;

    public PolicyOrchestrationService(
            UnifiedLoggerFactory loggerFactory,
            GovernancePolicyServiceGrpc.GovernancePolicyServiceBlockingStub governancePolicyClient,
            ApplicationRepository applicationRepository,
            ObjectMapper objectMapper) {
        this.logger = loggerFactory.getLogger(PolicyOrchestrationService.class);
        this.governancePolicyClient = governancePolicyClient;
        this.applicationRepository = applicationRepository;
        this.objectMapper = objectMapper;
    }

    public void forwardPoliciesToIdentityService(String tenantId, Application app, OnboardingRequestDTO request) {
        // Build a complete policy with all components
        UpsertPolicyRequest policyRequest = buildCompletePolicyRequest(
                tenantId, app.getId(), app.getIdentityType(), request, app.getAssociatedSchemaId());

        logger.info().message("Sending complete policy to identity-service via gRPC.")
                .detail("appId", app.getId())
                .detail("identityType", app.getIdentityType())
                .detail("hasCorrelation", request.correlation() != null)
                .detail("hasAttributeMappings", request.attributeMappings() != null).log();

        UpsertPolicyResponse response = governancePolicyClient.upsertPolicy(policyRequest);
        if (!response.getSuccess()) {
            throw new IllegalArgumentException(
                    "Policy was rejected by identity-service: " + response.getMessage());
        }
    }

    public IdentityMappingPolicy getPolicy(String tenantId, UUID resourceId, String identityType) {
        try {
            logger.info().message("Fetching policies from identity-service via gRPC.")
                    .detail("tenantId", tenantId)
                    .detail("resourceId", resourceId)
                    .detail("identityType", identityType).log();

            GetPolicyRequest request = GetPolicyRequest.newBuilder()
                    .setTenantId(tenantId)
                    .setIdentityType(identityType)
                    .setResourceId(resourceId.toString())
                    .build();

            return governancePolicyClient.getPolicy(request);
        } catch (Exception e) {
            logger.warn().message("Failed to fetch policies from identity-service for application.")
                    .detail("appId", resourceId)
                    .detail("tenantId", tenantId)
                    .detail("error", e.getMessage()).log();
            return null;
        }
    }

    public void deletePolicies(String tenantId, UUID resourceId) {
        logger.info().message("Deleting policies from Identity Service")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId).log();

        DeletePoliciesByResourceRequest deletePolicyRequest = DeletePoliciesByResourceRequest.newBuilder()
                .setTenantId(tenantId)
                .setResourceId(resourceId.toString())
                .build();

        DeletePoliciesByResourceResponse deletePolicyResponse = governancePolicyClient
                .deletePoliciesByResource(deletePolicyRequest);

        if (!deletePolicyResponse.getSuccess()) {
            throw new RuntimeException("Failed to delete policies from Identity Service: "
                    + deletePolicyResponse.getMessage());
        }

        logger.info().message("Successfully deleted policies from Identity Service")
                .detail("deletedCount", deletePolicyResponse.getDeletedCount())
                .detail("resourceId", resourceId).log();
    }

    private UpsertPolicyRequest buildCompletePolicyRequest(String tenantId, UUID appId,
            String identityType, OnboardingRequestDTO request, UUID schemaId) {
        UpsertPolicyRequest.Builder requestBuilder = UpsertPolicyRequest.newBuilder()
                .setTenantId(tenantId)
                .setIdentityType(identityType)
                .setResourceId(appId.toString());
        
        if (schemaId != null) {
            requestBuilder.setSchemaId(schemaId.toString());
        }

        IdentityMappingPolicy.Builder policyBuilder = IdentityMappingPolicy.newBuilder();

        // Convert Correlation Rules
        if (request.correlation() != null) {
            policyBuilder.addCorrelationRules(convertCorrelationRule(appId, request.correlation()));
        }

        // Convert Attribute Mappings
        if (request.attributeMappings() != null && request.attributeMappings().attributeMappings() != null) {
            for (OnboardingRequestDTO.OnboardingAttributeMappingDTO mapping : request.attributeMappings()
                    .attributeMappings()) {
                policyBuilder.addAttributeMappings(convertAttributeMapping(appId, mapping));
            }
        }

        // Convert Synchronization Reactions
        if (request.synchronizationPolicy() != null && request.synchronizationPolicy().reactions() != null) {
            for (OnboardingRequestDTO.SynchronizationReactionDTO reaction : request.synchronizationPolicy().reactions()) {
                policyBuilder.addSynchronizationReactions(
                        SynchronizationReaction.newBuilder()
                                .setSituation(reaction.situation())
                                .setAction(reaction.action())
                                .build()
                );
            }
            logger.info().message("Added synchronization reactions to policy")
                    .detail("reactionCount", request.synchronizationPolicy().reactions().size())
                    .log();
        } else {
            // Apply sensible defaults based on authoritative flag
            boolean isAuthoritative = request.details() != null && Boolean.TRUE.equals(request.details().isAuthoritative());
            logger.info().message("No sync reactions provided, applying defaults")
                    .detail("isAuthoritative", isAuthoritative)
                    .log();
            
            // UNMATCHED: HR creates identities, others don't
            policyBuilder.addSynchronizationReactions(
                    SynchronizationReaction.newBuilder()
                            .setSituation("UNMATCHED")
                            .setAction(isAuthoritative ? "add-focus" : "none")
                            .build()
            );
            // UNLINKED: always link
            policyBuilder.addSynchronizationReactions(
                    SynchronizationReaction.newBuilder()
                            .setSituation("UNLINKED")
                            .setAction("link")
                            .build()
            );
            // LINKED: always sync
            policyBuilder.addSynchronizationReactions(
                    SynchronizationReaction.newBuilder()
                            .setSituation("LINKED")
                            .setAction("sync")
                            .build()
            );
            // DELETED: HR disables, others just unlink
            policyBuilder.addSynchronizationReactions(
                    SynchronizationReaction.newBuilder()
                            .setSituation("DELETED")
                            .setAction(isAuthoritative ? "inactivate-focus" : "unlink")
                            .build()
            );
            // DISPUTED: never auto-resolve
            policyBuilder.addSynchronizationReactions(
                    SynchronizationReaction.newBuilder()
                            .setSituation("DISPUTED")
                            .setAction("none")
                            .build()
            );
        }

        requestBuilder.setPolicy(policyBuilder.build());
        return requestBuilder.build();
    }

    private AttributeMapping convertAttributeMapping(UUID appId, OnboardingRequestDTO.OnboardingAttributeMappingDTO dto) {
        AttributeMapping.Builder builder = AttributeMapping.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setSourceResourceId(appId.toString())
                .setTargetAttributeName(dto.targetAttributeName());

        if (dto.sourceAttributeName() != null) {
            builder.setSourceAttributeName(dto.sourceAttributeName());
        }
        
        if (dto.mode() != null) {
            builder.setMode(dto.mode());
        } else {
            builder.setMode("direct"); // Default
        }

        if ("script".equalsIgnoreCase(dto.mode()) && dto.script() != null) {
             Struct.Builder structBuilder = Struct.newBuilder();
             if (dto.script().data() != null) {
                 structBuilder.putFields("data", Value.newBuilder().setStringValue(dto.script().data()).build());
             }
             if (dto.script().lang() != null) {
                 structBuilder.putFields("lang", Value.newBuilder().setStringValue(dto.script().lang()).build());
             }
             builder.setScript(structBuilder.build());
        }

        return builder.build();
    }

    private CorrelationRule convertCorrelationRule(UUID appId, OnboardingRequestDTO.CorrelationDTO dto) {
        try {
            CorrelationRule.Builder ruleBuilder = CorrelationRule.newBuilder()
                    .setId(UUID.randomUUID().toString()) // Server generates a new ID
                    .setResourceId(appId.toString())
                    .setMode(dto.mode());

            if ("script".equalsIgnoreCase(dto.mode())) {
                if (dto.script() != null) {
                    Struct.Builder structBuilder = Struct.newBuilder();
                    if (dto.script().data() != null) {
                        structBuilder.putFields("data",
                                Value.newBuilder().setStringValue(dto.script().data()).build());
                    }
                    if (dto.script().lang() != null) {
                        structBuilder.putFields("lang",
                                Value.newBuilder().setStringValue(dto.script().lang()).build());
                    }
                    ruleBuilder.setRuleGroup(structBuilder.build());
                }
            } else if (dto.ruleGroup() != null) {
                Map<String, Object> ruleGroupMap = objectMapper.convertValue(
                        dto.ruleGroup(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });

                Struct.Builder structBuilder = Struct.newBuilder();
                for (Map.Entry<String, Object> entry : ruleGroupMap.entrySet()) {
                    structBuilder.putFields(entry.getKey(), objectToProtobufValue(entry.getValue()));
                }
                ruleBuilder.setRuleGroup(structBuilder.build());
            }

            return ruleBuilder.build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert correlation rule", e);
        }
    }

    private Value objectToProtobufValue(Object value) {
        if (value == null) {
            return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
        }
        if (value instanceof String) {
            return Value.newBuilder().setStringValue((String) value).build();
        }
        if (value instanceof Number) {
            return Value.newBuilder().setNumberValue(((Number) value).doubleValue()).build();
        }
        if (value instanceof Boolean) {
            return Value.newBuilder().setBoolValue((Boolean) value).build();
        }
        if (value instanceof List) {
            ListValue.Builder listBuilder = ListValue.newBuilder();
            for (Object item : (List<?>) value) {
                listBuilder.addValues(objectToProtobufValue(item));
            }
            return Value.newBuilder().setListValue(listBuilder.build()).build();
        }
        if (value instanceof Map) {
            Struct.Builder structBuilder = Struct.newBuilder();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                structBuilder.putFields(String.valueOf(entry.getKey()), objectToProtobufValue(entry.getValue()));
            }
            return Value.newBuilder().setStructValue(structBuilder.build()).build();
        }
        // Fallback for unknown types
        return Value.newBuilder().setStringValue(value.toString()).build();
    }
}
