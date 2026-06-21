package com.aidentx.authservice.service.Auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.audit.AuditLogEventBuilder;
import com.aidentx.authservice.service.AuditLogEventService;
import com.aidentx.authservice.service.NotificationEventProducer;
import com.aidentx.authservice.exception.InvalidOldPasswordException;
import com.aidentx.authservice.exception.PasswordResetTokenExpiredException;
import com.aidentx.authservice.exception.PasswordValidationException;
import com.aidentx.authservice.exception.UserNotFoundException;
import com.aidentx.authservice.model.dto.PasswordResetRequest;
import com.aidentx.authservice.model.dto.ResetPasswordRequest;
import com.aidentx.authservice.model.dto.StatusMessageResponse;
import com.aidentx.authservice.model.dto.UpdatePasswordRequest;
import com.aidentx.authservice.model.entity.PasswordPolicy;
import com.aidentx.authservice.model.entity.PasswordResetToken;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.PasswordPolicyRepository;
import com.aidentx.authservice.repository.PasswordResetTokenRepository;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.authservice.utils.RequestUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordService {

    private final IUnifiedLogger logger;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordPolicyRepository passwordPolicyRepository;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;
    private final AuditLogEventService auditLogEventService;
    private final NotificationEventProducer notificationEventProducer;
    private final PasswordHistoryService passwordHistoryService;
    private final PasswordValidationService passwordValidationService;

    @Value("${front.url}")
    private String frontUrl;

    public PasswordService(UnifiedLoggerFactory unifiedLoggerFactory, UserRepository userRepository,
            PasswordEncoder passwordEncoder, PasswordResetTokenRepository passwordResetTokenRepository, 
            PasswordPolicyRepository passwordPolicyRepository, JwtUtil jwtUtil,
            JavaMailSender mailSender, AuditLogEventService auditLogEventService, 
            NotificationEventProducer notificationEventProducer, PasswordHistoryService passwordHistoryService,
            PasswordValidationService passwordValidationService) {
        this.logger = unifiedLoggerFactory.getLogger("PasswordService", AppConstants.LOG_CATEGORY_USER_MGMT);
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordPolicyRepository = passwordPolicyRepository;
        this.jwtUtil = jwtUtil;
        this.mailSender = mailSender;
        this.auditLogEventService = auditLogEventService;
        this.notificationEventProducer = notificationEventProducer;
        this.passwordHistoryService = passwordHistoryService;
        this.passwordValidationService = passwordValidationService;
    }

        /**
     * Updates the password for the authenticated user.
     * Validates the old password and applies password policy validation to the new password.
     * <p>
     * This method:
     * - Verifies the old password is correct
     * - Validates the new password against tenant-specific password policies
     * - Updates the user's password hash
     * - Records the change in password history
     * - Logs the change for audit purposes
     * </p>
     *
     * @param request the update password request containing old and new passwords
     * @param userId  the ID of the authenticated user (from JWT)
     * @return a status/message response indicating success
     * @throws PasswordValidationException if the new password fails policy validation
     */
    @Transactional
    public StatusMessageResponse updatePassword(UpdatePasswordRequest request, UUID userId) {
        logger.trace().message(String.format(AppConstants.TRACE_UPDATE_PASSWORD_METHOD, userId))
                .detail("userId", userId.toString()).log();
        HttpServletRequest req = getCurrentHttpRequest();
        String ip = RequestUtils.getClientIP(req);
        String userAgent = RequestUtils.getUserAgent(req);

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(String.format(AppConstants.USER_NOT_FOUND, userId)));
            
            // First validate the new password against policy
            passwordValidationService.validatePassword(request.getNewPassword(), user);
            
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode customMetadata = objectMapper.createObjectNode();
                customMetadata.put("username", user.getUsername());
                customMetadata.put("email", user.getEmail());
                customMetadata.put("type", "user");
                customMetadata.put("reason", "invalid_old_password");
                auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(user.getId(),
                        AppConstants.USER_EVENT_TYPE, user.getUsername(), user.getTenantId(), user.getId(), AppConstants.USER_EVENT_TYPE,
                        user.getUsername(), AppConstants.PASSWORD_UPDATE_EVENT, AppConstants.FAILURE_EVENT_TYPE,
                        AppConstants.EVENT_RISK_MEDIUM, ip, userAgent,
                        String.format(AppConstants.INVALID_OLD_PASSWORD, user.getEmail()), null, null, null,
                        customMetadata));
                logger.warn().objectType("user").objectId(user.getEmail())
                        .message(String.format(AppConstants.INVALID_OLD_PASSWORD, user.getEmail()))
                        .details(
                                Map.of("operation", AppConstants.OP_UPDATE_PASSWORD, "userId", user.getId().toString(),
                                        "clientIP", ip, "userAgent", userAgent))
                        .log();
                throw new InvalidOldPasswordException(AppConstants.INVALID_OLD_PASSWORD_MSG);
            }
            String hashedNewPassword = passwordEncoder.encode(request.getNewPassword());
            user.setPasswordHash(hashedNewPassword);
            user.setPasswordChangedAt(java.time.Instant.now());
            userRepository.save(user);

            // Save the new password to history for future password policy enforcement
            passwordHistoryService.savePasswordToHistory(user.getId(), hashedNewPassword, user.getTenantId());
            
            // NOTE: Cleanup disabled - keeping all password history for audit purposes
            // Password validation will only check against recent passwords based on policy

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode stateAfter = objectMapper.createObjectNode();
            stateAfter.put("passwordChangedAt", user.getPasswordChangedAt().toString());
            ObjectNode customMetadata = objectMapper.createObjectNode();
            customMetadata.put("username", user.getUsername());
            customMetadata.put("email", user.getEmail());
            customMetadata.put("type", "user");
            auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(user.getId(),
                    AppConstants.USER_EVENT_TYPE, user.getUsername(), user.getTenantId(), user.getId(), AppConstants.USER_EVENT_TYPE,
                    user.getUsername(), AppConstants.PASSWORD_UPDATE_EVENT, AppConstants.SUCCESS_EVENT_TYPE,
                    AppConstants.EVENT_RISK_LOW, ip, userAgent,
                    String.format(AppConstants.PASSWORD_UPDATE_SUCCESS, user.getEmail()), null, stateAfter, null,
                    customMetadata));
            logger.info().objectType("user").objectId(user.getEmail())
                    .message(String.format(AppConstants.PASSWORD_UPDATE_SUCCESS, user.getEmail()))
                    .details(Map.of("operation", AppConstants.OP_UPDATE_PASSWORD, "userId", user.getId().toString(),
                            "clientIP", ip, "userAgent", userAgent))
                    .log();
            return new StatusMessageResponse(200, AppConstants.PASSWORD_UPDATE_SUCCESS_MSG);
        } catch (UserNotFoundException | InvalidOldPasswordException | PasswordValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType("user").objectId(userId.toString())
                    .message(String.format(AppConstants.PASSWORD_UPDATE_ERROR, userId))
                    .detail("operation", AppConstants.OP_UPDATE_PASSWORD).log();
            throw new RuntimeException(AppConstants.PASSWORD_UPDATE_ERROR_MSG);
        }
    }

    /**
     * Initiates the password reset process for the given email.
     * If the user exists, generates a password reset token and sends an email with
     * a reset link.
     * Always returns a generic success message for security.
     *
     * @param request the password reset request containing the user's email
     * @return a status message response
     */
    @Transactional
    public StatusMessageResponse requestPasswordReset(PasswordResetRequest request) {
        String email = request.getEmail();
        logger.trace().message(String.format(AppConstants.TRACE_PASSWORD_RESET_REQUEST, email)).detail("email", email)
                .log();
        String genericMsg = AppConstants.PASSWORD_RESET_GENERIC_MSG;
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                logger.warn().objectType("user").objectId(email)
                        .message(String.format(AppConstants.PASSWORD_RESET_USER_NOT_FOUND, email))
                        .detail("operation", AppConstants.OP_REQUEST_PASSWORD_RESET).log();
                return new StatusMessageResponse(200, genericMsg);
            }
            String token = jwtUtil.generatePasswordResetToken(user.getId());
            Instant expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
            Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUser(user);
            if (existingToken.isPresent()) {
                PasswordResetToken resetToken = existingToken.get();
                resetToken.setToken(token);
                resetToken.setExpiresAt(expiresAt);
                passwordResetTokenRepository.save(resetToken);
                logger.info().objectType("user").objectId(user.getEmail())
                        .message(String.format(AppConstants.PASSWORD_RESET_TOKEN_UPDATED, user.getEmail()))
                        .detail("operation", AppConstants.OP_REQUEST_PASSWORD_RESET)
                        .detail("userId", user.getId().toString()).log();
            } else {
                PasswordResetToken resetToken = new PasswordResetToken(user, token, expiresAt);
                passwordResetTokenRepository.save(resetToken);
                logger.info().objectType("user").objectId(user.getEmail())
                        .message(String.format(AppConstants.PASSWORD_RESET_TOKEN_CREATED, user.getEmail()))
                        .detail("operation", AppConstants.OP_REQUEST_PASSWORD_RESET)
                        .detail("userId", user.getId().toString()).log();
            }
            sendPasswordResetEmail(user.getEmail(), token);
            logger.info().objectType("user").objectId(user.getEmail())
                    .message(String.format(AppConstants.PASSWORD_RESET_EMAIL_SENT, user.getEmail()))
                    .detail("operation", AppConstants.OP_REQUEST_PASSWORD_RESET)
                    .detail("userId", user.getId().toString()).log();
            return new StatusMessageResponse(200, genericMsg);
        } catch (Exception ex) {
            logger.error(ex).objectType("user").objectId(email)
                    .message(String.format(AppConstants.PASSWORD_RESET_UNEXPECTED_ERROR, email))
                    .detail("operation", AppConstants.OP_REQUEST_PASSWORD_RESET).log();
            return new StatusMessageResponse(500, AppConstants.GENERIC_ERROR_MESSAGE);
        }
    }

    /**
     * Resets the password for a user using the provided token.
     * <p>
     * This method validates the password reset token, checks its expiry, and
     * updates
     * the user's password if the token is valid. The token is then invalidated.
     * </p>
     *
     * @param token   the password reset token
     * @param request the request containing the new password
     * @return a status message response indicating success or failure
     */
    @Transactional
    public StatusMessageResponse resetPassword(String token, ResetPasswordRequest request) {
        logger.trace().message(String.format(AppConstants.TRACE_RESET_PASSWORD, token))
                .detail("tokenPrefix", token.substring(0, Math.min(token.length(), 10))).log();
        HttpServletRequest req = getCurrentHttpRequest();
        String ip = RequestUtils.getClientIP(req);
        String userAgent = RequestUtils.getUserAgent(req);
        try {
            PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token).orElseThrow(() -> {
                logger.warn().objectType("token").objectId(token)
                        .message(String.format(AppConstants.PASSWORD_RESET_TOKEN_NOT_FOUND, token))
                        .detail("operation", AppConstants.OP_RESET_PASSWORD).log();
                return new PasswordResetTokenExpiredException(AppConstants.PASSWORD_RESET_LINK_EXPIRED_MSG);
            });
            if (resetToken.getExpiresAt().isBefore(Instant.now())) {
                logger.warn().objectType("token").objectId(token)
                        .message(String.format(AppConstants.PASSWORD_RESET_TOKEN_EXPIRED, token))
                        .detail("operation", AppConstants.OP_RESET_PASSWORD).log();
                passwordResetTokenRepository.delete(resetToken);
                throw new PasswordResetTokenExpiredException(AppConstants.PASSWORD_RESET_LINK_EXPIRED_MSG);
            }
            User user = resetToken.getUser();
            
            // Validate the new password against tenant-specific password policy
            passwordValidationService.validatePassword(request.getNewPassword(), user);
            
            String hashedNewPassword = passwordEncoder.encode(request.getNewPassword());
            user.setPasswordHash(hashedNewPassword);
            user.setPasswordChangedAt(Instant.now());
            user.setIsPasswordExpired(false);
            userRepository.save(user);
            passwordResetTokenRepository.delete(resetToken);

            // Save the new password to history for future password policy enforcement
            passwordHistoryService.savePasswordToHistory(user.getId(), hashedNewPassword, user.getTenantId());
            
            // NOTE: Cleanup disabled - keeping all password history for audit purposes
            // Password validation will only check against recent passwords based on policy

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode stateAfter = objectMapper.createObjectNode();
            stateAfter.put("passwordChangedAt", user.getPasswordChangedAt().toString());
            ObjectNode customMetadata = objectMapper.createObjectNode();
            customMetadata.put("username", user.getUsername());
            customMetadata.put("email", user.getEmail());
            customMetadata.put("type", "user");
            customMetadata.put("resetMethod", "token");
            auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(user.getId(),
                    AppConstants.USER_EVENT_TYPE, user.getUsername(), user.getTenantId(), user.getId(), AppConstants.USER_EVENT_TYPE,
                    user.getUsername(), AppConstants.PASSWORD_RESET_EVENT, AppConstants.SUCCESS_EVENT_TYPE,
                    AppConstants.EVENT_RISK_LOW, ip, userAgent,
                    String.format(AppConstants.PASSWORD_RESET_SUCCESS, user.getEmail()), null, stateAfter, null,
                    customMetadata));
            logger.info().objectType("user").objectId(user.getEmail())
                    .message(String.format(AppConstants.PASSWORD_RESET_SUCCESS, user.getEmail()))
                    .details(Map.of("operation", AppConstants.OP_RESET_PASSWORD, "userId", user.getId().toString(),
                            "clientIP", ip, "userAgent", userAgent))
                    .log();
            
            // Send password reset notification via Kafka
            notificationEventProducer.sendPasswordResetNotification(user);
            
            return new StatusMessageResponse(200, AppConstants.PASSWORD_RESET_SUCCESS_MSG);
        } catch (PasswordResetTokenExpiredException | PasswordValidationException ex) {
            logger.warn().message(ex.getMessage()).detail("operation", AppConstants.OP_RESET_PASSWORD).log();
            throw ex;
        } catch (Exception ex) {
            logger.error(ex).objectType("token").objectId(token)
                    .message(String.format(AppConstants.PASSWORD_RESET_UNEXPECTED_ERROR, token))
                    .detail("operation", AppConstants.OP_RESET_PASSWORD).log();
            throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, ex);
        }
    }

    /**
     * Sends a password reset email with a reset link containing the token.
     *
     * @param toEmail the recipient's email
     * @param token   the password reset token
     */
    private void sendPasswordResetEmail(String toEmail, String token) {
        String resetUrl = frontUrl + "/reset_password/" + token;
        String subject = AppConstants.PASSWORD_RESET_EMAIL_SUBJECT;
        String content = String.format(AppConstants.PASSWORD_RESET_EMAIL_CONTENT, resetUrl);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            logger.error(e).category(AppConstants.LOG_CATEGORY_NOTIFICATION).objectType("email").objectId(toEmail)
                    .message(String.format(AppConstants.PASSWORD_RESET_EMAIL_FAILED, toEmail))
                    .detail("operation", AppConstants.OP_SEND_PASSWORD_RESET_EMAIL).log();
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}