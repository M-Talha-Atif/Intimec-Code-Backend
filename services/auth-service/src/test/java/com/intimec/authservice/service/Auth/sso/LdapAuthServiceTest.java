package com.aidentx.authservice.service.Auth.sso;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.config.LdapConfig;
import com.aidentx.authservice.exception.UserNotFoundException;
import com.aidentx.authservice.model.dto.LoginRequest;
import com.aidentx.authservice.model.dto.LoginSuccessResponse;
import com.aidentx.authservice.model.dto.TokenResponse;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
 * Rigorous unit tests for {@link LdapAuthService}.
 */
@ExtendWith(MockitoExtension.class)
class LdapAuthServiceTest {

    @Mock private SsoConfigurationManager ssoConfigManager;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserAuthenticationMethodService userAuthenticationMethodService;
    @Mock private AuditLogEventService auditLogEventService;
    @Mock private UnifiedLoggerFactory loggerFactory;
    @Mock private IUnifiedLogger logger;
    @Mock private LogEventBuilder logEventBuilder;

    private LdapAuthService ldapAuthService;

    private LdapConfig enabledLdapConfig;
    private LoginRequest loginRequest;
    private User user;
    private UUID userId;
    private static final String MOCK_IP_ADDRESS = "127.0.0.1";
    private static final String MOCK_USER_AGENT = "JUnit Test Agent";

    @BeforeEach
    void setUp() {
        when(loggerFactory.getLogger(anyString(), anyString())).thenReturn(logger);
        lenient().when(logger.trace()).thenReturn(logEventBuilder);
        lenient().when(logger.info()).thenReturn(logEventBuilder);
        lenient().when(logger.debug()).thenReturn(logEventBuilder);
        lenient().when(logger.warn()).thenReturn(logEventBuilder);
        lenient().when(logger.error(any(Throwable.class))).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.message(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectType(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectId(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.detail(anyString(), any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.details(anyMap())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.failureReason(anyString())).thenReturn(logEventBuilder);
        doNothing().when(logEventBuilder).log();

        ldapAuthService = new LdapAuthService(
            loggerFactory,
            ssoConfigManager,
            authenticationManager,
            userRepository,
            jwtUtil,
            refreshTokenService,
            userAuthenticationMethodService,
            auditLogEventService
        );

        enabledLdapConfig = new LdapConfig();
        enabledLdapConfig.setEnabled(true);
        
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("ldapuser");
        user.setEmail("ldapuser@example.com");

        loginRequest = new LoginRequest("ldapuser", "password");
        
        // This method is called before every test
        setupMockRequest();
    }
    
    /**
     * Sets up a mock HttpServletRequest and stubs all necessary getHeader calls.
     */
    private void setupMockRequest() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        // --- FIX IS HERE ---
        // Stub all headers that RequestUtils might ask for.
        // We make getHeader lenient so it doesn't fail on unstubbed calls.
        lenient().when(mockRequest.getHeader("X-Forwarded-For")).thenReturn(MOCK_IP_ADDRESS);
        lenient().when(mockRequest.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("Proxy-Client-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("HTTP_CLIENT_IP")).thenReturn(null);
        lenient().when(mockRequest.getHeader("HTTP_X_FORWARDED_FOR")).thenReturn(null);
        
        // Stub the User-Agent header call specifically.
        when(mockRequest.getHeader("User-Agent")).thenReturn(MOCK_USER_AGENT);
        
        // Stub the final fallback method if no headers are found.
        lenient().when(mockRequest.getRemoteAddr()).thenReturn("fallback-ip");
    }

    @Test
    @DisplayName("LDAP Login: Should succeed when LDAP is enabled, auth succeeds, and user exists in DB")
    void ldapLogin_shouldSucceed_whenAllConditionsMet() {
        when(ssoConfigManager.getLdapConfig()).thenReturn(enabledLdapConfig);
        
        Authentication successfulAuth = new UsernamePasswordAuthenticationToken(user.getUsername(), "password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(successfulAuth);
        
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(userId)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(userId)).thenReturn("new-refresh-token");
        when(jwtUtil.getExpiration()).thenReturn(3600L); // Mock the expiration time
        doNothing().when(refreshTokenService).deleteAllUserTokens(user); // Mock deleteAllUserTokens
        when(userRepository.save(user)).thenReturn(user); // Mock save
        
        LoginSuccessResponse<TokenResponse> response = ldapAuthService.ldapLogin(loginRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatus());
        assertEquals("new-access-token", response.getData().getToken());

        verify(refreshTokenService).createRefreshToken(eq(user), eq("new-refresh-token"));
        verify(userAuthenticationMethodService).trackUserMethod(eq(user), eq(com.aidentx.authservice.enums.AuthenticationMethod.LDAP));
        verify(auditLogEventService).sendAuditEvent(any());
    }
    
    @Test
    @DisplayName("LDAP Login: Should throw UserNotFoundException if LDAP is disabled")
    void ldapLogin_shouldThrowException_whenLdapIsDisabled() {
        LdapConfig disabledConfig = new LdapConfig();
        disabledConfig.setEnabled(false);
        when(ssoConfigManager.getLdapConfig()).thenReturn(disabledConfig);

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> ldapAuthService.ldapLogin(loginRequest));
        
        assertEquals(AppConstants.MSG_LDAP_AUTH_NOT_ENABLED, exception.getMessage());
        verifyNoInteractions(authenticationManager, userRepository, jwtUtil);
    }

    @Test
    @DisplayName("LDAP Login: Should throw UserNotFoundException if LDAP auth fails (bad credentials)")
    void ldapLogin_shouldThrowException_whenLdapAuthFails() {
        when(ssoConfigManager.getLdapConfig()).thenReturn(enabledLdapConfig);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
            .thenThrow(new BadCredentialsException("LDAP server says no"));
        
        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> ldapAuthService.ldapLogin(loginRequest));
        
        assertEquals(AppConstants.MSG_LDAP_AUTH_ERROR, exception.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
        verifyNoInteractions(jwtUtil);
    }

    @Test
    @DisplayName("LDAP Login: Should throw UserNotFoundException if LDAP user is not in the local DB")
    void ldapLogin_shouldThrowException_whenLdapUserNotInDb() {
        when(ssoConfigManager.getLdapConfig()).thenReturn(enabledLdapConfig);
        
        Authentication successfulAuth = new UsernamePasswordAuthenticationToken(user.getUsername(), "password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(successfulAuth);
        
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> ldapAuthService.ldapLogin(loginRequest));
        
        assertTrue(exception.getMessage().contains(user.getUsername()));
        
        verify(auditLogEventService).sendAuditEvent(any());
        verifyNoInteractions(jwtUtil, refreshTokenService);
    }
}