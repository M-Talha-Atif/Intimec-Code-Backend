package com.aidentx.connector_runtime_service.service;

import com.aidentx.connector_runtime_service.helper.OperationsHelper;
import com.aidentx.connector_runtime_service.mapper.ProtoMapper;
import com.aidentx.dal.protos.runtime.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;

import java.util.List;
import java.util.stream.Collectors;

/**
 * gRPC service implementation that exposes connector runtime operations.
 *
 * <p>
 * This class adapts incoming gRPC requests to the ConnId connector framework
 * via {@link OperationsHelper}. Methods are intentionally thin wrappers — they
 * validate the connector ID, delegate to the helper, and translate results to
 * protobuf responses.
 */
@GrpcService
public class ConnectorRuntimeServiceImpl extends ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceImplBase {

    private final IUnifiedLogger logger;
    private final ConnectorDiscoveryService discoveryService;
    private final OperationsHelper operationsHelper;

    /**
     * Create service with required collaborators.
     *
     * @param discoveryService discovery helper for available connectors
     * @param loggerFactory    factory used to obtain a structured logger
     * @param operationsHelper helper that performs connector operations
     */
    public ConnectorRuntimeServiceImpl(
            ConnectorDiscoveryService discoveryService,
            UnifiedLoggerFactory loggerFactory,
            OperationsHelper operationsHelper) {
        this.discoveryService = discoveryService;
        this.logger = loggerFactory.getLogger("ConnectorRuntimeServiceImpl", "provisioning");
        this.operationsHelper = operationsHelper;
    }

    @Override
    public void discoverConnectors(Empty request, StreamObserver<DiscoverConnectorsResponse> responseObserver) {
        var availableConnectors = discoveryService.getAvailableConnectors();
        var responseBuilder = DiscoverConnectorsResponse.newBuilder();

        availableConnectors.values()
                .forEach(info -> responseBuilder.addConnectors(ProtoMapper.toDiscoveredConnector(info)));

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getConnectorIcon(GetConnectorIconRequest request, StreamObserver<GetConnectorIconResponse> responseObserver) {
        try {
            // Get connector key from ID
            String connectorKey = discoveryService.getKeyFromId(request.getId());
            // logger.info().message(String.format("Getting icon for connector ID: %s, connector key: %s", request.getId(), connectorKey)).log();

            if (connectorKey == null) {
                // logger.info().message(String.format("Connector key is null for ID: %s, returning default icon", request.getId())).log();
                // Return default icon if connector not found
                loadDefaultIcon(responseObserver);
                return;
            }

            // Extract bundle name from connector key string
            // Format: "ConnectorKey( bundleName=... bundleVersion=... connectorName=... )"
            String bundleName = extractBundleName(connectorKey);
            // logger.info().message(String.format("Extracted bundle name: %s", bundleName)).log();

            if (bundleName == null || bundleName.isEmpty()) {
                // logger.info().message(String.format("Could not extract bundle name from connector key: %s, returning default icon", connectorKey)).log();
                loadDefaultIcon(responseObserver);
                return;
            }

            // Try to load icon from classpath using bundle name
            String iconPath = "/icons/" + bundleName + ".png";
            // logger.info().message(String.format("Trying to load icon from path: %s", iconPath)).log();

            try (var inputStream = getClass().getResourceAsStream(iconPath)) {
                if (inputStream != null) {
                    // logger.info().message(String.format("Found PNG icon for connector: %s", bundleName)).log();
                    byte[] iconBytes = inputStream.readAllBytes();
                    String contentType = determineContentType(bundleName + ".png");
                    responseObserver.onNext(GetConnectorIconResponse.newBuilder()
                            .setIconData(com.google.protobuf.ByteString.copyFrom(iconBytes))
                            .setContentType(contentType)
                            .build());
                    responseObserver.onCompleted();
                    return;
                } else {
                    // logger.info().message(String.format("PNG icon not found for connector: %s at path: %s", bundleName, iconPath)).log();
                }
            }

            // Try with .jpg extension if .png not found
            iconPath = "/icons/" + bundleName + ".jpg";
            // logger.info().message(String.format("Trying to load JPG icon from path: %s", iconPath)).log();

            try (var inputStream = getClass().getResourceAsStream(iconPath)) {
                if (inputStream != null) {
                    // logger.info().message(String.format("Found JPG icon for connector: %s", bundleName)).log();
                    byte[] iconBytes = inputStream.readAllBytes();
                    String contentType = determineContentType(bundleName + ".jpg");
                    responseObserver.onNext(GetConnectorIconResponse.newBuilder()
                            .setIconData(com.google.protobuf.ByteString.copyFrom(iconBytes))
                            .setContentType(contentType)
                            .build());
                    responseObserver.onCompleted();
                    return;
                } else {
                    // logger.info().message(String.format("JPG icon not found for connector: %s at path: %s", bundleName, iconPath)).log();
                }
            }

            // logger.info().message(String.format("No icon found for connector: %s, returning default icon", bundleName)).log();
            // Return default icon if specific icon not found
            loadDefaultIcon(responseObserver);

        } catch (Exception e) {
            // logger.error().message(String.format("Failed to get icon for connector ID %s", request.getId())).log();
            loadDefaultIcon(responseObserver);
        }
    }

    /**
     * Tests connectivity for a connector using the provided configuration.
     * The response contains a success flag and a human readable message.
     */
    @Override
    public void testConnection(TestConnectionRequest request, StreamObserver<TestConnectionResponse> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());
            logger.info().message(
                    String.format("Executing TestConnection for connector '{}'", info.getConnectorDisplayName())).log();
            operationsHelper.withConnectorFacade(info, request.getConfiguration(), facade -> {
                facade.test();
                return null;
            });
            responseObserver.onNext(
                    TestConnectionResponse.newBuilder().setSuccess(true).setMessage("Connection successful.").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error(e).message(String.format("Connection test failed for connector ID %s", request.getId())).log();
            responseObserver.onNext(TestConnectionResponse.newBuilder().setSuccess(false)
                    .setMessage("Test failed: " + e.getMessage()).build());
            responseObserver.onCompleted();
        }
    }
    // This new method goes inside the `ConnectorRuntimeServiceImpl` class

