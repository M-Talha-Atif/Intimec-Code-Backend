package com.aidentx.iam.policy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityTypePolicyDTO;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.policy.model.entity.IdentityTypePolicy;
import com.aidentx.iam.policy.repository.IdentityTypePolicyRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IdentityTypePolicyServiceImpl implements IdentityTypePolicyService {

  private final IdentityTypePolicyRepository identityTypePolicyRepository;
  private final ObjectMapper objectMapper;
  private final IUnifiedLogger logger;
  private final GovernancePolicyHelper helper;

  public IdentityTypePolicyServiceImpl(IdentityTypePolicyRepository identityTypePolicyRepository,
      ObjectMapper objectMapper,
      UnifiedLoggerFactory loggerFactory,
      GovernancePolicyHelper helper) {
    this.identityTypePolicyRepository = identityTypePolicyRepository;
    this.objectMapper = objectMapper;
    this.logger = loggerFactory.getLogger(IdentityTypePolicyServiceImpl.class);
    this.helper = helper;
  }

  @Override
  @Transactional
  public IdentityTypePolicyDTO getIdentityTypePolicy(String tenantId, IdentityType identityType) {
    logger.info().message("Retrieving identity type policy")
        .detail("tenantId", tenantId)
        .detail("identityType", identityType)
        .log();

    helper.validateIdentityType(identityType);
    return identityTypePolicyRepository.findByTenantIdAndIdentityType(tenantId, identityType)
        .map(this::convertEntityToDto)
        .orElse(new IdentityTypePolicyDTO(List.of()));
  }

  @Override
  @Transactional
  public IdentityTypePolicyDTO updateIdentityTypePolicy(String tenantId, IdentityType identityType,
      IdentityTypePolicyDTO policyDto) {
    logger.info().message("Updating identity type policy")
        .detail("tenantId", tenantId)
        .detail("identityType", identityType)
        .log();

    helper.validateIdentityType(identityType);
    IdentityTypePolicy policy = identityTypePolicyRepository
        .findByTenantIdAndIdentityType(tenantId, identityType)
        .orElse(new IdentityTypePolicy(tenantId, identityType));

    if (policyDto.sourcePriorities() != null) {
      policy.setSourcePriorities(objectMapper.valueToTree(policyDto.sourcePriorities()));
    }

    identityTypePolicyRepository.save(policy);
    return convertEntityToDto(policy);
  }

  private IdentityTypePolicyDTO convertEntityToDto(IdentityTypePolicy policy) {
    List<OnboardingRequestDTO.AttributeSourcePriorityDTO> sourcePriorities = policy.getSourcePriorities() != null
        ? objectMapper.convertValue(policy.getSourcePriorities(),
            new TypeReference<List<OnboardingRequestDTO.AttributeSourcePriorityDTO>>() {
            })
        : List.of();

    return new IdentityTypePolicyDTO(sourcePriorities);
  }
}
