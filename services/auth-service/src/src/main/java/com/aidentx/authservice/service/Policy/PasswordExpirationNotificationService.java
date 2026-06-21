package com.aidentx.authservice.service.Policy;

import com.aidentx.authservice.config.PasswordExpirationNotificationConfig;
import com.aidentx.authservice.model.entity.PasswordPolicy;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.kafka.NotificationEventData;
import com.aidentx.authservice.repository.PasswordPolicyRepository;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.authservice.service.NotificationEventProducer;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for automatically sending password expiration notifications to users.
 * <p>
 * This service runs as a scheduled task to check for users whose passwords are about to expire
 * based on the password policy's passwordExpirationNotificationDays configuration.
 * Sends notifications via Kafka events using configurable operation and notification types.
 * The schedule runs daily at 9:00 AM by default but can be customized via configuration.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
@Service
public class PasswordExpirationNotificationService {

    private final IUnifiedLogger logger;
    private final UserRepository userRepository;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final NotificationEventProducer notificationEventProducer;
    private final PasswordExpirationNotificationConfig config;

    public PasswordExpirationNotificationService(UnifiedLoggerFactory unifiedLoggerFactory,
                                               UserRepository userRepository,
                                               PasswordPolicyRepository passwordPolicyRepository,
                                               NotificationEventProducer notificationEventProducer,
                                               PasswordExpirationNotificationConfig config) {
        this.logger = unifiedLoggerFactory.getLogger("PasswordExpirationNotificationService", AppConstants.LOG_CATEGORY_POLICY_MGMT);
        this.userRepository = userRepository;
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.notificationEventProducer = notificationEventProducer;
        this.config = config;
    }

    /**
     * Scheduled task that runs automatically to check and send password expiration notifications.
     * The schedule is configurable through application properties.
     * Default: Daily at 9:00 AM (cron = "0 0 9 * * *")
     */
    @Scheduled(cron = "${aidentx.auth.password-expiration-notification.cron-expression:0 0 9,21 * * *}")
    @Transactional
    public void checkAndSendPasswordExpirationNotifications() {
        if (!config.isEnabled()) {
            logger.debug().message(AppConstants.DEBUG_CRON_NOTIFICATION_DISABLED).log();
            return;
        }

        logger.info().message(AppConstants.INFO_CRON_NOTIFICATION_START).log();

        try {
            long currentTime = System.currentTimeMillis();
            
            // Get count first to check if we need to proceed
            long totalUsersRequiringNotification = userRepository.countUsersRequiringPasswordExpirationNotification(currentTime);
            
            if (totalUsersRequiringNotification == 0) {
                logger.info().message(AppConstants.INFO_CRON_NO_USERS_FOR_NOTIFICATION).log();
                return;
            }

            // Apply limit if needed
            int usersToProcess = Math.min((int) totalUsersRequiringNotification, config.getMaxUsersPerRun());
            if (totalUsersRequiringNotification > config.getMaxUsersPerRun()) {
                logger.warn()
                    .message(AppConstants.WARN_CRON_NOTIFICATION_LIMIT_REACHED)
                    .detail("totalUsers", totalUsersRequiringNotification)
                    .detail("maxUsersPerRun", config.getMaxUsersPerRun())
                    .log();
            }

            // Fetch users requiring notification
            Pageable pageable = PageRequest.of(0, usersToProcess);
            List<User> usersRequiringNotification = userRepository.findUsersRequiringPasswordExpirationNotification(currentTime, pageable);

            if (usersRequiringNotification.isEmpty()) {
                logger.info().message(AppConstants.INFO_CRON_NO_USERS_FOR_NOTIFICATION).log();
                return;
            }

            // Group users by tenant for efficient processing
            Map<String, List<User>> usersByTenant = usersRequiringNotification.stream()
                .collect(Collectors.groupingBy(User::getTenantId));

            logger.info()
                .message(AppConstants.INFO_CRON_FOUND_USERS_FOR_NOTIFICATION)
                .detail("totalUsers", usersRequiringNotification.size())
                .detail("tenantCount", usersByTenant.size())
                .log();

            int notificationsSent = 0;
            int notificationsFailed = 0;

            // Process each tenant
            for (Map.Entry<String, List<User>> tenantEntry : usersByTenant.entrySet()) {
                String tenantId = tenantEntry.getKey();
                List<User> tenantUsers = tenantEntry.getValue();

                logger.debug()
                    .message(AppConstants.DEBUG_CRON_PROCESSING_TENANT)
                    .detail("tenantId", tenantId)
                    .detail("userCount", tenantUsers.size())
                    .log();

                try {
                    // Get the password policy for this tenant to get notification days
                    Optional<PasswordPolicy> policyOpt = passwordPolicyRepository.findByTenantId(tenantId);
                    if (policyOpt.isEmpty() || policyOpt.get().getPasswordExpirationNotificationDays() == null) {
                        logger.warn()
                            .message(AppConstants.NO_PASSWORD_POLICY_CONFIGURED+ tenantId)
                            .detail("tenantId", tenantId)
                            .log();
                        notificationsFailed += tenantUsers.size();
                        continue;
                    }

                    int result = sendNotificationsForTenant(tenantId, tenantUsers, currentTime);
                    notificationsSent += result;
                    notificationsFailed += (tenantUsers.size() - result);

                } catch (Exception e) {
                    logger.error(e)
                            .message(AppConstants.ERROR_CRON_KAFKA_NOTIFICATION_FAILED)
                            .detail("tenantId", tenantId)
                            .detail("userCount", tenantUsers.size())
                            .detail("error", e.getMessage())
                            .log();
                    notificationsFailed += tenantUsers.size();
                }
            }

            logger.info()
                .message(AppConstants.INFO_CRON_NOTIFICATION_COMPLETE)
                .detail("notificationsSent", notificationsSent)
                .detail("notificationsFailed", notificationsFailed)
                .log();

        } catch (Exception e) {
            logger.error(e)
                    .message(AppConstants.ERROR_CRON_NOTIFICATION_GENERAL)
                    .detail("error", e.getMessage())
                    .log();
        }
    }

