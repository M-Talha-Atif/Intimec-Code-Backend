package com.aidentx.iam.policy.service;

import com.aidentx.dal.protos.application.ApplicationServiceGrpc;
import com.aidentx.dal.protos.application.GetApplicationRequest;
import com.aidentx.dal.protos.application.GetApplicationResponse;
import com.aidentx.dal.protos.application.GetApplicationSchemaAttributesRequest;
import com.aidentx.dal.protos.application.GetApplicationSchemaAttributesResponse;
import com.aidentx.dal.protos.application.ApplicationDTO;
import com.aidentx.dal.protos.application.ApplicationSchemaAttributeDTO;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for fetching application schema information from core-service via
 * gRPC.
 * Used to validate source field names in correlation rules and attribute
 * mappings.
 */
@Service
public class ApplicationSchemaService {

  private final IUnifiedLogger logger;

  @GrpcClient("core-service")
  private ApplicationServiceGrpc.ApplicationServiceBlockingStub applicationServiceStub;

  public ApplicationSchemaService(UnifiedLoggerFactory loggerFactory) {
    this.logger = loggerFactory.getLogger(ApplicationSchemaService.class);
  }

  /**
   * Fetches the schema field names for a given application/resource via gRPC.
   * 
   * @param tenantId   The tenant identifier
   * @param resourceId The application/resource UUID
   * @return Set of source field names available in the application schema
   * @throws IllegalArgumentException if application not found or has no schema
   */
  public Set<String> getApplicationSchemaFieldNames(String tenantId, UUID resourceId) {
    logger.debug().message("Fetching application schema field names via gRPC")
        .detail("tenantId", tenantId)
        .detail("resourceId", resourceId)
        .log();

    try {
      GetApplicationSchemaAttributesRequest request = GetApplicationSchemaAttributesRequest.newBuilder()
          .setTenantId(tenantId)
          .setApplicationId(resourceId.toString())
          .build();

      GetApplicationSchemaAttributesResponse response = applicationServiceStub
          .getApplicationSchemaAttributes(request);

      if (response.getAttributesList().isEmpty()) {
        logger.warn().message("No schema attributes found for application")
            .detail("resourceId", resourceId)
            .log();
        return Collections.emptySet();
      }

      Set<String> fieldNames = response.getAttributesList().stream()
          .map(ApplicationSchemaAttributeDTO::getName)
          .filter(name -> name != null && !name.trim().isEmpty())
          .collect(Collectors.toSet());

      logger.debug().message("Successfully fetched schema field names via gRPC")
          .detail("resourceId", resourceId)
          .detail("fieldCount", fieldNames.size())
          .log();

      return fieldNames;

    } catch (io.grpc.StatusRuntimeException e) {
      logger.error(e).message("gRPC error fetching application schema")
          .detail("tenantId", tenantId)
          .detail("resourceId", resourceId)
          .detail("status", e.getStatus())
          .log();

      if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
        throw new IllegalArgumentException(
            "Application not found with resourceId: " + resourceId);
      }

      throw new IllegalArgumentException(
          "Failed to fetch application schema for resourceId: " + resourceId + ". " + e.getMessage(), e);

    } catch (Exception e) {
      logger.error(e).message("Failed to fetch application schema")
          .detail("tenantId", tenantId)
          .detail("resourceId", resourceId)
          .log();

      if (e instanceof IllegalArgumentException) {
        throw (IllegalArgumentException) e;
      }

      throw new IllegalArgumentException(
          "Failed to fetch application schema for resourceId: " + resourceId + ". " + e.getMessage(), e);
    }
  }

  /**
   * Validates that a resource ID exists as a registered application.
   * 
   * @param tenantId   The tenant identifier
   * @param resourceId The application/resource UUID
   * @throws IllegalArgumentException if application not found
   */
  public void validateResourceExists(String tenantId, UUID resourceId) {
    logger.debug().message("Validating resource exists")
        .detail("tenantId", tenantId)
        .detail("resourceId", resourceId)
        .log();

    try {
      GetApplicationRequest request = GetApplicationRequest.newBuilder()
          .setTenantId(tenantId)
          .setApplicationId(resourceId.toString())
          .build();

      GetApplicationResponse response = applicationServiceStub.getApplication(request);
      ApplicationDTO application = response.getApplication();

      if (application == null || application.getId().isEmpty()) {
        throw new IllegalArgumentException(
            "Application not found with resourceId: " + resourceId);
      }

      logger.debug().message("Resource validation successful")
          .detail("resourceId", resourceId)
          .detail("applicationName", application.getName())
          .log();

    } catch (Exception e) {
      logger.error(e).message("Resource validation failed")
          .detail("tenantId", tenantId)
          .detail("resourceId", resourceId)
          .log();

      if (e instanceof IllegalArgumentException) {
        throw (IllegalArgumentException) e;
      }

      throw new IllegalArgumentException(
          "Failed to validate resource existence for resourceId: " + resourceId, e);
    }
  }
}
