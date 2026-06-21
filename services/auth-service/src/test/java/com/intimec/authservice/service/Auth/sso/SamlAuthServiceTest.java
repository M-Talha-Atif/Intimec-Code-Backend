package com.aidentx.authservice.service.Auth.sso;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.config.SamlConfig;
import com.aidentx.authservice.exception.SamlRedirectException;
import com.aidentx.authservice.exception.UserNotFoundException;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.AuditLogEventService;
import com.aidentx.authservice.service.RefreshTokenService;
import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.authservice.service.UserAuthenticationMethodService;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;

/**
 * Rigorous unit tests for {@link SamlAuthService}.
 * <p>
 * This class tests the SAML login flow by mocking the SsoConfigurationManager,
 * the SAML Authentication object, and all service dependencies. It verifies
 * successful token generation and correct exception handling for various failure scenarios.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class SamlAuthServiceTest {

    // --- Mocks for Dependencies ---
    @Mock private SsoConfigurationManager ssoConfigManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserAuthenticationMethodService userAuthMethodService;
    @Mock private AuditLogEventService auditLogEventService;
    @Mock private UnifiedLoggerFactory loggerFactory;

    // --- Mocks for Logging ---
    @Mock private IUnifiedLogger logger;
    @Mock private LogEventBuilder logEventBuilder;
    
    // --- Mock for Spring Security Principal ---
    @Mock private Authentication authentication;
    @Mock private Saml2AuthenticatedPrincipal samlPrincipal;

    // --- SUT (System Under Test) ---
    private SamlAuthService samlAuthService;

    // --- Test Data ---
    private SamlConfig enabledSamlConfig;
    private User user;
    private UUID userId;
    private String userEmail;

    @BeforeEach
    void setUp() {
        // Mock the logger factory and fluent API
        when(loggerFactory.getLogger(anyString(), anyString())).thenReturn(logger);
        lenient().when(logger.trace()).thenReturn(logEventBuilder);
        lenient().when(logger.info()).thenReturn(logEventBuilder);
        lenient().when(logger.debug()).thenReturn(logEventBuilder);
        lenient().when(logger.warn()).thenReturn(logEventBuilder);
        lenient().when(logger.error()).thenReturn(logEventBuilder);
        lenient().when(logger.error(any(Throwable.class))).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.message(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectType(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectId(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectId(any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.detail(anyString(), any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.details(anyMap())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.failureReason(anyString())).thenReturn(logEventBuilder);
        doNothing().when(logEventBuilder).log();

        // Instantiate the SamlAuthService
        samlAuthService = new SamlAuthService(
            loggerFactory,
            ssoConfigManager,
            userRepository,
            jwtUtil,
            refreshTokenService,
            userAuthMethodService,
            auditLogEventService
        );

        // Use ReflectionTestUtils to set @Value fields
        ReflectionTestUtils.setField(samlAuthService, "frontUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(samlAuthService, "frontUrl_callback", "/sso-callback");

        // Setup common test data
        enabledSamlConfig = new SamlConfig();
        enabledSamlConfig.setEnabled(true);
        enabledSamlConfig.setName("TestSAML");
        enabledSamlConfig.setEmailAttribute("email"); // Configure a specific attribute for testing
        
        userId = UUID.randomUUID();
        userEmail = "saml.user@example.com";
        user = new User();
        user.setId(userId);
        user.setUsername("saml.user");
        user.setEmail(userEmail);

        setupMockRequest();
    }
    
    private void setupMockRequest() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn("JUnit Test Agent");
        lenient().when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Forwarded-Proto")).thenReturn(null);
        lenient().when(mockRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        lenient().when(mockRequest.getHeader("HTTP_X_FORWARDED")).thenReturn(null);
        lenient().when(mockRequest.getHeader("HTTP_X_CLUSTER_CLIENT_IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("HTTP_FORWARDED_FOR")).thenReturn(null);
        lenient().when(mockRequest.getHeader("HTTP_FORWARDED")).thenReturn(null);
        lenient().when(mockRequest.getHeader("HTTP_VIA")).thenReturn(null);
        lenient().when(mockRequest.getHeader("REMOTE_ADDR")).thenReturn(null);
    }

    @Test
    @DisplayName("SAML Response: Should redirect with tokens on successful authentication")
    void handleSamlResponse_shouldRedirectWithTokens_onSuccess() {
        // Arrange
        when(ssoConfigManager.getSamlConfig()).thenReturn(enabledSamlConfig);
        when(authentication.getPrincipal()).thenReturn(samlPrincipal);
        when(samlPrincipal.getFirstAttribute("email")).thenReturn(userEmail);
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(userId)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(userId)).thenReturn("new-refresh-token");

        // Act & Assert
        SamlRedirectException exception = assertThrows(SamlRedirectException.class,
                () -> samlAuthService.handleSamlResponse(authentication));
        
        // Verify the redirect URL contains the tokens
        assertTrue(exception.getRedirectUrl().contains("token=new-access-token"));
        assertTrue(exception.getRedirectUrl().contains("refresh=new-refresh-token"));
        assertNull(exception.getErrorMessage()); // No error message on success

        // Verify interactions
        verify(refreshTokenService).createRefreshToken(eq(user), eq("new-refresh-token"));
        verify(userAuthMethodService).trackUserMethod(eq(user), eq(com.aidentx.authservice.enums.AuthenticationMethod.SAML));
        verify(auditLogEventService).sendAuditEvent(any());
    }

    @Test
    @DisplayName("SAML Response: Should throw UserNotFoundException if email cannot be extracted")
    void handleSamlResponse_shouldThrowException_whenEmailNotExtractable() {
        // Arrange
        when(ssoConfigManager.getSamlConfig()).thenReturn(enabledSamlConfig);
        when(authentication.getPrincipal()).thenReturn(samlPrincipal);
        // Simulate no 'email' attribute in the SAML response
        when(samlPrincipal.getFirstAttribute("email")).thenReturn(null);

        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> samlAuthService.handleSamlResponse(authentication));
        
        assertEquals(AppConstants.SAML_AUTH_FAILED_EMAIL_EXTRACTION, exception.getMessage());
        verify(auditLogEventService).sendAuditEvent(any());
        verifyNoInteractions(userRepository, jwtUtil);
    }
    
    @Test
    @DisplayName("SAML Response: Should throw UserNotFoundException if user is not in local DB")
    void handleSamlResponse_shouldThrowException_whenUserNotInDb() {
        // Arrange
        when(ssoConfigManager.getSamlConfig()).thenReturn(enabledSamlConfig);
        when(authentication.getPrincipal()).thenReturn(samlPrincipal);
        when(samlPrincipal.getFirstAttribute("email")).thenReturn(userEmail);
        // Simulate user not found in our database
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());
        
        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> samlAuthService.handleSamlResponse(authentication));

        assertTrue(exception.getMessage().contains(userEmail));
        verify(auditLogEventService).sendAuditEvent(any());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("SAML Callback: Should throw SamlRedirectException with error message on failure")
    void handleSamlCallback_shouldRedirectWithError_onFailure() {
        // Arrange
        String errorMessage = "Invalid signature";
        
        // Act & Assert
        SamlRedirectException exception = assertThrows(SamlRedirectException.class,
                () -> samlAuthService.handleSamlCallback(false, true, errorMessage, null));

        assertTrue(exception.getRedirectUrl().contains("error="));
        assertNotNull(exception.getErrorMessage());
    }
    
    @Test
    @DisplayName("SAML Callback: Should throw SamlRedirectException if user not found")
    void handleSamlCallback_shouldRedirectWithError_whenUserNotFound() {
        // Arrange
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        SamlRedirectException exception = assertThrows(SamlRedirectException.class,
                () -> samlAuthService.handleSamlCallback(true, false, null, authentication));
        
        assertEquals(AppConstants.WRONG_USERNAME_OR_PASSWORD, exception.getErrorMessage());
        verify(auditLogEventService).sendAuditEvent(any());
    }
}