    /**
     * Sends password expiration notifications for users in a specific tenant.
     * 
     * @param tenantId the tenant ID
     * @param users list of users requiring notifications
     * @param currentTime current timestamp
     * @return number of notifications successfully sent
     */
    @Transactional
    private int sendNotificationsForTenant(String tenantId, List<User> users, long currentTime) {
        // Get password policy for this tenant
        Optional<PasswordPolicy> passwordPolicyOpt = passwordPolicyRepository.findByTenantId(tenantId);
        if (passwordPolicyOpt.isEmpty() || passwordPolicyOpt.get().getPolicyExpiresAt() == null || passwordPolicyOpt.get().getPasswordExpirationNotificationDays() == null) {
            logger.warn()
                .message(AppConstants.WARN_CRON_TENANT_NO_POLICY)
                .detail("tenantId", tenantId)
                .log();
            return 0;
        }

        PasswordPolicy passwordPolicy = passwordPolicyOpt.get();

        // Check if emails have already been sent for this policy period
        if (passwordPolicy.getIsEmailSent() != null && passwordPolicy.getIsEmailSent()) {
            logger.info()
                .message(AppConstants.PASSWORD_EXPIRATION_EMAILS_ALREADY_SENT)
                .detail("tenantId", tenantId)
                .detail("policyId", passwordPolicy.getId().toString())
                .log();
            return 0;
        }

        List<NotificationEventData.UserNotificationData> notificationDataList = new ArrayList<>();

        for (User user : users) {
            try {
                // Validate user data
                if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                    logger.warn()
                        .message(AppConstants.WARN_CRON_USER_NO_EMAIL)
                        .detail("userId", user.getId().toString())
                        .log();
                    continue;
                }

                // Calculate days until policy expiration
                long timeUntilExpiration = passwordPolicy.getPolicyExpiresAt() - currentTime;
                int daysUntilExpiration = (int) Math.max(0, Math.ceil(timeUntilExpiration / 86400000.0));

                // Prepare template variables
                NotificationEventData.TemplateVariables templateVariables = new NotificationEventData.TemplateVariables();
                templateVariables.setUsername(user.getUsername() != null ? user.getUsername() : "User");
                templateVariables.setEmail(user.getEmail());
                templateVariables.setExpirationDays(String.valueOf(daysUntilExpiration));

                // Prepare user data
                NotificationEventData.UserData userData = new NotificationEventData.UserData();
                userData.setEmail(user.getEmail());
                userData.setUsername(user.getUsername() != null ? user.getUsername() : "User");

                // Create notification data
                NotificationEventData.UserNotificationData notificationData = new NotificationEventData.UserNotificationData();
                notificationData.setUserId(user.getId().toString());
                notificationData.setUserData(userData);
                notificationData.setTemplateVariables(templateVariables);

                notificationDataList.add(notificationData);

                logger.debug()
                    .message(AppConstants.DEBUG_CRON_USER_NOTIFICATION_PREPARED)
                    .detail("userId", user.getId().toString())
                    .detail("email", user.getEmail())
                    .detail("expirationDays", daysUntilExpiration)
                    .log();

            } catch (Exception e) {
                logger.error(e)
                        .message(AppConstants.ERROR_CRON_NOTIFICATION_FAILED)
                        .detail("userId", user.getId().toString())
                        .detail("email", user.getEmail())
                        .detail("tenantId", tenantId)
                        .detail("error", e.getMessage())
                        .log();
            }
        }

