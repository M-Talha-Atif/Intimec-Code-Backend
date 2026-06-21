package com.aidentx.authservice.service.Auth;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.exception.InvalidOldPasswordException;
import com.aidentx.authservice.exception.PasswordResetTokenExpiredException;
import com.aidentx.authservice.exception.UserNotFoundException;
import com.aidentx.authservice.model.dto.PasswordResetRequest;
import com.aidentx.authservice.model.dto.ResetPasswordRequest;
import com.aidentx.authservice.model.dto.StatusMessageResponse;
import com.aidentx.authservice.model.dto.UpdatePasswordRequest;
import com.aidentx.authservice.model.entity.PasswordResetToken;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.PasswordResetTokenRepository;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.AuditLogEventService;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Rigorous unit tests for {@link PasswordService}.
 * <p>
 * This class tests all password management functionality, including
 * updating passwords, requesting resets, and finalizing resets with tokens.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    // --- Mocks for Dependencies ---
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private JavaMailSender mailSender;
    @Mock private AuditLogEventService auditLogEventService;
    @Mock private UnifiedLoggerFactory loggerFactory;

    // --- Mocks for Logging ---
    @Mock private IUnifiedLogger logger;
    @Mock private LogEventBuilder logEventBuilder;
    @Mock private MimeMessage mimeMessage; // For mocking email sending

    // --- SUT (System Under Test) ---
    private PasswordService passwordService;

    // --- Test Data ---
    private UUID userId;
    private User user;
    private String userEmail;
    private String oldPassword;
    private String newPassword;

    @BeforeEach
    void setUp() {
        // Mock the logger factory and fluent API
        when(loggerFactory.getLogger(anyString(), anyString())).thenReturn(logger);
        lenient().when(logger.trace()).thenReturn(logEventBuilder);
        lenient().when(logger.info()).thenReturn(logEventBuilder);
        lenient().when(logger.warn()).thenReturn(logEventBuilder);
        lenient().when(logger.error(any(Throwable.class))).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.message(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.detail(anyString(), any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.details(any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectId(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectType(anyString())).thenReturn(logEventBuilder);
        doNothing().when(logEventBuilder).log();

        // Instantiate the PasswordService
        passwordService = new PasswordService(
            loggerFactory,
            userRepository,
            passwordEncoder,
            passwordResetTokenRepository,
            jwtUtil,
            mailSender,
            auditLogEventService
        );

        // Setup common test data
        userId = UUID.randomUUID();
        userEmail = "test@example.com";
        oldPassword = "oldPassword123";
        newPassword = "newPassword456";
        
        user = new User();
        user.setId(userId);
        user.setEmail(userEmail);
        user.setPasswordHash("hashedOldPassword");
        
        setupMockRequest();
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }
    
    private void setupMockRequest() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("JUnit Test Agent");
        lenient().when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Cluster-Client-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Forwarded")).thenReturn(null);
        lenient().when(mockRequest.getHeader("Forwarded-For")).thenReturn(null);
        lenient().when(mockRequest.getHeader("Forwarded")).thenReturn(null);
    }

    // === Tests for updatePassword ===

    @Test
    @DisplayName("Update Password: Should succeed with correct old password")
    void updatePassword_shouldSucceed_withValidCredentials() {
        UpdatePasswordRequest request = new UpdatePasswordRequest(oldPassword, newPassword);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(oldPassword, "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("hashedNewPassword");
        
        StatusMessageResponse response = passwordService.updatePassword(request, userId);
        
        assertEquals(200, response.getStatus());
        assertEquals(AppConstants.PASSWORD_UPDATE_SUCCESS_MSG, response.getMessage());
        verify(userRepository).save(user);
        assertEquals("hashedNewPassword", user.getPasswordHash());
        assertNotNull(user.getPasswordChangedAt());
        verify(auditLogEventService).sendAuditEvent(any());
    }

    @Test
    @DisplayName("Update Password: Should throw UserNotFoundException for non-existent user")
    void updatePassword_shouldThrowException_whenUserNotFound() {
        UpdatePasswordRequest request = new UpdatePasswordRequest(oldPassword, newPassword);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> passwordService.updatePassword(request, userId));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Update Password: Should throw InvalidOldPasswordException for incorrect old password")
    void updatePassword_shouldThrowException_whenOldPasswordIsIncorrect() {
        UpdatePasswordRequest request = new UpdatePasswordRequest("wrongOldPassword", newPassword);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPassword", "hashedOldPassword")).thenReturn(false);

        assertThrows(InvalidOldPasswordException.class, () -> passwordService.updatePassword(request, userId));
        verify(userRepository, never()).save(any(User.class));
        verify(auditLogEventService).sendAuditEvent(any());
    }

    // === Tests for requestPasswordReset ===

    @Test
    @DisplayName("Request Reset: Should send email and save token for existing user")
    void requestPasswordReset_shouldSendEmail_forExistingUser() {
        PasswordResetRequest request = new PasswordResetRequest(userEmail);
        String resetTokenStr = "reset-token";
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(jwtUtil.generatePasswordResetToken(userId)).thenReturn(resetTokenStr);
        
        StatusMessageResponse response = passwordService.requestPasswordReset(request);

        assertEquals(200, response.getStatus());
        assertEquals(AppConstants.PASSWORD_RESET_GENERIC_MSG, response.getMessage());
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(mailSender).send(any(MimeMessage.class));
    }
    
    @Test
    @DisplayName("Request Reset: Should return generic success for non-existent user")
    void requestPasswordReset_shouldReturnGenericSuccess_forNonExistentUser() {
        PasswordResetRequest request = new PasswordResetRequest("nouser@example.com");
        when(userRepository.findByEmail("nouser@example.com")).thenReturn(Optional.empty());

        StatusMessageResponse response = passwordService.requestPasswordReset(request);

        assertEquals(200, response.getStatus());
        assertEquals(AppConstants.PASSWORD_RESET_GENERIC_MSG, response.getMessage());
        verify(jwtUtil, never()).generatePasswordResetToken(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
    
    // === Tests for resetPassword ===

    @Test
    @DisplayName("Reset Password: Should succeed with a valid, unexpired token")
    void resetPassword_shouldSucceed_withValidToken() {
        String token = "valid-token";
        ResetPasswordRequest request = new ResetPasswordRequest(newPassword);
        PasswordResetToken resetToken = new PasswordResetToken(user, token, Instant.now().plusSeconds(3600));
        
        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode(newPassword)).thenReturn("hashedNewPassword");

        StatusMessageResponse response = passwordService.resetPassword(token, request);

        assertEquals(200, response.getStatus());
        assertEquals(AppConstants.PASSWORD_RESET_SUCCESS_MSG, response.getMessage());
        verify(userRepository).save(user);
        assertEquals("hashedNewPassword", user.getPasswordHash());
        verify(passwordResetTokenRepository).delete(resetToken);
        verify(auditLogEventService).sendAuditEvent(any());
    }
    
    @Test
    @DisplayName("Reset Password: Should throw exception for a non-existent token")
    void resetPassword_shouldThrowException_forNonExistentToken() {
        String token = "invalid-token";
        ResetPasswordRequest request = new ResetPasswordRequest(newPassword);
        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThrows(PasswordResetTokenExpiredException.class, () -> passwordService.resetPassword(token, request));
        verify(userRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Reset Password: Should throw exception for an expired token")
    void resetPassword_shouldThrowException_forExpiredToken() {
        String token = "expired-token";
        ResetPasswordRequest request = new ResetPasswordRequest(newPassword);
        PasswordResetToken expiredToken = new PasswordResetToken(user, token, Instant.now().minusSeconds(1));
        
        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(expiredToken));
        
        assertThrows(PasswordResetTokenExpiredException.class, () -> passwordService.resetPassword(token, request));
        verify(passwordResetTokenRepository).delete(expiredToken); // Verify the expired token is cleaned up
        verify(userRepository, never()).save(any());
    }
}