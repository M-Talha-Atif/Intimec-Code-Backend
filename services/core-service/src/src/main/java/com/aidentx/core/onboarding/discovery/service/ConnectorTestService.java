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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConnectorTestService {

    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    private final ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub runtimeClient;

    // Added a Protobuf JSON printer as a class member for efficiency
    private final JsonFormat.Printer protoJsonPrinter;

    public ConnectorTestService(
            ObjectMapper objectMapper,
            ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub runtimeClient,
            UnifiedLoggerFactory loggerFactory) {
        this.objectMapper = objectMapper;
        this.runtimeClient = runtimeClient;
        this.logger = loggerFactory.getLogger("ConnectorTestService");
        // Initialize the printer
        this.protoJsonPrinter = JsonFormat.printer().preservingProtoFieldNames();
    }

    // CORRECTED: Return type is now Map<String, Object>
    public Map<String, Object> testConnection(Map<String, Object> requestBody) throws Exception {
        String connectorId = (String) requestBody.get("connectorId");
        Map<String, Object> configMap = safeCastToMap(requestBody.get("configuration"));

        logger.info().message("Initiating connection test.").detail("connectorId", connectorId).log();

        TestConnectionRequest request = TestConnectionRequest.newBuilder()
                .setId(connectorId)
                .setConfiguration(mapToStruct(configMap))
                .build();

        TestConnectionResponse response = runtimeClient.testConnection(request);

        // Convert the Protobuf response to a Map before returning
        return protoResponseToMap(response);
    }

    public List<Map<String, Object>> searchAccounts(String connectorId, Map<String, Object> requestBody)
            throws Exception {
        Map<String, Object> configMap = safeCastToMap(requestBody.get("configuration"));
        Map<String, Object> filterMap = safeCastToMap(requestBody.get("filter"));

        logger.info().message("Initiating search.").detail("connectorId", connectorId).log();

        Filter.Builder filterBuilder = Filter.newBuilder();
        if (filterMap != null && !filterMap.isEmpty()) {
            JsonFormat.parser().ignoringUnknownFields().merge(objectMapper.writeValueAsString(filterMap),
                    filterBuilder);
        }

        SearchObjectsRequest request = SearchObjectsRequest.newBuilder()
                .setId(connectorId)
                .setConfiguration(mapToStruct(configMap))
                .setObjectClassName("__ACCOUNT__")
                .setFilter(filterBuilder.build())
                .setPageSize(1000)
                .build();

        List<Map<String, Object>> results = new ArrayList<>();
        runtimeClient.searchObjects(request).forEachRemaining(connectorObject -> {
            Map<String, Object> attrs = structToMap(connectorObject.getAttributes());
            attrs.put("__UID__", connectorObject.getUid());
            results.add(attrs);
        });

        logger.info().message("Search completed.").detail("found", results.size()).log();
        return results;
    }

    // CORRECTED: Return type is now Map<String, Object>
    public Map<String, Object> createAccount(String connectorId, Map<String, Object> requestBody) throws Exception {
        Map<String, Object> configMap = safeCastToMap(requestBody.get("configuration"));
        Map<String, Object> attributesMap = safeCastToMap(requestBody.get("attributes"));

        logger.info().message("Initiating account creation.").detail("connectorId", connectorId).log();

        CreateObjectRequest request = CreateObjectRequest.newBuilder()
                .setId(connectorId)
                .setConfiguration(mapToStruct(configMap))
                .setObjectClassName("__ACCOUNT__")
                .setAttributes(mapToStruct(attributesMap))
                .build();

        CreateObjectResponse response = runtimeClient.createObject(request);
        return protoResponseToMap(response);
    }

    // CORRECTED: Return type is now Map<String, Object>
    public Map<String, Object> updateAccount(String connectorId, String uid, Map<String, Object> requestBody)
            throws Exception {
        Map<String, Object> configMap = safeCastToMap(requestBody.get("configuration"));
        Map<String, Object> attributesMap = safeCastToMap(requestBody.get("attributesToUpdate"));

        logger.info().message("Initiating account update.").detail("connectorId", connectorId).detail("uid", uid).log();

        UpdateObjectRequest request = UpdateObjectRequest.newBuilder()
                .setId(connectorId)
                .setConfiguration(mapToStruct(configMap))
                .setObjectClassName("__ACCOUNT__")
                .setUid(uid)
                .setAttributesToUpdate(mapToStruct(attributesMap))
                .build();

        UpdateObjectResponse response = runtimeClient.updateObject(request);
        return protoResponseToMap(response);
    }

    // CORRECTED: Return type is now Map<String, Object>
    public Map<String, Object> deleteAccount(String connectorId, String uid, Map<String, Object> requestBody)
            throws Exception {
        Map<String, Object> configMap = safeCastToMap(requestBody.get("configuration"));

        logger.info().message("Initiating account deletion.").detail("connectorId", connectorId).detail("uid", uid)
                .log();

        DeleteObjectRequest request = DeleteObjectRequest.newBuilder()
                .setId(connectorId)
                .setConfiguration(mapToStruct(configMap))
                .setObjectClassName("__ACCOUNT__")
                .setUid(uid)
                .build();

        DeleteObjectResponse response = runtimeClient.deleteObject(request);
        return protoResponseToMap(response);
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

    // NEW Helper method to convert any Protobuf response to a Map
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

    public List<Map<String, Object>> getAvailableConnectors() {
        logger.info().message("Requesting list of available connectors from runtime service.").log();

        // The DiscoverConnectors gRPC method takes an Empty request object.
        DiscoverConnectorsResponse response = runtimeClient.discoverConnectors(
                com.google.protobuf.Empty.getDefaultInstance());

        // Convert the Protobuf response into a simple List of Maps for the JSON
        // response.
        List<Map<String, Object>> connectors = response.getConnectorsList().stream()
                .map(connector -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", connector.getId());
                    map.put("label", connector.getLabel());
                    return map;
                })
                .collect(Collectors.toList());

        logger.info().message("Discovered available connectors.")
                .detail("count", connectors.size())
                .log();

        return connectors;
    }

    /**
     * Safely cast an object to Map<String,Object> if possible, otherwise return null.
     */
    private Map<String, Object> safeCastToMap(Object obj) {
        if (obj == null)
            return null;
        if (obj instanceof Map<?, ?> m) {
            // Ensure keys are strings - create a new Map<String,Object> if necessary
            boolean keysAreStrings = m.keySet().stream().allMatch(k -> k instanceof String);
            if (keysAreStrings) {
                return (Map<String, Object>) m;
            }
            Map<String, Object> converted = new HashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                converted.put(String.valueOf(e.getKey()), e.getValue());
            }
            return converted;
        }
        return null;
    }
}