    @Override
    public void getConnectorProperties(GetConnectorPropertiesRequest request,
            StreamObserver<ConnectorProperties> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());
            logger.info()
                    .message(String.format("Fetching properties for connector '%s'", info.getConnectorDisplayName()))
                    .log();

            // Delegate the conversion logic to the ProtoMapper
            ConnectorProperties properties = ProtoMapper.toConnectorProperties(info);

            responseObserver.onNext(properties);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.fromThrowable(e).asRuntimeException());
        }
    }

    /**
     * Discovers object classes available from the configured connector.
     * Returns a sorted list of object class names.
     */
    @Override
    public void discoverObjectClasses(DiscoverObjectClassesRequest request,
            StreamObserver<DiscoverObjectClassesResponse> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());
            logger.info().message(
                    String.format("Discovering object classes for connector '%s'", info.getConnectorDisplayName()))
                    .log();

            List<String> objectClassNames = (List<String>) operationsHelper.withConnectorFacade(info,
                    request.getConfiguration(), facade -> {
                        Schema rawSchema = facade.schema();
                        return rawSchema.getObjectClassInfo().stream()
                                .map(ObjectClassInfo::getType)
                                .sorted()
                                .collect(Collectors.toList());
                    });

            DiscoverObjectClassesResponse response = DiscoverObjectClassesResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Object classes discovered successfully.")
                    .addAllObjectClassNames(objectClassNames)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    /**
     * Fetches the schema for a single object class from the connector and
     * converts it into a protobuf Struct suitable for transmission.
     */
    @Override
    public void fetchSchema(FetchSchemaRequest request, StreamObserver<FetchSchemaResponse> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());
            String objectClassName = request.getObjectClassName();
            logger.info().message(String.format("Fetching schema for '%s' on connector '%s'", objectClassName,
                    info.getConnectorDisplayName())).log();

            FetchSchemaResponse response = operationsHelper.withConnectorFacade(info, request.getConfiguration(),
                    facade -> {
            Schema rawSchema = facade.schema();
            ObjectClassInfo oci = rawSchema.findObjectClassInfo(objectClassName);
                        if (oci == null) {
                            throw new IllegalArgumentException("Object class '" + objectClassName + "' not found.");
                        }
                        return FetchSchemaResponse.newBuilder()
                                .setSuccess(true)
                                .setMessage("Schema for " + objectClassName + " loaded successfully")
                .setSchema(ProtoMapper.toProtoSchema(oci, rawSchema)) // Include capabilities
                                .build();
                    });

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    /**
     * Creates an object in the remote connector using the given attributes.
     */
    @Override
    public void createObject(CreateObjectRequest request, StreamObserver<CreateObjectResponse> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());
            logger.info().message(
                    String.format("Executing CreateObject for connector '%s'", info.getConnectorDisplayName()))
                    .log();
            Uid resultUid = (Uid) operationsHelper.withConnectorFacade(info, request.getConfiguration(),
                    facade -> facade.create(
                            new ObjectClass(request.getObjectClassName()),
                            operationsHelper.protoToConnIdAttributes(request.getAttributes()),
                            null));
            responseObserver.onNext(CreateObjectResponse.newBuilder().setUid(resultUid.getUidValue()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    /**
     * Updates an existing object identified by UID on the remote connector.
     */
    @Override
    public void updateObject(UpdateObjectRequest request, StreamObserver<UpdateObjectResponse> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());
            logger.info().message(
                    String.format("Executing UpdateObject for UID '%s' on connector '%s'", request.getUid(),
                            info.getConnectorDisplayName()))
                    .log();
            Uid resultUid = (Uid) operationsHelper.withConnectorFacade(info, request.getConfiguration(),
                    facade -> facade.update(
                            new ObjectClass(request.getObjectClassName()),
                            new Uid(request.getUid()),
                            operationsHelper.protoToConnIdAttributes(request.getAttributesToUpdate()),
                            null));
            responseObserver.onNext(UpdateObjectResponse.newBuilder().setUid(resultUid.getUidValue()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    /**
     * Deletes an object identified by UID from the remote connector.
     */
    @Override
    public void deleteObject(DeleteObjectRequest request, StreamObserver<DeleteObjectResponse> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());
            logger.info().message(
                    String.format("Executing DeleteObject for UID '%s' on connector '%s'", request.getUid(),
                            info.getConnectorDisplayName()))
                    .log();
            operationsHelper.withConnectorFacade(info, request.getConfiguration(), facade -> {
                facade.delete(new ObjectClass(request.getObjectClassName()), new Uid(request.getUid()), null);
                return null;
            });
            responseObserver.onNext(DeleteObjectResponse.newBuilder().setSuccess(true)
                    .setMessage("Object deleted successfully.").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    /**
     * Streams results of a search performed on the remote connector back to the
     * gRPC client. Pagination is delegated to {@link OperationsHelper}.
     */
    @Override
    public void searchObjects(SearchObjectsRequest request,
                              StreamObserver<com.aidentx.dal.protos.runtime.ConnectorObject> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());
            logger.info().message(
                    String.format("Executing streaming Search for connector '%s'", info.getConnectorDisplayName()))
                    .log();

            operationsHelper.search(
                    info,
                    request.getConfiguration(),
                    request.getObjectClassName(),
                    request.getFilter(),
                    request.getPageSize(),
                    connIdObject -> responseObserver.onNext(
                            ProtoMapper.toProtoConnectorObject(connIdObject)));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }
    

    /**
     * Enhanced search method with complex queries, pagination, and sorting support.
     */
    @Override
    public void searchObjectsPaged(SearchObjectsRequest request,
                                   StreamObserver<com.aidentx.dal.protos.runtime.SearchObjectsResponse> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());
            logger.info().message(
                    String.format("Executing enhanced paged Search for connector '%s'", info.getConnectorDisplayName()))
                    .log();
            
            com.aidentx.dal.protos.runtime.SearchObjectsResponse response = operationsHelper.search(
                    info,
                    request.getConfiguration(),
                    request.getObjectClassName(),
                    request.getFilter(),
                    request.getPageSize(),
                    request.getPageOffset(),
                    request.getSortBy(),
                    request.getSortDescending(),
                    request.getIncludeTotalCount(),
                    request.getSearchFieldsList());
                    
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    /**
     * Streams a limited number of results for previewing object data.
     * This is a specialized version of SearchObjects with a hardcoded limit.
     */
    @Override
    public void previewObjects(SearchObjectsRequest request,
                               StreamObserver<com.aidentx.dal.protos.runtime.ConnectorObject> responseObserver) {
        try {
            ConnectorInfo info = getConnectorInfoOrThrow(request.getId());

            // Use client-requested pageSize if > 0, otherwise default to 50.
            // Cap at MAX_PREVIEW_LIMIT to prevent abuse.
            final int DEFAULT_PREVIEW_LIMIT = 50;
            final int MAX_PREVIEW_LIMIT = 500;
            int limit = request.getPageSize() > 0
                    ? Math.min(request.getPageSize(), MAX_PREVIEW_LIMIT)
                    : DEFAULT_PREVIEW_LIMIT;

            logger.info().message(
                    String.format("Executing Preview for connector '%s' (limit=%d)",
                            info.getConnectorDisplayName(), limit))
                    .log();

            // Use searchWithLimit — this STOPS the connector scan after 'limit' objects.
            // Unlike the paged search() which iterates through ALL pages, this returns
            // false from the ResultsHandler to abort the connector immediately.
            operationsHelper.searchWithLimit(
                    info,
                    request.getConfiguration(),
                    request.getObjectClassName(),
                    request.getFilter(),
                    limit,
                    connIdObject -> responseObserver.onNext(
                            ProtoMapper.toProtoConnectorObject(connIdObject)));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException());
        }
    }

    /**
     * Resolves the connector id to a {@link ConnectorInfo} or throws a NOT_FOUND
     * gRPC status runtime exception.
     */
    private ConnectorInfo getConnectorInfoOrThrow(String connectorId) {
        String keyString = discoveryService.getKeyFromId(connectorId);
        if (keyString == null) {
            throw Status.NOT_FOUND.withDescription("Connector with ID not found: " + connectorId).asRuntimeException();
        }
        return discoveryService.getConnectorInfo(keyString);
    }

    /**
     * Loads the default icon from classpath and sends it via the response observer.
     */
    private void loadDefaultIcon(StreamObserver<GetConnectorIconResponse> responseObserver) {
        try {
            String defaultIconPath = "/icons/default-icon.png";
            try (var inputStream = getClass().getResourceAsStream(defaultIconPath)) {
                if (inputStream != null) {
                    byte[] defaultIconBytes = inputStream.readAllBytes();
                    responseObserver.onNext(GetConnectorIconResponse.newBuilder()
                            .setIconData(com.google.protobuf.ByteString.copyFrom(defaultIconBytes))
                            .setContentType("image/png")
                            .build());
                } else {
                    responseObserver.onNext(GetConnectorIconResponse.newBuilder()
                            .setIconData(com.google.protobuf.ByteString.EMPTY)
                            .setContentType("application/octet-stream")
                            .build());
                }
            }
        } catch (Exception e) {
            responseObserver.onNext(GetConnectorIconResponse.newBuilder()
                    .setIconData(com.google.protobuf.ByteString.EMPTY)
                    .setContentType("application/octet-stream")
                    .build());
        }
        responseObserver.onCompleted();
    }

    /**
     * Determines the content type based on file extension.
     */
    private String determineContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }

        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerFileName.endsWith(".ico")) {
            return "image/x-icon";
        } else {
            return "application/octet-stream";
        }
    }

    /**
     * Extracts the bundle name from a ConnectorKey string representation.
     * Expected format: "ConnectorKey( bundleName=... bundleVersion=... connectorName=... )"
     */
    private String extractBundleName(String connectorKeyString) {
        if (connectorKeyString == null || !connectorKeyString.contains("bundleName=")) {
            return null;
        }

        try {
            // Find the bundleName value between "bundleName=" and the next space or closing paren
            int bundleNameStart = connectorKeyString.indexOf("bundleName=") + "bundleName=".length();
            int bundleNameEnd = connectorKeyString.indexOf(" ", bundleNameStart);
            if (bundleNameEnd == -1) {
                bundleNameEnd = connectorKeyString.indexOf(")", bundleNameStart);
            }

            if (bundleNameStart > 0 && bundleNameEnd > bundleNameStart) {
                return connectorKeyString.substring(bundleNameStart, bundleNameEnd);
            }
        } catch (Exception e) {
            logger.error().message(String.format("Failed to extract bundle name from: %s", connectorKeyString)).log();
        }

        return null;
    }
}