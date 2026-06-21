package com.aidentx.authservice.service.Auth;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.exception.AccountLockedException;
import com.aidentx.authservice.exception.InvalidCredentialsException;
import com.aidentx.authservice.exception.UserNotFoundException;
import com.aidentx.authservice.model.dto.LoginRequest;
import com.aidentx.authservice.model.dto.LoginSuccessResponse;
import com.aidentx.authservice.model.dto.TokenResponse;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.AuditLogEventService;
import com.aidentx.authservice.service.RefreshTokenService;
import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.authservice.service.Auth.sso.LdapAuthService;
import com.aidentx.authservice.service.Auth.sso.OAuth2AuthService;
import com.aidentx.authservice.service.Auth.sso.SamlAuthService;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
 * Rigorous unit tests for the refactored {@link AuthServiceImpl}.
 * <p>
 * This class uses Mockito to mock dependencies and verify the behavior of the
 * core database login functionality within AuthServiceImpl. It uses ArgumentCaptor
 * to inspect the details of interactions with services like auditing.
 * </p>
 */
@ExtendWith(MockitoExtension.class) // Initializes mocks automatically
class AuthServiceImplTest {

    // --- Mocks for Core Dependencies ---
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditLogEventService auditLogEventService;
    @Mock private UnifiedLoggerFactory loggerFactory;

    // --- Mocks for Delegated Services ---
    @Mock private PasswordService passwordService;
    @Mock private TokenService tokenService;
    @Mock private LdapAuthService ldapAuthService;
    @Mock private SamlAuthService samlAuthService;
    @Mock private OAuth2AuthService oAuth2AuthService;
    @Mock private SsoConfigurationManager ssoConfigManager;

    // --- Mocks for Logging ---
    @Mock private IUnifiedLogger logger;
    @Mock private LogEventBuilder logEventBuilder;
    
    @Captor private ArgumentCaptor<AuditLogKafkaEvent> auditEventCaptor;

    // --- SUT (System Under Test) ---
    private AuthServiceImpl authService;

    // --- Test Data ---
    private String username;
    private String password;
    private UUID userId;
    private User user;
    private String accessToken;
    private String refreshTokenStr;
    private long expiration;
    private static final String MOCK_IP_ADDRESS = "127.0.0.1";
    private static final String MOCK_USER_AGENT = "JUnit Test Agent";


