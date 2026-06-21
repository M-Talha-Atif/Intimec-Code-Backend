package com.aidentx.connector_runtime_service.mapper;

import com.aidentx.connector_runtime_service.service.ConnectorDiscoveryService;
import com.aidentx.dal.protos.runtime.ConnectorProperties;
import com.aidentx.dal.protos.runtime.ConnectorProperty;
import com.aidentx.dal.protos.runtime.DiscoveredConnector;
import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import com.aidentx.logging.service.IUnifiedLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;

import java.util.List;

/**
 * Utility that maps between ConnId/JVM objects and the protobuf messages used
 * by the gRPC API. Contains best-effort conversions for connector properties
 * and connector objects to protobuf representations.
 */
public class ProtoMapper {

        // Prevent instantiation for utility class
        private ProtoMapper() {
        }

        private static final IUnifiedLogger logger = new com.aidentx.logging.service.impl.UnifiedLoggerImpl(
                        LogManager.getLogger(ProtoMapper.class), new ObjectMapper(), "connector-runtime-service");

        /**
         * Converts a ConnId {@link ConnectorInfo} to a protobuf
         * {@code DiscoveredConnector}.
         */
        public static DiscoveredConnector toDiscoveredConnector(ConnectorInfo info) {
                String keyString = info.getConnectorKey().toString();
                String id = ConnectorDiscoveryService.generateId(keyString);
                String iconUrl = "/api/v1/connectors/" + id + "/icon";
                return DiscoveredConnector.newBuilder()
                                .setId(id)
                                .setLabel(generatePrettyLabel(info))
                                .setBundleVersion(info.getConnectorKey().getBundleVersion())
                                .setIconUrl(iconUrl)
                                .build();
        }

        private static String generatePrettyLabel(ConnectorInfo info) {
                String className = info.getConnectorKey().getConnectorName();
                switch (className) {
                        case "net.tirasa.connid.bundles.cmd.CmdConnector":
                                return "Cmd Connector";
                        case "net.tirasa.connid.bundles.ad.ADConnector":
                                return "AD Connector";
                        case "com.evolveum.polygon.connector.csv.CsvConnector":
                                return "CSV Connector";
                        case "org.identityconnectors.databasetable.DatabaseTableConnector":
                                return "Database Table Connector";
                }
                String shortClassName = className.substring(className.lastIndexOf('.') + 1);
                return shortClassName.replaceAll("(?<=[a-z])(?=[A-Z])", " ");
        }

        /**
         * Converts ConnId {@link ConfigurationProperty} metadata into a protobuf
         * {@code ConnectorProperties} message.
         */
        public static ConnectorProperties toConnectorProperties(ConnectorInfo info) {
                ConnectorProperties.Builder propertiesBuilder = ConnectorProperties.newBuilder()
                                .setDisplayName(generatePrettyLabel(info));
                info.createDefaultAPIConfiguration().getConfigurationProperties().getPropertyNames()
                                .forEach(propName -> {
                                        ConfigurationProperty prop = info.createDefaultAPIConfiguration()
                                                        .getConfigurationProperties().getProperty(propName);
                                        propertiesBuilder.addProperties(ConnectorProperty.newBuilder()
                                                        .setName(prop.getName())
                                                        .setDisplayName(prop.getDisplayName(prop.getName()))
                                                        .setHelpMessage(prop.getHelpMessage(prop.getName()))
                                                        .setType(mapConnectorTypeToUiType(prop.getType().getName()))
                                                        .setRequired(prop.isRequired())
                                                        .setConfidential(prop.isConfidential())
                                                        .build());
                                });
                return propertiesBuilder.build();
        }

        public static String mapConnectorTypeToUiType(String javaType) {
                switch (javaType) {
                        case "java.lang.String":
                        case "org.identityconnectors.common.security.GuardedString":
                                return "string";
                        case "boolean":
                        case "java.lang.Boolean":
                                return "boolean";
                        case "[Ljava.lang.String;":
                                return "string[]";
                        case "int":
                        case "java.lang.Integer":
                                return "int";
                        case "long":
                        case "java.lang.Long":
                                return "long";
                        case "java.io.File":
                                return "File";
                        case "char":
                                return "char";
                        case "org.identityconnectors.common.security.GuardedByteArray":
                                return "byte[]";
                        default:
                                logger.warn().message("Unmapped connector property type encountered: '" + javaType
                                                + "'. Returning original type.").log();
                                return javaType;
                }
        }

        /**
         * Converts a ConnId {@link ObjectClassInfo} into a protobuf {@code Struct}
         * representation of its schema.
         * This method avoids intermediate JSON string conversion for better
         * performance.
         */
        public static Struct toProtoSchema(ObjectClassInfo oci, Schema schema) {
                if (oci == null) {
                        return Struct.getDefaultInstance();
                }
                Struct.Builder schemaBuilder = Struct.newBuilder();
                schemaBuilder.putFields("type", Value.newBuilder().setStringValue(oci.getType()).build());

                ListValue.Builder attributesListBuilder = ListValue.newBuilder();
                oci.getAttributeInfo().stream()
                                .map(ProtoMapper::mapAttributeInfoToProtoValue)
                                .forEach(attributesListBuilder::addValues);

                schemaBuilder.putFields("attributes", Value.newBuilder().setListValue(attributesListBuilder).build());

                // Derive capabilities for this object class from the schema's supported
                // operations
                if (schema != null) {
                        ListValue.Builder capsListBuilder = ListValue.newBuilder();
                        schema.getSupportedObjectClassesByOperation().forEach((opClass, ociSet) -> {
                                if (ociSet != null && ociSet.contains(oci)) {
                                        // Normalize operation name for UI consumption
                                        String opName = normalizeOperationName(opClass.getSimpleName());
                                        capsListBuilder.addValues(Value.newBuilder().setStringValue(opName).build());
                                }
                        });
                        schemaBuilder.putFields("capabilities",
                                        Value.newBuilder().setListValue(capsListBuilder.build()).build());
                }
                return schemaBuilder.build();
        }

