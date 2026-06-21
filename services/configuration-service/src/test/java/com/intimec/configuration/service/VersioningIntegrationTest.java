package com.aidentx.authservice.service.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidentx.authservice.model.dto.Encryption.ConfigurationPatchRequest;
import com.aidentx.authservice.model.entity.Configuration.Configuration;
import com.aidentx.authservice.repository.config.ConfigurationRepository;
import com.aidentx.authservice.service.Configuration.impl.ArtifactServiceImpl;
import com.aidentx.authservice.service.Configuration.impl.ConfigurationEncryptionService;
import com.aidentx.authservice.service.Configuration.impl.ConfigurationServiceImpl;
import com.aidentx.authservice.service.Configuration.impl.delegate.ConfigurationAuditHelper;
import com.aidentx.configuration.ConfigurationType;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test to verify that versioning works correctly in the actual
 * service implementation.
 */
@ExtendWith(MockitoExtension.class)
public class VersioningIntegrationTest {

    @Mock private ConfigurationRepository configurationRepository;
    @Mock private ArtifactServiceImpl artifactService;
    @Mock private ConfigurationEncryptionService encryptionServiceDelegate;
    @Mock private ConfigurationAuditHelper auditHelper;
    @Mock private UnifiedLoggerFactory loggerFactory; // Mock the logger factory
    @Mock private IUnifiedLogger logger;
    @Mock private LogEventBuilder logEventBuilder;

    private ConfigurationService configurationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        
        // Mock the factory to return our mocked logger - using the exact call the service makes
        when(loggerFactory.getLogger("ConfigurationServiceImpl", "service-config")).thenReturn(logger);
        
        // Mock logger methods that might be called - using lenient to avoid unnecessary stubbing errors
        lenient().when(logger.trace()).thenReturn(logEventBuilder);
        lenient().when(logger.info()).thenReturn(logEventBuilder);
        lenient().when(logger.debug()).thenReturn(logEventBuilder);
        lenient().when(logger.warn()).thenReturn(logEventBuilder);
        lenient().when(logger.error(any(Exception.class))).thenReturn(logEventBuilder);
        
        // Mock LogEventBuilder fluent methods
        lenient().when(logEventBuilder.message(any(String.class))).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.detail(any(String.class), any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.details(anyMap())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectType(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectId(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.failureReason(anyString())).thenReturn(logEventBuilder);
        doNothing().when(logEventBuilder).log();
        
        // Use the new, correct constructor for the main service
        configurationService = new ConfigurationServiceImpl(
            configurationRepository,
            artifactService,
            encryptionServiceDelegate,
            auditHelper,
            objectMapper,
            loggerFactory // Pass the mocked factory
        );
    }

    @Test
    public void testVersionIncrementOnPatch() throws Exception {
        // Arrange
        Configuration existingConfig = new Configuration("database", "test_patch_versioning");
        existingConfig.setConfigData(objectMapper.readTree("{\"host\": \"localhost\", \"port\": 5432}"));
        assertEquals(1, existingConfig.getVersion());

        when(configurationRepository.findByConfigTypeAndConfigKey("database", "test_patch_versioning")).thenReturn(Optional.of(existingConfig));
        when(configurationRepository.save(any(Configuration.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock the delegate service methods to simply pass data through for this test
        when(encryptionServiceDelegate.decryptInlineData(any(), anyString(), anyString())).thenAnswer(i -> i.getArgument(0));
        when(encryptionServiceDelegate.encryptDataForSaving(any(), anyString(), anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(0));
        
        ConfigurationPatchRequest patchRequest = new ConfigurationPatchRequest();
        patchRequest.setData(objectMapper.readTree("{\"timeout\": 30000}"));

        // Act
        Configuration result = configurationService.partialUpdateConfiguration(ConfigurationType.DATABASE, "test_patch_versioning", patchRequest);

        // Assert
        assertEquals(2, result.getVersion(), "Version should increment by 1 on patch.");
        assertTrue(result.getConfigData().has("timeout"), "Patched data should contain the new field.");
        verify(configurationRepository).save(existingConfig);
    }
}