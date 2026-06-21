package com.aidentx.iam.grpc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.CorrelationRuleDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityMappingPolicyDTO;
import com.aidentx.iam.contracts.dto.policy.SynchronizationReactionDTO;
import com.aidentx.iam.contracts.grpc.AttributeMapping;
import com.aidentx.iam.contracts.grpc.CorrelationRule;
import com.aidentx.iam.contracts.grpc.DeletePoliciesByResourceRequest;
import com.aidentx.iam.contracts.grpc.DeletePoliciesByResourceResponse;
import com.aidentx.iam.contracts.grpc.GetPolicyRequest;
import com.aidentx.iam.contracts.grpc.GovernancePolicyServiceGrpc;
import com.aidentx.iam.contracts.grpc.IdentityMappingPolicy;
import com.aidentx.iam.contracts.grpc.SynchronizationReaction;
import com.aidentx.iam.contracts.grpc.UpsertPolicyRequest;
import com.aidentx.iam.contracts.grpc.UpsertPolicyResponse;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.policy.service.ResourceGovernancePolicyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class GovernancePolicyGrpcServiceImpl extends GovernancePolicyServiceGrpc.GovernancePolicyServiceImplBase {
  @Autowired
  private ResourceGovernancePolicyService resourceGovernancePolicyService;

  @Autowired
  private ObjectMapper objectMapper; // for converting structs to Dtos

  @Override
  public void upsertPolicy(UpsertPolicyRequest request, StreamObserver<UpsertPolicyResponse> responseObserver) {
    try {
      String tenantId = request.getTenantId();
      UUID resourceId = UUID.fromString(request.getResourceId());
      String identityType = request.getIdentityType();
      UUID schemaId = request.getSchemaId() != null && !request.getSchemaId().isEmpty() 
          ? UUID.fromString(request.getSchemaId()) 
          : null;

      IdentityMappingPolicyDTO policyDto = convertProtoToDtoWithReactions(request.getPolicy(),
          UUID.fromString(request.getResourceId()));
      resourceGovernancePolicyService.updatePolicy(tenantId, IdentityType.fromValue(identityType.toUpperCase()),
          resourceId, schemaId,
          policyDto);

      // Persist synchronization reactions separately (JSONB column)
      if (policyDto.synchronizationReactions() != null && !policyDto.synchronizationReactions().isEmpty()) {
        resourceGovernancePolicyService.updateSynchronizationReactions(
            tenantId, IdentityType.fromValue(identityType.toUpperCase()),
            resourceId, policyDto.synchronizationReactions());
      }

      UpsertPolicyResponse response = UpsertPolicyResponse.newBuilder()
          .setSuccess(true)
          .setMessage("Policy updated successfully.")
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      UpsertPolicyResponse response = UpsertPolicyResponse.newBuilder()
          .setSuccess(false)
          .setMessage("Failed to update policy: " + e.getMessage())
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted(); // Use onNext + onCompleted for a graceful error response
    }
  }

  private IdentityMappingPolicyDTO convertProtoToDto(IdentityMappingPolicy proto, UUID resourceId) {

    // Convert correlation rules
    List<CorrelationRuleDTO> correlations = proto.getCorrelationRulesList().stream()
        .map(c -> {
          try {
            String ruleGroupJson = JsonFormat.printer().print(c.getRuleGroup());

            OnboardingRequestDTO.RuleGroupDTO ruleGroup = null;
            OnboardingRequestDTO.ScriptDTO script = null;

            if ("script".equalsIgnoreCase(c.getMode())) {
              script = objectMapper.readValue(ruleGroupJson, OnboardingRequestDTO.ScriptDTO.class);
            } else {
              ruleGroup = objectMapper.readValue(ruleGroupJson, OnboardingRequestDTO.RuleGroupDTO.class);
            }

            return new CorrelationRuleDTO(
                UUID.fromString(c.getId()),
                UUID.fromString(c.getResourceId()),
                c.getMode(),
                ruleGroup,
                script,
                null,
                null);
          } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize a CorrelationRule's RuleGroup from proto Struct.", e);
          }
        })
        .collect(Collectors.toList());

    // Convert attribute mappings
    List<AttributeMappingDTO> mappings = proto.getAttributeMappingsList().stream()
        .map(m -> {
          OnboardingRequestDTO.ScriptDTO script = null;
          if (m.hasScript()) {
            try {
              String scriptJson = JsonFormat.printer().print(m.getScript());
              script = objectMapper.readValue(scriptJson, OnboardingRequestDTO.ScriptDTO.class);
            } catch (Exception e) {
              throw new RuntimeException("Failed to deserialize script", e);
            }
          }
          return new AttributeMappingDTO(
              UUID.fromString(m.getId()),
              UUID.fromString(m.getSourceResourceId()),
              m.getSourceAttributeName(),
              m.getTargetAttributeName(),
              m.getMode(),
              script);
        })
        .collect(Collectors.toList());

    return new IdentityMappingPolicyDTO(resourceId, correlations, mappings);
  }

  private IdentityMappingPolicyDTO convertProtoToDtoWithReactions(IdentityMappingPolicy proto, UUID resourceId) {
    IdentityMappingPolicyDTO base = convertProtoToDto(proto, resourceId);

    // Convert synchronization reactions
    List<SynchronizationReactionDTO> reactions = proto.getSynchronizationReactionsList().stream()
        .map(r -> new SynchronizationReactionDTO(r.getSituation(), r.getAction()))
        .collect(Collectors.toList());

    return new IdentityMappingPolicyDTO(resourceId, base.correlationRules(), base.attributeMappings(), reactions);
  }

  @Override
  public void getPolicy(GetPolicyRequest request, StreamObserver<IdentityMappingPolicy> responseObserver) {
    try {
      String tenantId = request.getTenantId();
      UUID resourceId = UUID.fromString(request.getResourceId());
      String identityType = request.getIdentityType();

      IdentityMappingPolicyDTO policyDto = resourceGovernancePolicyService.getPolicy(tenantId,
          IdentityType.fromValue(identityType.toUpperCase()), resourceId);

      // Enrich with synchronization reactions from JSONB
      List<SynchronizationReactionDTO> reactions = resourceGovernancePolicyService
          .getSynchronizationReactions(tenantId, IdentityType.fromValue(identityType.toUpperCase()), resourceId);
      IdentityMappingPolicyDTO enrichedDto = new IdentityMappingPolicyDTO(
          policyDto.resourceId(), policyDto.correlationRules(), policyDto.attributeMappings(), reactions);

      IdentityMappingPolicy policyProto = convertDtoToProtoWithReactions(enrichedDto);

      responseObserver.onNext(policyProto);
      responseObserver.onCompleted();

    } catch (Exception e) {
      responseObserver.onError(io.grpc.Status.INTERNAL
          .withDescription("Failed to retrieve policy: " + e.getMessage())
          .asRuntimeException());
    }
  }

  @Override
  public void deletePoliciesByResource(DeletePoliciesByResourceRequest request,
      StreamObserver<DeletePoliciesByResourceResponse> responseObserver) {
    try {
      String tenantId = request.getTenantId();
      UUID resourceId = UUID.fromString(request.getResourceId());

      int deletedCount = resourceGovernancePolicyService.deletePoliciesByResource(tenantId, resourceId);

      DeletePoliciesByResourceResponse response = DeletePoliciesByResourceResponse.newBuilder()
          .setSuccess(true)
          .setMessage("Successfully deleted " + deletedCount + " policies for resource " + resourceId)
          .setDeletedCount(deletedCount)
          .build();

      responseObserver.onNext(response);
      responseObserver.onCompleted();

    } catch (Exception e) {
      DeletePoliciesByResourceResponse response = DeletePoliciesByResourceResponse.newBuilder()
          .setSuccess(false)
          .setMessage("Failed to delete policies: " + e.getMessage())
          .setDeletedCount(0)
          .build();
      responseObserver.onNext(response);
      responseObserver.onCompleted();
    }
  }

  private IdentityMappingPolicy convertDtoToProto(IdentityMappingPolicyDTO dto) {
    IdentityMappingPolicy.Builder builder = IdentityMappingPolicy.newBuilder();
    // Convert correlation rules
    if (dto.correlationRules() != null) {
      dto.correlationRules().forEach(c -> {
        try {
          CorrelationRule.Builder ruleBuilder = CorrelationRule.newBuilder()
              .setId(c.id() != null ? c.id().toString() : UUID.randomUUID().toString())
              .setResourceId(c.resourceId() != null ? c.resourceId().toString() : "")
              .setMode(c.mode() != null ? c.mode() : "attribute");

          // Use the same approach as core service
          Map<String, Object> ruleGroupMap = objectMapper.convertValue(
              c.ruleGroup(),
              new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
              });

          Struct.Builder structBuilder = Struct.newBuilder();
          for (Map.Entry<String, Object> entry : ruleGroupMap.entrySet()) {
            structBuilder.putFields(entry.getKey(), objectToProtobufValue(entry.getValue()));
          }

          ruleBuilder.setRuleGroup(structBuilder.build());
          builder.addCorrelationRules(ruleBuilder.build());
        } catch (Exception e) {
          throw new RuntimeException("Failed to serialize a CorrelationRule's RuleGroup to proto Struct.", e);
        }
      });
    }

    // Convert attribute mappings
    if (dto.attributeMappings() != null) {
      dto.attributeMappings().forEach(m -> {
        AttributeMapping.Builder mappingBuilder = AttributeMapping.newBuilder()
            .setId(m.id() != null ? m.id().toString() : UUID.randomUUID().toString())
            .setSourceResourceId(m.sourceResourceId() != null ? m.sourceResourceId().toString() : "")
            .setSourceAttributeName(m.sourceAttributeName() != null ? m.sourceAttributeName() : "")
            .setMode(m.mode() != null ? m.mode() : "default")
            .setTargetAttributeName(m.targetAttributeName() != null ? m.targetAttributeName() : "");

        if (m.script() != null) {
          try {
            Map<String, Object> scriptMap = objectMapper.convertValue(m.script(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                });
            Struct.Builder structBuilder = Struct.newBuilder();
            for (Map.Entry<String, Object> entry : scriptMap.entrySet()) {
              structBuilder.putFields(entry.getKey(), objectToProtobufValue(entry.getValue()));
            }
            mappingBuilder.setScript(structBuilder.build());
          } catch (Exception e) {
            throw new RuntimeException("Failed to serialize script", e);
          }
        }

        builder.addAttributeMappings(mappingBuilder.build());
      });
    }

    return builder.build();
  }

  private IdentityMappingPolicy convertDtoToProtoWithReactions(IdentityMappingPolicyDTO dto) {
    IdentityMappingPolicy base = convertDtoToProto(dto);
    IdentityMappingPolicy.Builder builder = base.toBuilder();

    // Convert synchronization reactions
    if (dto.synchronizationReactions() != null) {
      dto.synchronizationReactions().forEach(r -> {
        builder.addSynchronizationReactions(
            SynchronizationReaction.newBuilder()
                .setSituation(r.situation())
                .setAction(r.action())
                .build());
      });
    }

    return builder.build();
  }

  private Value objectToProtobufValue(Object value) {
    if (value == null) {
      return Value.newBuilder()
          .setNullValue(NullValue.NULL_VALUE).build();
    }
    if (value instanceof String s) {
      return Value.newBuilder().setStringValue(s).build();
    }
    if (value instanceof Double d) {
      return Value.newBuilder().setNumberValue(d).build();
    }
    if (value instanceof Number n) {
      return Value.newBuilder().setNumberValue(n.doubleValue()).build();
    }
    if (value instanceof Boolean b) {
      return Value.newBuilder().setBoolValue(b).build();
    }
    if (value instanceof List<?> list) {
      ListValue.Builder listBuilder = ListValue.newBuilder();
      list.forEach(item -> listBuilder.addValues(objectToProtobufValue(item)));
      return Value.newBuilder().setListValue(listBuilder.build()).build();
    }
    if (value instanceof Map<?, ?> map) {
      Struct.Builder structBuilder = Struct.newBuilder();
      map.forEach((k, v) -> structBuilder.putFields(k.toString(), objectToProtobufValue(v)));
      return Value.newBuilder().setStructValue(structBuilder.build()).build();
    }
    return Value.newBuilder().setStringValue(value.toString()).build();
  }
}
