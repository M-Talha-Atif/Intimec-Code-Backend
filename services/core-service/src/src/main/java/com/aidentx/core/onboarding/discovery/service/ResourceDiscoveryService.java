package com.aidentx.core.onboarding.discovery.service;

import com.aidentx.dal.protos.runtime.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;
import com.google.protobuf.util.JsonFormat;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ResourceDiscoveryService {

    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    private final ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub runtimeClient;
    private final JsonFormat.Printer protoJsonPrinter;

    public ResourceDiscoveryService(
            ObjectMapper objectMapper,
            ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub runtimeClient,
            UnifiedLoggerFactory loggerFactory) {
        this.objectMapper = objectMapper;
        this.runtimeClient = runtimeClient;
        this.logger = loggerFactory.getLogger("ResourceDiscoveryService");
        this.protoJsonPrinter = JsonFormat.printer().preservingProtoFieldNames();
    }

    /**
     * Implements "Step 1: Discover Available Connectors"
     */
    public Map<String, Object> getAvailableConnectors() {
        logger.info().message("Requesting list of available connectors.").log();
        DiscoverConnectorsResponse response = runtimeClient
                .discoverConnectors(com.google.protobuf.Empty.getDefaultInstance());
        List<Map<String, String>> connectors = response.getConnectorsList().stream()
                .map(c -> Map.of("id", c.getId(), "label", c.getLabel(), "bundle_version", c.getBundleVersion(), "iconUrl", c.getIconUrl()))
                .collect(Collectors.toList());
        return Map.of("connectors", connectors);
    }

    /**
     * Get the icon data for a specific connector by ID
     */
    public IconData getConnectorIconData(String connectorId) {
        logger.info().message(String.format("Requesting icon data for connector ID: %s", connectorId)).log();
        try {
            GetConnectorIconRequest request = GetConnectorIconRequest.newBuilder()
                    .setId(connectorId)
                    .build();
            GetConnectorIconResponse response = runtimeClient.getConnectorIcon(request);
            return new IconData(response.getIconData().toByteArray(), response.getContentType());
        } catch (Exception e) {
            logger.error().message(String.format("Failed to get icon data for connector ID %s", connectorId)).log();
            // Return default icon data
            return getDefaultIconData();
        }
    }

    /**
     * Get default icon data
     */
    private IconData getDefaultIconData() {
        try {
            // Try to load default icon from classpath
            var inputStream = getClass().getResourceAsStream("/icons/default-icon.png");
            if (inputStream != null) {
                byte[] data = inputStream.readAllBytes();
                inputStream.close();
                return new IconData(data, "image/png");
            }
        } catch (Exception e) {
            logger.error().message("Failed to load default icon").log();
        }
        return new IconData(new byte[0], "application/octet-stream");
    }

    /**
     * Simple data class to hold icon data and content type
     */
    public static class IconData {
        private final byte[] data;
        private final String contentType;

        public IconData(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }

        public byte[] getData() {
            return data;
        }

        public String getContentType() {
            return contentType;
        }
    }

    /**
     * Implements "Step 2: Get Connector Properties"
     */
    public Map<String, Object> getConnectorProperties(String connectorId) {
        logger.info().message("Requesting properties for connector.").detail("connectorId", connectorId).log();
        GetConnectorPropertiesRequest request = GetConnectorPropertiesRequest.newBuilder().setId(connectorId).build();
        ConnectorProperties response = runtimeClient.getConnectorProperties(request);
        return protoResponseToMap(response);
    }

    /**
     * Implements "Step 3: Test Connection"
     */
    public Map<String, Object> testConnection(String connectorId, Map<String, Object> configuration) throws Exception {
        logger.info().message("Initiating connection test.").detail("connectorId", connectorId).log();
    Map<String, Object> config = extractConfigurationIfWrapped(configuration);
    TestConnectionRequest request = TestConnectionRequest.newBuilder()
        .setId(connectorId)
        .setConfiguration(mapToStruct(config))
        .build();
        TestConnectionResponse response = runtimeClient.testConnection(request);
        return protoResponseToMap(response);
    }

    /**
     * Implements "Step 4: Discover Object Classes"
     */
    public List<String> discoverObjectClasses(String connectorId, Map<String, Object> configuration) throws Exception {
        logger.info().message("Discovering object classes for connector.").detail("connectorId", connectorId).log();
    Map<String, Object> config = extractConfigurationIfWrapped(configuration);
    DiscoverObjectClassesRequest request = DiscoverObjectClassesRequest.newBuilder()
        .setId(connectorId)
        .setConfiguration(mapToStruct(config))
        .build();
    DiscoverObjectClassesResponse response = runtimeClient.discoverObjectClasses(request);
    return response.getObjectClassNamesList();
    }

    /**
     * Implements "Step 4.1: Discover Object Schema"
     */
    public Map<String, Object> discoverObjectSchema(String connectorId, String objectName,
            Map<String, Object> configuration) throws Exception {
        logger.info().message("Discovering schema for object.").detail("connectorId", connectorId)
                .detail("objectName", objectName).log();
        Map<String, Object> config = extractConfigurationIfWrapped(configuration);
        FetchSchemaRequest request = FetchSchemaRequest.newBuilder()
                .setId(connectorId)
                .setObjectClassName(objectName)
                .setConfiguration(mapToStruct(config))
                .build();
        FetchSchemaResponse response = runtimeClient.fetchSchema(request);
        return structToMap(response.getSchema());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractConfigurationIfWrapped(Map<String, Object> maybeWrapped) {
        if (maybeWrapped == null) {
            return Map.of();
        }
        Object inner = maybeWrapped.get("configuration");
        if (inner instanceof Map) {
            return (Map<String, Object>) inner;
        }
        return maybeWrapped;
    }

    // --- Helper Methods ---
    private Struct mapToStruct(Map<String, Object> map) throws Exception {
        if (map == null)
            return Struct.getDefaultInstance();
        Struct.Builder builder = Struct.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(objectMapper.writeValueAsString(map), builder);
        return builder.build();
    }

    private Map<String, Object> structToMap(Struct struct) {
        try {
            String jsonString = protoJsonPrinter.print(struct);
            return objectMapper.readValue(jsonString, new TypeReference<>() {
            });
        } catch (Exception e) {
            logger.error(e).message("Failed to convert protobuf Struct to Map").log();
            return new HashMap<>();
        }
    }

    private Map<String, Object> protoResponseToMap(com.google.protobuf.MessageOrBuilder response) {
        try {
            String jsonString = protoJsonPrinter.print(response);
            return objectMapper.readValue(jsonString, new TypeReference<>() {
            });
        } catch (Exception e) {
            logger.error(e).message("Failed to convert protobuf Response to Map").log();
            return Map.of("error", "Failed to serialize protobuf response", "message", e.getMessage());
        }
    }

    @SuppressWarnings({"unchecked"})
    public Map<String, Object> previewObjectType(String connectorId, String objectClassName,
            Map<String, Object> requestBody, int page, int size)
            throws Exception {
        logger.info().message("Initiating object type preview.")
                .detail("connectorId", connectorId)
                .detail("page", page)
                .detail("size", size)
                .log();

        // Extract parameters from the request body
    Map<String, Object> configuration = requestBody.containsKey("configuration") && requestBody.get("configuration") instanceof Map
        ? (Map<String, Object>) requestBody.get("configuration")
        : Map.of();
    List<String> attributesToInclude = requestBody.containsKey("attributesToInclude") && requestBody.get("attributesToInclude") instanceof List
        ? (List<String>) requestBody.get("attributesToInclude")
        : null;

        if (configuration == null || objectClassName == null || attributesToInclude == null) {
            throw new IllegalArgumentException(
                    "Request body must contain 'configuration', 'objectClassName', and 'attributesToInclude'.");
        }

        // Prepare the gRPC request — tell the server exactly how many rows we need.
        // We need (page * size) rows to skip + size rows for the page + 1 to check hasNextPage.
        int totalNeeded = (page + 1) * size + 1;
        SearchObjectsRequest grpcRequest = SearchObjectsRequest.newBuilder()
                .setId(connectorId)
                .setConfiguration(mapToStruct(configuration))
                .setObjectClassName(objectClassName)
                .setPageSize(totalNeeded)
                .build();

        Iterator<ConnectorObject> iterator = runtimeClient.previewObjects(grpcRequest);

        // Skip rows before the requested page
        int toSkip = page * size;
        int skipped = 0;
        while (iterator.hasNext() && skipped < toSkip) {
            iterator.next();
            skipped++;
        }

        // Collect exactly one page of results
        List<Map<String, Object>> pageContent = new ArrayList<>(size);
        while (iterator.hasNext() && pageContent.size() < size) {
            ConnectorObject obj = iterator.next();
            Map<String, Object> fullAttributes = connectorObjectToMap(obj);
            Map<String, Object> filtered = new HashMap<>();
            for (String attr : attributesToInclude) {
                if (fullAttributes.containsKey(attr)) {
                    filtered.put(attr, fullAttributes.get(attr));
                }
            }
            pageContent.add(filtered);
        }

        // Only peek one more row to check if there's a next page — don't drain the entire stream
        boolean hasNextPage = iterator.hasNext();

        return Map.of(
                "content", pageContent,
                "page", page,
                "size", size,
                "hasNextPage", hasNextPage
        );
    }

    // Add a helper method to convert a ConnectorObject to a Map
    private Map<String, Object> connectorObjectToMap(ConnectorObject connectorObject) {
        try {
            if (connectorObject == null) {
                return new HashMap<>();
            }
            return structToMap(connectorObject.getAttributes());
        } catch (Exception e) {
            logger.error(e).message("Failed to convert ConnectorObject to Map").log();
            return new HashMap<>();
        }
    }
}