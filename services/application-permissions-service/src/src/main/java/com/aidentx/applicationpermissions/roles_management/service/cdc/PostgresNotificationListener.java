package com.aidentx.applicationpermissions.roles_management.service.cdc;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.service.reconciliation.DynamicRuleReconciliationService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * PostgreSQL Change Data Capture (CDC) listener service.
 * Listens for NOTIFY events from PostgreSQL triggers on the identities table.
 * When user data changes, triggers dynamic rule reconciliation for IMMEDIATE rules only.
 * 
 * This replaces the inefficient cron-based polling approach with true event-driven architecture.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
@ConditionalOnProperty(name = AppConstants.CDC_ENABLED_PROPERTY, havingValue = AppConstants.CDC_DEFAULT_ENABLED, matchIfMissing = true)
public class PostgresNotificationListener {

    private final IUnifiedLogger logger;
    private final DataSource dataSource;
    private final DynamicRuleReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService executorService;
    
    @Value("${postgres.cdc.channel:" + AppConstants.CDC_CHANNEL_USER_CHANGES + "}")
    private String cdcChannel;
    
    private volatile boolean listening = false;
    private Connection listenerConnection;

    public PostgresNotificationListener(
            UnifiedLoggerFactory loggerFactory,
            DataSource dataSource,
            DynamicRuleReconciliationService reconciliationService,
            ObjectMapper objectMapper) {
        this.logger = loggerFactory.getLogger(PostgresNotificationListener.class);
        this.dataSource = dataSource;
        this.reconciliationService = reconciliationService;
        this.objectMapper = objectMapper;
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Initializes the PostgreSQL CDC listener on application startup.
     * Establishes a dedicated connection and listens to the channel.
     */
    @PostConstruct
    public void startListening() {
        logger.info()
            .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
            .message(AppConstants.LOG_CDC_LISTENER_STARTING)
            .log();
        
        try {
            // Get a dedicated connection for listening
            listenerConnection = dataSource.getConnection();
            PGConnection pgConnection = listenerConnection.unwrap(PGConnection.class);
            
            // Execute LISTEN command
            try (Statement stmt = listenerConnection.createStatement()) {
                stmt.execute("LISTEN " + cdcChannel);
            }
            
            listening = true;
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message(AppConstants.LOG_CDC_LISTENER_STARTED)
                .detail(AppConstants.DETAIL_KEY_CDC_CHANNEL, cdcChannel)
                .log();
            
            // Start polling for notifications in background
            executorService.scheduleWithFixedDelay(
                this::pollNotifications,
                0,
                AppConstants.CDC_POLL_INTERVAL_MS,
                TimeUnit.MILLISECONDS
            );
                
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message(AppConstants.LOG_CDC_CONNECTION_FAILED)
                .log();
        }
    }
    
    /**
     * Polls for NOTIFY events from PostgreSQL.
     * Checks for notifications and processes them.
     */
    private void pollNotifications() {
        if (!listening || listenerConnection == null) {
            return;
        }
        
        try {
            PGConnection pgConnection = listenerConnection.unwrap(PGConnection.class);
            PGNotification[] notifications = pgConnection.getNotifications();
            
            if (notifications != null && notifications.length > 0) {
                for (PGNotification notification : notifications) {
                    handleNotification(notification.getName(), notification.getParameter());
                }
            }
            
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message(AppConstants.ERROR_CDC_POLL_FAILED)
                .log();
            
            // Attempt to reconnect
            reconnect();
        }
    }
    
    /**
     * Attempts to reconnect to PostgreSQL and re-establish LISTEN.
     */
    private void reconnect() {
        try {
            if (listenerConnection != null && !listenerConnection.isClosed()) {
                listenerConnection.close();
            }
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message("Reconnecting to PostgreSQL CDC channel")
                .log();
            
            listenerConnection = dataSource.getConnection();
            try (Statement stmt = listenerConnection.createStatement()) {
                stmt.execute("LISTEN " + cdcChannel);
            }
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message("Reconnected to PostgreSQL CDC channel")
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message("Failed to reconnect to PostgreSQL CDC")
                .log();
        }
    }
    
    /**
     * Cleanup when service stops.
     */
    @PreDestroy
    public void stopListening() {
        listening = false;
        executorService.shutdown();
        
        try {
            if (listenerConnection != null && !listenerConnection.isClosed()) {
                try (Statement stmt = listenerConnection.createStatement()) {
                    stmt.execute("UNLISTEN " + cdcChannel);
                }
                listenerConnection.close();
            }
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message(AppConstants.LOG_CDC_LISTENER_STOPPED)
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message(AppConstants.ERROR_CDC_LISTENER_CLOSE_FAILED)
                .log();
        }
    }

    /**
     * Handles incoming PostgreSQL NOTIFY events.
     * Parses the notification payload and triggers reconciliation for affected user.
     * 
     * @param channelName the PostgreSQL channel name
     * @param payload the JSON notification payload
     */
    private void handleNotification(String channelName, String payload) {
        logger.trace()
            .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
            .message(AppConstants.LOG_CDC_NOTIFICATION_RECEIVED)
            .detail(AppConstants.DETAIL_KEY_CDC_CHANNEL, channelName)
            .detail(AppConstants.DETAIL_KEY_CDC_NOTIFICATION, payload)
            .log();
        
        try {
            // Parse notification payload
            JsonNode notification = objectMapper.readTree(payload);
            String operation = notification.get(AppConstants.DETAIL_KEY_CDC_OPERATION).asText();
            String table = notification.get(AppConstants.DETAIL_KEY_CDC_TABLE).asText();
            UUID userId = UUID.fromString(notification.get(AppConstants.DETAIL_KEY_CDC_USER_ID).asText());
            String tenantId = notification.get(AppConstants.DETAIL_KEY_CDC_TENANT_ID).asText(); // tenant_id is String, not UUID
            long timestamp = notification.get(AppConstants.DETAIL_KEY_CDC_TIMESTAMP).asLong();
            
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message(AppConstants.LOG_CDC_NOTIFICATION_PARSED)
                .detail(AppConstants.DETAIL_KEY_CDC_OPERATION, operation)
                .detail(AppConstants.DETAIL_KEY_CDC_TABLE, table)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_CDC_TIMESTAMP, String.valueOf(timestamp))
                .log();
            
            // Trigger reconciliation (only for IMMEDIATE rules)
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_TRIGGERING_RECONCILIATION)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_CDC_OPERATION, operation)
                .log();
            