        if (notificationDataList.isEmpty()) {
            return 0;
        }

        try {
            // Send notification event to Kafka
            NotificationEventData eventData = new NotificationEventData();
            // Use PASSWORD_REMINDER_RESET operation type for password expiration notifications
            eventData.setOperationType(NotificationEventData.OperationType.PASSWORD_REMINDER_RESET);
            eventData.setData(notificationDataList);
            eventData.setTenantId(tenantId);

            // Use existing notification producer to send to Kafka
            notificationEventProducer.sendPasswordExpirationNotificationEvent(eventData);

            logger.debug()
                .message(AppConstants.DEBUG_CRON_NOTIFICATION_EVENT_SENT)
                .detail("tenantId", tenantId)
                .detail("userCount", notificationDataList.size())
                .log();

            // Log individual notifications sent
            for (NotificationEventData.UserNotificationData notificationData : notificationDataList) {
                logger.info()
                    .message(AppConstants.INFO_CRON_NOTIFICATION_SENT)
                    .detail("userId", notificationData.getUserId())
                    .detail("email", notificationData.getUserData().getEmail())
                    .detail("tenantId", tenantId)
                    .detail("expirationDays", notificationData.getTemplateVariables().getExpirationDays())
                    .log();
            }

            // Mark emails as sent for this policy period to prevent spam
            passwordPolicyRepository.updateIsEmailSent(passwordPolicy.getId(), true);

            logger.info()
                .message(AppConstants.PASSWORD_POLICY_UPDATED_EMAILS_MARKED_SENT)
                .detail("tenantId", tenantId)
                .detail("policyId", passwordPolicy.getId().toString())
                .log();

            return notificationDataList.size();

        } catch (Exception e) {
            logger.error(e)
                    .message(AppConstants.ERROR_CRON_KAFKA_NOTIFICATION_FAILED)
                    .detail("tenantId", tenantId)
                    .detail("userCount", notificationDataList.size())
                    .detail("error", e.getMessage())
                    .log();
            return 0;
        }
    }

    /**
     * Manually triggers password expiration notification check.
     * Useful for testing or manual execution.
     * 
     * @return number of notifications sent
     */
    public int triggerPasswordExpirationNotifications() {
        logger.info().message(AppConstants.INFO_CRON_MANUAL_NOTIFICATION_TRIGGER).log();
        
        int initialCount = getTotalNotificationsSent();
        checkAndSendPasswordExpirationNotifications();
        return getTotalNotificationsSent() - initialCount;
    }

    /**
     * Gets the count of users currently requiring password expiration notifications.
     * 
     * @return count of users requiring notifications
     */
    public long getUsersRequiringNotificationCount() {
        long currentTime = System.currentTimeMillis();
        return userRepository.countUsersRequiringPasswordExpirationNotification(currentTime);
    }

    /**
     * Gets the list of users currently requiring password expiration notifications.
     * 
     * @param limit maximum number of users to return
     * @return list of users requiring notifications
     */
    public List<User> getUsersRequiringNotification(int limit) {
        long currentTime = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(0, limit);
        return userRepository.findUsersRequiringPasswordExpirationNotification(currentTime, pageable);
    }

    /**
     * Gets the total number of notifications sent.
     * This is a simple counter for demonstration - in a real system you might
     * want to track this in a database or metrics system.
     * 
     * @return total notifications sent
     */
    private int getTotalNotificationsSent() {
        // This is a placeholder implementation
        // In a real system, you'd track this metric properly
        return 0;
    }
}