    @BeforeEach
    void setUp() {
        // MockitoExtension handles @Mock annotations, so openMocks is not needed.
        
        // Mock the logger factory to return our mocked logger
        when(loggerFactory.getLogger(anyString(), anyString())).thenReturn(logger);
        
        // Mock logger fluent API to prevent NullPointerExceptions during tests
        lenient().when(logger.trace()).thenReturn(logEventBuilder);
        lenient().when(logger.info()).thenReturn(logEventBuilder);
        lenient().when(logger.debug()).thenReturn(logEventBuilder);
        lenient().when(logger.warn()).thenReturn(logEventBuilder);
        lenient().when(logger.error(any(Throwable.class))).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.message(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.detail(anyString(), any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.details(any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectId(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectType(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.failureReason(anyString())).thenReturn(logEventBuilder);
        doNothing().when(logEventBuilder).log();

        // Instantiate the refactored AuthServiceImpl
        authService = new AuthServiceImpl(
            userRepository,
            passwordEncoder,
            jwtUtil,
            refreshTokenService,
            auditLogEventService,
            ssoConfigManager,
            loggerFactory,
            ldapAuthService,
            samlAuthService,
            oAuth2AuthService,
            passwordService,
            tokenService
        );
        
        // Setup common test data
        username = "testUser";
        password = "testPass";
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash(password);
        accessToken = "dummy-jwt-token";
        refreshTokenStr = "dummy-refresh-token";
        expiration = 3600L;
    }

    private void setupMockRequest() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
        lenient().when(mockRequest.getRemoteAddr()).thenReturn(MOCK_IP_ADDRESS);
        lenient().when(mockRequest.getHeader("User-Agent")).thenReturn(MOCK_USER_AGENT);
        lenient().when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Cluster-Client-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("X-Forwarded")).thenReturn(null);
        lenient().when(mockRequest.getHeader("Forwarded-For")).thenReturn(null);
        lenient().when(mockRequest.getHeader("Forwarded")).thenReturn(null);
    }

    @Test
    @DisplayName("Login: Should throw UserNotFoundException and log failure audit event")
    void login_shouldThrowUserNotFoundException_andLogAudit_whenUserNotFound() {
        setupMockRequest();
        LoginRequest loginRequest = new LoginRequest(username, password);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> authService.login(loginRequest));
        assertTrue(exception.getMessage().contains(username));
        
        // Verify rigorous audit event details
        verify(auditLogEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditLogKafkaEvent capturedEvent = auditEventCaptor.getValue();
        assertEquals(AppConstants.FAILURE_EVENT_TYPE, capturedEvent.getStatus());
        assertEquals(AppConstants.LOGIN_EVENT, capturedEvent.getEvent());
        assertEquals(loginRequest.getIdentifier(), capturedEvent.getActorName());
        assertNull(capturedEvent.getActorId()); // Actor ID is null because user was not found
        
        // Verify no further interactions happened
        verifyNoMoreInteractions(passwordEncoder, jwtUtil, refreshTokenService);
    }

    @Test
    @DisplayName("Login: Should throw AccountLockedException and log locked audit event")
    void login_shouldThrowAccountLockedException_andLogAudit_whenAccountIsLocked() {
        setupMockRequest();
        LoginRequest loginRequest = new LoginRequest(username, password);
        user.setLockedUntil(Instant.now().plusSeconds(3600));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act & Assert
        AccountLockedException exception = assertThrows(AccountLockedException.class, () -> authService.login(loginRequest));
        assertTrue(exception.getMessage().contains(user.getLockedUntil().toString()));
        
        // Verify rigorous audit event details
        verify(auditLogEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditLogKafkaEvent capturedEvent = auditEventCaptor.getValue();
        assertEquals(AppConstants.FAILURE_EVENT_TYPE, capturedEvent.getStatus());
        assertEquals(AppConstants.LOGIN_EVENT, capturedEvent.getEvent());
        assertEquals(user.getId(), capturedEvent.getActorId());
        assertEquals(AppConstants.ACCOUNT_LOCKED_TITLE, capturedEvent.getErrorDetails());
        
        // Verify no further interactions
        verifyNoMoreInteractions(passwordEncoder, jwtUtil, refreshTokenService);
    }

    @Test
    @DisplayName("Login: Should throw InvalidCredentialsException and log failure audit event")
    void login_shouldThrowInvalidCredentialsException_andLogAudit_whenPasswordIsIncorrect() {
        setupMockRequest();
        LoginRequest loginRequest = new LoginRequest(username, "wrongPassword");
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", user.getPasswordHash())).thenReturn(false);

        // Act & Assert
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
        assertEquals(AppConstants.WRONG_USERNAME_OR_PASSWORD, exception.getMessage());
        
        // Verify rigorous audit event details
        verify(auditLogEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditLogKafkaEvent capturedEvent = auditEventCaptor.getValue();
        assertEquals(AppConstants.FAILURE_EVENT_TYPE, capturedEvent.getStatus());
        assertEquals(AppConstants.LOGIN_EVENT, capturedEvent.getEvent());
        assertEquals(user.getId(), capturedEvent.getActorId());
        assertEquals(AppConstants.WRONG_USERNAME_OR_PASSWORD, capturedEvent.getErrorDetails());

        // Verify no further interactions
        verifyNoMoreInteractions(jwtUtil, refreshTokenService);
    }
    
    @Test
    @DisplayName("Login: Should succeed for a previously locked account whose lock has expired")
    void login_shouldSucceed_whenAccountLockIsExpired() {
        setupMockRequest();
        LoginRequest loginRequest = new LoginRequest(username, password);
        user.setLockedUntil(Instant.now().minusSeconds(1)); // Lock expired 1 second ago
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, password)).thenReturn(true);
        when(jwtUtil.generateToken(userId)).thenReturn(accessToken);
        when(jwtUtil.getExpiration()).thenReturn(expiration);
        
        // Act
        LoginSuccessResponse<TokenResponse> response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        verify(refreshTokenService).createRefreshToken(any(), any());
    }

    @Test
    @DisplayName("Login: Should return LoginSuccessResponse and log success audit event")
    void login_shouldReturnLoginSuccessResponse_andLogAudit_onSuccess() {
        setupMockRequest();
        LoginRequest loginRequest = new LoginRequest(username, password);
        
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, password)).thenReturn(true);
        when(jwtUtil.generateToken(userId)).thenReturn(accessToken);
        when(jwtUtil.getExpiration()).thenReturn(expiration);
        when(jwtUtil.generateRefreshToken(userId)).thenReturn(refreshTokenStr);
        
        // Act
        LoginSuccessResponse<TokenResponse> response = authService.login(loginRequest);
        
        // Assert Response
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        TokenResponse tokenResponse = response.getData();
        assertEquals(accessToken, tokenResponse.getToken());
        
        // Assert Interactions
        verify(refreshTokenService).deleteAllUserTokens(user);
        verify(refreshTokenService).createRefreshToken(user, refreshTokenStr);
        
        // Verify rigorous audit event details
        verify(auditLogEventService).sendAuditEvent(auditEventCaptor.capture());
        AuditLogKafkaEvent capturedEvent = auditEventCaptor.getValue();
        assertEquals(AppConstants.SUCCESS_EVENT_TYPE, capturedEvent.getStatus());
        assertEquals(AppConstants.LOGIN_EVENT, capturedEvent.getEvent());
        assertEquals(user.getId(), capturedEvent.getActorId());
        assertNull(capturedEvent.getErrorDetails()); // No error details on success
        assertNotNull(capturedEvent.getStateAfter().get("lastLoginAt"));
    }

    @Test
    @DisplayName("Login: Should throw RuntimeException on unexpected error during token generation")
    void login_shouldThrowRuntimeException_onUnexpectedError() {
        setupMockRequest();
        LoginRequest loginRequest = new LoginRequest(username, password);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(password, user.getPasswordHash())).thenReturn(true);
        when(jwtUtil.generateToken(any(UUID.class))).thenThrow(new RuntimeException("JWT signing error"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(loginRequest));
        assertEquals(AppConstants.GENERIC_ERROR_MESSAGE, exception.getMessage());

        // Verify that the error was logged
        verify(logger).error(any(RuntimeException.class));
    }
}