            triggerReconciliation(userId, tenantId, operation);
            
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message(AppConstants.LOG_CDC_NOTIFICATION_PROCESSED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_LISTENER)
                .message(AppConstants.LOG_CDC_NOTIFICATION_PARSE_FAILED)
                .detail(AppConstants.DETAIL_KEY_CDC_NOTIFICATION, payload)
                .log();
        }
    }

    /**
     * Triggers dynamic rule reconciliation for the affected user.
     * Only processes IMMEDIATE schedule rules (other schedules handled by cron).
     * 
     * @param userId the ID of the affected user
     * @param tenantId the tenant ID
     * @param operation the CDC operation (INSERT/UPDATE/DELETE)
     */
    private void triggerReconciliation(UUID userId, String tenantId, String operation) {
        try {
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_RECONCILIATION_STARTED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId)
                .detail(AppConstants.DETAIL_KEY_CDC_OPERATION, operation)
                .log();
            
            // Trigger reconciliation for this user (only IMMEDIATE rules)
            reconciliationService.reconcileUserWithImmediateRules(userId, tenantId);
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_RECONCILIATION_COMPLETED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId)
                .log();
            
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_CDC_RECONCILIATION)
                .message(AppConstants.LOG_CDC_RECONCILIATION_FAILED)
                .detail(AppConstants.DETAIL_KEY_CDC_USER_ID, userId.toString())
                .detail(AppConstants.DETAIL_KEY_CDC_TENANT_ID, tenantId.toString())
                .log();
        }
    }

}
