package com.aidentx.core.audit_management.config;

/**
 * Application-wide constants including error messages, error titles,
 * HTTP status descriptions, and validation rules.
 */
public class AppConstants {
    
    // Logger Categories
    public static final String LOG_CATEGORY_KAFKA_CONSUMER = "Kafka-Consumer";

    // Log Messages
    public static final String LOG_KAFKA_EVENT_RECEIVED = "Received AuditLogKafkaEvent from Kafka topic 'audit-log'";
    public static final String LOG_KAFKA_PROCESS_SUCCESS = "Successfully processed and saved audit log event";
    public static final String LOG_KAFKA_PROCESS_FAILURE = "Failed to process and save audit log event from Kafka";
    public static final String LOG_KAFKA_TENANT_ID_EXTRACTED = "Successfully extracted tenantId from audit event metadata";
    public static final String LOG_KAFKA_TENANT_ID_NOT_FOUND = "TenantId not found in audit event metadata, using fallback value";
    public static final String LOG_KAFKA_TENANT_ID_NULL_OR_EMPTY = "TenantId is null or empty, using 'unknown' as fallback";

    // JSON / Conversion
    public static final String JSONNODE_TO_STRING_ERROR = "Failed to convert JsonNode to String";
    public static final String STRING_TO_JSONNODE_ERROR = "Failed to convert String to JsonNode";

    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}