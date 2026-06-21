package com.aidentx.taskmanagement.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aidentx.dal.protos.application.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.dto.ApplicationReference;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Service for fetching application configurations from the core-service via gRPC. This class
 * abstracts the gRPC client and provides graceful error handling.
 */
@Service
public class ApplicationConfigService {

  private final ApplicationServiceGrpc.ApplicationServiceBlockingStub applicationStub;
  private final IUnifiedLogger logger;

  public ApplicationConfigService(
      ApplicationServiceGrpc.ApplicationServiceBlockingStub applicationStub,
      UnifiedLoggerFactory loggerFactory) {
    this.applicationStub = applicationStub;
    this.logger = loggerFactory.getLogger(ApplicationConfigService.class);
  }

  /**
   * Fetches the full ApplicationDTO from the core-service.
   *
   * @param tenantId The tenant identifier.
   * @param applicationId The application UUID.
   * @return The full ApplicationDTO.
   * @throws RuntimeException if the gRPC call fails for reasons other than NOT_FOUND.
   */
  public ApplicationDTO getApplication(String tenantId, UUID applicationId) {
    try {
      logger
          .info()
          .message(TaskManagementConstants.MSG_FETCHING_APPLICATION_DETAILS)
          .detail("applicationId", applicationId)
          .log();
      GetApplicationRequest request =
          GetApplicationRequest.newBuilder()
              .setTenantId(tenantId)
              .setApplicationId(applicationId.toString())
              .build();
      return applicationStub.getApplication(request).getApplication();
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
        logger
            .warn()
            .message(TaskManagementConstants.MSG_APPLICATION_NOT_FOUND_CORE)
            .detail("applicationId", applicationId)
            .log();
        // Propagate as a standard exception that our global handler can catch
        throw new IllegalArgumentException(
            String.format(TaskManagementConstants.ERR_APPLICATION_NOT_FOUND, applicationId));
      }
      logger
          .error(e)
          .message(TaskManagementConstants.MSG_GRPC_ERROR_FETCHING_APPLICATION)
          .detail("status", e.getStatus())
          .log();
      throw new RuntimeException(
          String.format(
              TaskManagementConstants.ERR_FETCHING_APPLICATION_CONFIG,
              e.getStatus().getDescription()),
          e);
    }
  }

  /**
   * Fetches a lightweight reference to an application (ID and name). Gracefully handles cases where
   * the application is not found by returning a fallback reference.
   *
   * @param tenantId The tenant identifier.
   * @param applicationId The application UUID.
   * @return An ApplicationReference containing the ID and name, or a fallback if not found.
   */
  public ApplicationReference getApplicationReference(String tenantId, UUID applicationId) {
    if (applicationId == null) {
      return null;
    }
    try {
      ApplicationDTO app = getApplication(tenantId, applicationId);
      return new ApplicationReference(applicationId, app.getName());
    } catch (IllegalArgumentException e) {
      // Application not found, return a fallback reference with the ID as the name
      logger
          .debug()
          .message(TaskManagementConstants.MSG_APPLICATION_NOT_FOUND_FALLBACK)
          .detail("applicationId", applicationId)
          .log();
      return new ApplicationReference(applicationId, applicationId.toString());
    } catch (Exception e) {
      // Any other gRPC error, also return a fallback reference
      logger
          .warn()
          .message(TaskManagementConstants.MSG_FAILED_RESOLVE_APP_NAME_FALLBACK)
          .detail("applicationId", applicationId)
          .detail("error", e.getMessage())
          .log();
      return new ApplicationReference(applicationId, applicationId.toString());
    }
  }

  /**
   * Schema metadata for an entitlement object class, including the configured
   * display and description attribute names.
   */
  public record EntitlementObjectClassMeta(
      String objectClass,
      String displayAttribute,
      String descriptionAttribute,
      List<String> schemaAttributeNames) {

    /** Backward-compatible constructor (no schema attribute names). */
    public EntitlementObjectClassMeta(String objectClass, String displayAttribute, String descriptionAttribute) {
      this(objectClass, displayAttribute, descriptionAttribute, List.of());
    }
  }

  /**
   * Returns the entitlement object classes configured in the application's schema,
   * along with their display and description attribute names.
   * Falls back to a single __GROUP__ entry with null attributes if no entitlement
   * schemas are configured or the gRPC call fails.
   *
   * @param tenantId The tenant identifier.
   * @param applicationId The application UUID.
   * @return List of EntitlementObjectClassMeta entries.
   */
  public List<EntitlementObjectClassMeta> getEntitlementObjectClasses(String tenantId, UUID applicationId) {
    try {
      GetApplicationSchemaAttributesRequest request =
          GetApplicationSchemaAttributesRequest.newBuilder()
              .setTenantId(tenantId)
              .setApplicationId(applicationId.toString())
              .build();
      GetApplicationSchemaAttributesResponse response =
          applicationStub.getApplicationSchemaAttributes(request);

      // Group entitlement attributes by object class
      List<ApplicationSchemaAttributeDTO> entitlementAttrs = response.getAttributesList().stream()
          .filter(attr -> "entitlement".equalsIgnoreCase(attr.getObjectTypeKind()))
          .collect(Collectors.toList());

      if (entitlementAttrs.isEmpty()) {
        logger.info().message("No entitlement schemas configured — falling back to __GROUP__")
            .detail("applicationId", applicationId)
            .log();
        return List.of(new EntitlementObjectClassMeta(
            TaskManagementConstants.OBJECT_CLASS_GROUP, null, null));
      }

      // Build one meta entry per distinct object class, collecting attribute names
      Map<String, String> displayAttrMap = new java.util.LinkedHashMap<>();
      Map<String, String> descAttrMap = new java.util.LinkedHashMap<>();
      Map<String, List<String>> attrNamesMap = new java.util.LinkedHashMap<>();
      for (ApplicationSchemaAttributeDTO attr : entitlementAttrs) {
        String oc = attr.getObjectType();
        attrNamesMap.computeIfAbsent(oc, k -> new java.util.ArrayList<>()).add(attr.getName());
        if (attr.getIsDisplayAttribute()) {
          displayAttrMap.put(oc, attr.getName());
        }
        if (attr.getIsDescriptionAttribute()) {
          descAttrMap.put(oc, attr.getName());
        }
      }

      List<EntitlementObjectClassMeta> result = new java.util.ArrayList<>();
      for (String oc : attrNamesMap.keySet()) {
        result.add(new EntitlementObjectClassMeta(
            oc, displayAttrMap.get(oc), descAttrMap.get(oc), attrNamesMap.get(oc)));
      }

      logger.info().message("Resolved entitlement object classes from schema")
          .detail("applicationId", applicationId)
          .detail("classes", result.stream().map(EntitlementObjectClassMeta::objectClass).collect(Collectors.toList()))
          .log();
      return result;
    } catch (Exception e) {
      logger.warn().message("Failed to fetch schema — falling back to __GROUP__")
          .detail("applicationId", applicationId)
          .detail("error", e.getMessage())
          .log();
      return List.of(new EntitlementObjectClassMeta(
          TaskManagementConstants.OBJECT_CLASS_GROUP, null, null));
    }
  }

  /**
   * Returns the list of schema attribute names for accounts configured in the application.
   * If no account schema is found, returns an empty list (meaning: store all attributes).
   */
  public List<String> getAccountSchemaAttributeNames(String tenantId, UUID applicationId) {
    try {
      GetApplicationSchemaAttributesRequest request =
          GetApplicationSchemaAttributesRequest.newBuilder()
              .setTenantId(tenantId)
              .setApplicationId(applicationId.toString())
              .build();
      GetApplicationSchemaAttributesResponse response =
          applicationStub.getApplicationSchemaAttributes(request);

      List<String> accountAttrs = response.getAttributesList().stream()
          .filter(attr -> "account".equalsIgnoreCase(attr.getObjectTypeKind()))
          .map(ApplicationSchemaAttributeDTO::getName)
          .filter(name -> name != null && !name.trim().isEmpty())
          .collect(Collectors.toList());

      if (!accountAttrs.isEmpty()) {
        logger.info().message("Resolved account schema attributes")
            .detail("applicationId", applicationId)
            .detail("count", accountAttrs.size())
            .log();
      }
      return accountAttrs;
    } catch (Exception e) {
      logger.warn().message("Failed to fetch account schema — storing all attributes")
          .detail("applicationId", applicationId)
          .detail("error", e.getMessage())
          .log();
      return List.of();
    }
  }
}
