package com.aidentx.authservice.service.Auth.sso;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.config.OAuth2Config;
import com.aidentx.authservice.exception.OAuth2RedirectException;
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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
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
 * Rigorous unit tests for {@link OAuth2AuthService}.
 * <p>
 * This class tests the OAuth2 login flow, focusing on the callback handling.
 * It mocks the OAuth2AuthenticationToken and service dependencies to verify
 * correct token generation and error handling.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OAuth2AuthServiceTest {

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
    
    // --- Mocks for Spring Security Principal ---
    @Mock private OAuth2AuthenticationToken authentication;
    @Mock private OAuth2User oAuth2User;

    // --- SUT (System Under Test) ---
    private OAuth2AuthService oAuth2AuthService;

    // --- Test Data ---
    private OAuth2Config enabledOAuthConfig;
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
        
        // Instantiate the OAuth2AuthService
        oAuth2AuthService = new OAuth2AuthService(
            loggerFactory,
            ssoConfigManager,
            userRepository,
            jwtUtil,
            refreshTokenService,
            userAuthMethodService,
            auditLogEventService
        );

        // Use ReflectionTestUtils to set @Value fields
        ReflectionTestUtils.setField(oAuth2AuthService, "frontUrl", "http://localhost:3000");
        ReflectionTestUtils.setField(oAuth2AuthService, "frontUrl_callback", "/sso-callback");

        // Setup common test data
        enabledOAuthConfig = new OAuth2Config();
        enabledOAuthConfig.setEnabled(true);
        enabledOAuthConfig.setName("TestOAuth2");
        enabledOAuthConfig.setEmailAttributeMapping("email");
        
        userId = UUID.randomUUID();
        userEmail = "oauth2.user@example.com";
        user = new User();
        user.setId(userId);
        user.setUsername("oauth2.user");
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
    @DisplayName("OAuth2 Callback: Should redirect with tokens on successful authentication")
    void handleOAuth2CallbackGet_shouldRedirectWithTokens_onSuccess() {
        // Arrange
        when(ssoConfigManager.getOAuth2Config()).thenReturn(enabledOAuthConfig);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", userEmail));
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(userId)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(userId)).thenReturn("new-refresh-token");
        
        // Act & Assert
        OAuth2RedirectException exception = assertThrows(OAuth2RedirectException.class,
                () -> oAuth2AuthService.handleOAuth2CallbackGet(null, null, null, null, true, null, authentication));
        
        assertTrue(exception.getRedirectUrl().contains("token=new-access-token"));
        assertTrue(exception.getRedirectUrl().contains("refresh=new-refresh-token"));
        assertNull(exception.getMessage());

        verify(refreshTokenService).createRefreshToken(eq(user), eq("new-refresh-token"));
        verify(userAuthMethodService).trackUserMethod(eq(user), eq(com.aidentx.authservice.enums.AuthenticationMethod.OAUTH2));
        verify(auditLogEventService).sendAuditEvent(any());
    }

    @Test
    @DisplayName("OAuth2 Callback: Should redirect with error if authentication fails")
    void handleOAuth2CallbackGet_shouldRedirectWithError_onAuthFailure() {
        // Arrange
        String error = "access_denied";
        String errorDescription = "User denied access";
        
        // Act & Assert
        OAuth2RedirectException exception = assertThrows(OAuth2RedirectException.class,
                () -> oAuth2AuthService.handleOAuth2CallbackGet(null, null, error, errorDescription, null, null, null));
        
        assertEquals(errorDescription, exception.getMessage());
        assertTrue(exception.getRedirectUrl().contains("error=User+denied+access"));
    }

    @Test
    @DisplayName("OAuth2 Callback: Should redirect with error if email not in principal")
    void handleOAuth2CallbackGet_shouldRedirectWithError_whenEmailNotInPrincipal() {
        // Arrange
        when(ssoConfigManager.getOAuth2Config()).thenReturn(enabledOAuthConfig);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        // Email attribute is missing
        when(oAuth2User.getAttributes()).thenReturn(Map.of("name", "Some User"));

        // Act & Assert
        OAuth2RedirectException exception = assertThrows(OAuth2RedirectException.class,
                () -> oAuth2AuthService.handleOAuth2CallbackGet(null, null, null, null, true, null, authentication));
        
        assertEquals(AppConstants.OAUTH2_AUTH_FAILED_EMAIL_EXTRACTION, exception.getMessage());
        verify(auditLogEventService).sendAuditEvent(any());
        verifyNoInteractions(userRepository, jwtUtil);
    }
    
    @Test
    @DisplayName("OAuth2 Callback: Should redirect with error if user not in local DB")
    void handleOAuth2CallbackGet_shouldRedirectWithError_whenUserNotInDb() {
        // Arrange
        when(ssoConfigManager.getOAuth2Config()).thenReturn(enabledOAuthConfig);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttributes()).thenReturn(Map.of("email", userEmail));
        // User not found in local DB
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());
        
        // Act & Assert
        OAuth2RedirectException exception = assertThrows(OAuth2RedirectException.class,
                () -> oAuth2AuthService.handleOAuth2CallbackGet(null, null, null, null, true, null, authentication));
        
        assertEquals(AppConstants.WRONG_USERNAME_OR_PASSWORD, exception.getMessage());
        verify(auditLogEventService).sendAuditEvent(any());
        verifyNoInteractions(jwtUtil);
    }
}