        private static String normalizeOperationName(String simpleName) {
                // Common ConnId op classes end with ApiOp, e.g., CreateApiOp
                String name = simpleName;
                if (name.endsWith("ApiOp")) {
                        name = name.substring(0, name.length() - "ApiOp".length());
                }
                return name.toLowerCase();
        }

        private static Value mapAttributeInfoToProtoValue(AttributeInfo attrInfo) {
                Struct.Builder attrBuilder = Struct.newBuilder();
                attrBuilder.putFields("name", Value.newBuilder().setStringValue(attrInfo.getName()).build());
                attrBuilder.putFields("type", Value.newBuilder()
                                .setStringValue(mapConnectorTypeToUiType(attrInfo.getType().getName())).build());

                if (attrInfo.getFlags() != null && !attrInfo.getFlags().isEmpty()) {
                        ListValue.Builder flagsListBuilder = ListValue.newBuilder();
                        attrInfo.getFlags().stream()
                                        .map(flag -> Value.newBuilder().setStringValue(flag.name()).build())
                                        .forEach(flagsListBuilder::addValues);
                        attrBuilder.putFields("flags", Value.newBuilder().setListValue(flagsListBuilder).build());
                }
                return Value.newBuilder().setStructValue(attrBuilder).build();
        }

        /**
         * Converts a ConnId {@link ConnectorObject} into the protobuf
         * {@code ConnectorObject} representation.
         *
         * <p>Handles the well-known ConnId issue where certain connectors (notably
         * the Evolveum CSV Connector) return string values as {@code List<String>}
         * where each element is a single character. Despite declaring attributes
         * as {@code String} type in their schema, these connectors internally
         * iterate over the {@code char[]} of each field value, producing e.g.
         * {@code ["F","u","l","l","-","T","i","m","e"]} for the value
         * {@code "Full-Time"}.</p>
         *
         * <p>The normalization detects this pattern (a multi-element list where
         * every element is a single-character String or a Character) and joins
         * them back into a proper string. This is equivalent to what MidPoint
         * does in its own provisioning normalization layer.</p>
         */
        public static com.aidentx.dal.protos.runtime.ConnectorObject toProtoConnectorObject(ConnectorObject connObj) {
                if (connObj == null) {
                        return com.aidentx.dal.protos.runtime.ConnectorObject.getDefaultInstance();
                }
                Struct.Builder attributesBuilder = Struct.newBuilder();
                connObj.getAttributes().forEach(
                                attr -> attributesBuilder.putFields(attr.getName(),
                                                toProtobufAttributeValue(attr.getValue())));

                return com.aidentx.dal.protos.runtime.ConnectorObject.newBuilder()
                                .setUid(connObj.getUid().getUidValue())
                                .setAttributes(attributesBuilder)
                                .build();
        }

        /**
         * Converts a ConnId Attribute value (always a {@code List<Object>}) to a
         * Protobuf {@link Value}.
         *
         * <p><b>Character-array normalization:</b> Some connectors (e.g. Evolveum
         * CSV) return each character of a text value as a separate element in the
         * attribute's value list — either as {@link Character} objects or as
         * single-character {@link String} objects. This method detects both patterns
         * and joins the characters back into a single string value.</p>
         *
         * <p>For legitimate multi-valued attributes (elements longer than 1 char),
         * the values are kept as a Protobuf {@code ListValue}.</p>
         */
        private static Value toProtobufAttributeValue(List<Object> attrValues) {
                if (attrValues == null || attrValues.isEmpty()) {
                        return Value.newBuilder()
                                        .setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
                }

                // ── Character-array normalization ─────────────────────────────────
                // Case 1: List<Character> — some connectors return actual char objects
                if (attrValues.get(0) instanceof Character) {
                        StringBuilder sb = new StringBuilder(attrValues.size());
                        for (Object ch : attrValues) {
                                sb.append(ch);
                        }
                        return Value.newBuilder().setStringValue(sb.toString()).build();
                }

                // Case 2: List<String> where every element is exactly 1 character long.
                // This is the actual pattern from the Evolveum CSV Connector — it
                // declares attributes as String type in its schema, but internally
                // iterates over the char[] of each field value.
                if (attrValues.size() > 1 && attrValues.get(0) instanceof String
                                && attrValues.stream().allMatch(v -> v instanceof String s && s.length() == 1)) {
                        StringBuilder sb = new StringBuilder(attrValues.size());
                        for (Object ch : attrValues) {
                                sb.append(ch);
                        }
                        return Value.newBuilder().setStringValue(sb.toString()).build();
                }

                // ── Normal attribute handling ─────────────────────────────────────
                // Single-valued attribute — unwrap to scalar value
                if (attrValues.size() == 1) {
                        return toProtobufValue(attrValues.get(0));
                }

                // Multi-valued attribute — keep as ListValue
                ListValue.Builder listBuilder = ListValue.newBuilder();
                for (Object item : attrValues) {
                        listBuilder.addValues(toProtobufValue(item));
                }
                return Value.newBuilder().setListValue(listBuilder.build()).build();
        }

        private static Value toProtobufValue(Object value) {
                if (value == null) {
                        return Value.newBuilder().setNullValue(com.google.protobuf.NullValue.NULL_VALUE).build();
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
                        list.forEach(item -> listBuilder.addValues(toProtobufValue(item)));
                        return Value.newBuilder().setListValue(listBuilder.build()).build();
                }
                return Value.newBuilder().setStringValue(value.toString()).build();
        }
}