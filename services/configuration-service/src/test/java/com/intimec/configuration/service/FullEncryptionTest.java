package com.aidentx.authservice.service.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.model.entity.Configuration.Configuration;
import com.aidentx.authservice.repository.config.ConfigurationRepository;
import com.aidentx.authservice.service.Configuration.impl.ArtifactServiceImpl;
import com.aidentx.authservice.service.Configuration.impl.ConfigurationEncryptionService;
import com.aidentx.authservice.service.Configuration.impl.ConfigurationServiceImpl;
import com.aidentx.authservice.service.Configuration.impl.delegate.ConfigurationAuditHelper;
import com.aidentx.configuration.ConfigurationType;
import com.aidentx.encryption.service.EncryptionService;
import com.aidentx.encryption.service.FieldEncryptionService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for full configuration encryption scenarios.
 * This class verifies the end-to-end decryption flow for fully encrypted configuration data.
 */
@ExtendWith(MockitoExtension.class)
public class FullEncryptionTest {

    private static final String CONFIG_KEY = "jwt_settings";
    private static final String ENCRYPTED_BLOB = "eyJlbmN...SyIsIml2IjoiV..."; // A placeholder for the blob

    // --- Mocks for Service Dependencies ---
    @Mock private ConfigurationRepository configurationRepository;
    @Mock private ArtifactServiceImpl artifactService;
    @Mock private ConfigurationAuditHelper auditHelper;
    @Mock private EncryptionService coreEncryptionService;
    @Mock private FieldEncryptionService fieldEncryptionService;
    @Mock private UnifiedLoggerFactory loggerFactory;

    // --- Mocks for Logging ---
    @Mock private IUnifiedLogger logger;
    @Mock private LogEventBuilder logEventBuilder;

    private ObjectMapper objectMapper;
    private ConfigurationEncryptionService encryptionServiceDelegate;
    private ConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Mock the logger factory to return our mocked logger for all service instances
        when(loggerFactory.getLogger(anyString(), anyString())).thenReturn(logger);

        // Mock logger chain methods to prevent NullPointerExceptions
        lenient().when(logger.trace()).thenReturn(logEventBuilder);
        lenient().when(logger.info()).thenReturn(logEventBuilder);
        lenient().when(logger.debug()).thenReturn(logEventBuilder);
        lenient().when(logger.warn()).thenReturn(logEventBuilder);
        lenient().when(logger.error(any(Throwable.class))).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.message(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.detail(anyString(), any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.details(anyMap())).thenReturn(logEventBuilder);
        doNothing().when(logEventBuilder).log();
        
        // Create the real delegate service with its mocked dependencies
        encryptionServiceDelegate = new ConfigurationEncryptionService(
            coreEncryptionService,
            fieldEncryptionService,
            objectMapper,
            loggerFactory
        );

        // Create the main service under test with its mocked dependencies
        configurationService = new ConfigurationServiceImpl(
            configurationRepository,
            artifactService,
            encryptionServiceDelegate, // Pass the real delegate instance
            auditHelper,
            objectMapper,
            loggerFactory
        );
    }

    @Test
    @DisplayName("Get Config Data: Should correctly decrypt a fully encrypted configuration")
    void testFullEncryptionDecryption() throws Exception {
        // Arrange
        // 1. Define the final, decrypted data we expect
        ObjectNode expectedDecryptedData = objectMapper.createObjectNode();
        expectedDecryptedData.put("secret", "my-jwt-secret-key");
        expectedDecryptedData.put("issuer", "mycompany-auth");

        // 2. Define the encrypted JSON structure that is stored in the database
        ObjectNode fullEncryptedInlineData = objectMapper.createObjectNode();
        fullEncryptedInlineData.put("encrypted", true);
        fullEncryptedInlineData.put("data", ENCRYPTED_BLOB);

        // 3. Create the JPA entity that holds this encrypted data
        Configuration configuration = new Configuration(ConfigurationType.SECURITY.getValue(), CONFIG_KEY);
        configuration.setConfigData(fullEncryptedInlineData);

        // 4. Stub the repository mock:
        //    When findByConfigTypeAndConfigKey is called with the exact strings, return our test configuration.
        when(configurationRepository.findByConfigTypeAndConfigKey(ConfigurationType.SECURITY.getValue(), CONFIG_KEY))
            .thenReturn(Optional.of(configuration));
        
        // 5. Stub the core decryption service:
        //    When the decrypt method is called with our specific blob and key type, return the expected JSON string.
        //    Note: The key type is determined by EncryptionRulesConfig.getKeyType(), which returns "security_config" for "security"
        String decryptedJsonString = objectMapper.writeValueAsString(expectedDecryptedData);
        when(coreEncryptionService.decrypt(eq(ENCRYPTED_BLOB), eq("security_config")))
            .thenReturn(decryptedJsonString);

        // Act
        // 6. Call the service method we want to test
        Optional<JsonNode> retrieved = configurationService.getConfigurationData(ConfigurationType.SECURITY.getValue(), CONFIG_KEY);

        // Assert
        // 7. Verify the final result
        assertTrue(retrieved.isPresent(), "The configuration data Optional should not be empty.");
        JsonNode actualDecryptedData = retrieved.get();
        assertEquals("my-jwt-secret-key", actualDecryptedData.get("secret").asText());
        assertEquals("mycompany-auth", actualDecryptedData.get("issuer").asText());
        assertFalse(actualDecryptedData.has("encrypted"), "The final decrypted data should not contain the 'encrypted' flag.");
        
        // 8. Verify the interactions:
        //    - Confirm that the repository was called with the *exact* arguments.
        //    - Confirm that the core decryption service was also called as expected.
        verify(configurationRepository).findByConfigTypeAndConfigKey(ConfigurationType.SECURITY.getValue(), CONFIG_KEY);
        verify(coreEncryptionService).decrypt(ENCRYPTED_BLOB, "security_config");
    }
}