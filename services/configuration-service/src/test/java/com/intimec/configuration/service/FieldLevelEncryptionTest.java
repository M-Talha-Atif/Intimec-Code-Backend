package com.aidentx.authservice.service.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.model.dto.Encryption.ConfigurationPatchRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for field-level encryption functionality in configuration management.
 * This version is updated to test the refactored service architecture, with all original tests restored and fixed.
 */
@ExtendWith(MockitoExtension.class)
public class FieldLevelEncryptionTest {

    private static final String CONFIG_KEY = "email_test";
    private static final String CONFIG_TYPE_STRING = "notification";

    // --- Mocks for all dependencies ---
    @Mock private ConfigurationRepository configurationRepository;
    @Mock private ArtifactServiceImpl artifactService;
    @Mock private ConfigurationAuditHelper auditHelper;
    @Mock private EncryptionService coreEncryptionService;
    @Mock private FieldEncryptionService fieldEncryptionService;
    @Mock private UnifiedLoggerFactory loggerFactory; // Mock the logger factory
    @Mock private IUnifiedLogger logger; // Mock for IUnifiedLogger
    @Mock private LogEventBuilder logEventBuilder;

    // --- Real Objects ---
    private ObjectMapper objectMapper;
    private ConfigurationEncryptionService encryptionServiceDelegate;
    private ConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        // Set up loggerFactory to return the logger mock for both services BEFORE creating instances
        lenient().when(loggerFactory.getLogger("ConfigurationServiceImpl", "service-config")).thenReturn(logger);
        lenient().when(loggerFactory.getLogger("ConfigurationEncryptionService", "encryption-config")).thenReturn(logger);

        // Mock logger chain methods
        lenient().when(logger.trace()).thenReturn(logEventBuilder);
        lenient().when(logger.info()).thenReturn(logEventBuilder);
        lenient().when(logger.debug()).thenReturn(logEventBuilder);
        lenient().when(logger.warn()).thenReturn(logEventBuilder);
        lenient().when(logger.error(any(Throwable.class))).thenReturn(logEventBuilder);
        
        // Mock LogEventBuilder fluent methods  
        lenient().when(logEventBuilder.message(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.detail(anyString(), any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.details(anyMap())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectType(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectId(anyString())).thenReturn(logEventBuilder);
        lenient().doNothing().when(logEventBuilder).log();

        // Create a real instance of the delegate service, injecting mocks for its dependencies
        encryptionServiceDelegate = new ConfigurationEncryptionService(
            coreEncryptionService,
            fieldEncryptionService,
            objectMapper,
            loggerFactory // Pass the mocked factory
        );

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
    void testFieldLevelEncryption() throws Exception {
        // Arrange
        ObjectNode testData = objectMapper.createObjectNode();
        testData.put("smtp_host", "smtp.example.com");
        testData.put("smtp_port", 587);
        testData.put("smtp_username", "user@example.com");
        testData.put("smtp_password", "secret123");
        testData.put("api_key", "sk-1234567890abcdef");

        ObjectNode encryptedData = objectMapper.createObjectNode();
        encryptedData.put("smtp_host", "smtp.example.com");
        encryptedData.put("smtp_port", 587);
        encryptedData.put("smtp_username", "user@example.com");
        encryptedData.set("smtp_password", objectMapper.createObjectNode().put("$crypto", "encrypted_password_data"));
        encryptedData.set("api_key", objectMapper.createObjectNode().put("$crypto", "encrypted_api_key_data"));

        Configuration savedConfig = new Configuration(CONFIG_TYPE_STRING, CONFIG_KEY);
        savedConfig.setConfigData(encryptedData);

        // Mocks
        when(fieldEncryptionService.encryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING), eq(false))).thenReturn(encryptedData);
        when(fieldEncryptionService.containsEncryptedFields(any(JsonNode.class))).thenReturn(true);
        when(fieldEncryptionService.decryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING))).thenReturn(testData);
        when(configurationRepository.save(any(Configuration.class))).thenReturn(savedConfig);
        when(configurationRepository.findByConfigTypeAndConfigKey(CONFIG_TYPE_STRING, CONFIG_KEY)).thenReturn(Optional.of(savedConfig));

        // Act
        Configuration saved = configurationService.saveConfiguration(ConfigurationType.NOTIFICATION, CONFIG_KEY, testData, "config", false);
        Optional<JsonNode> retrieved = configurationService.getConfigurationData(ConfigurationType.NOTIFICATION, CONFIG_KEY);

        // Assert
        assertNotNull(saved);
        assertTrue(hasEncryptedField(saved.getConfigData(), "smtp_password"), "Password should be encrypted in storage");
        
        assertTrue(retrieved.isPresent());
        JsonNode decryptedData = retrieved.get();
        assertEquals("secret123", decryptedData.get("smtp_password").asText());
        assertEquals("sk-1234567890abcdef", decryptedData.get("api_key").asText());

        verify(fieldEncryptionService, atLeastOnce()).encryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING), eq(false));
        verify(fieldEncryptionService, atLeastOnce()).decryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING));
    }

    @Test
    void testPatchWithFieldLevelEncryption() throws Exception {
        // Arrange
        ObjectNode initialEncryptedData = objectMapper.createObjectNode()
            .put("smtp_host", "smtp.example.com")
            .put("active", true)
            .set("smtp_password", objectMapper.createObjectNode().put("$crypto", "encrypted_old_password"));

        ObjectNode initialDecryptedData = objectMapper.createObjectNode()
            .put("smtp_host", "smtp.example.com")
            .put("active", true)
            .put("smtp_password", "secret123");

        Configuration existingConfig = new Configuration(CONFIG_TYPE_STRING, CONFIG_KEY);
        existingConfig.setConfigData(initialEncryptedData);
        
        ObjectNode patchData = objectMapper.createObjectNode()
            .put("smtp_password", "newsecret456")
            .put("smtp_port", 587);

        ConfigurationPatchRequest patchRequest = new ConfigurationPatchRequest();
        patchRequest.setData(patchData);
        patchRequest.setActive(false);

        ObjectNode finalEncryptedData = objectMapper.createObjectNode()
            .put("smtp_host", "smtp.example.com")
            .put("active", false)
            .put("smtp_port", 587)
            .set("smtp_password", objectMapper.createObjectNode().put("$crypto", "encrypted_new_password"));
            
        ObjectNode finalDecryptedData = objectMapper.createObjectNode()
            .put("smtp_host", "smtp.example.com")
            .put("active", false)
            .put("smtp_port", 587)
            .put("smtp_password", "newsecret456");

        Configuration updatedConfig = new Configuration(CONFIG_TYPE_STRING, CONFIG_KEY);
        updatedConfig.setConfigData(finalEncryptedData);

        // Mocks
        when(configurationRepository.findByConfigTypeAndConfigKey(CONFIG_TYPE_STRING, CONFIG_KEY)).thenReturn(Optional.of(existingConfig));
        when(fieldEncryptionService.containsEncryptedFields(any(JsonNode.class))).thenReturn(true);
        when(fieldEncryptionService.decryptSensitiveFields(eq(initialEncryptedData), eq(CONFIG_TYPE_STRING))).thenReturn(initialDecryptedData);
        when(fieldEncryptionService.encryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING), eq(false))).thenReturn(finalEncryptedData);
        when(configurationRepository.save(any(Configuration.class))).thenReturn(updatedConfig);
        when(fieldEncryptionService.decryptSensitiveFields(eq(finalEncryptedData), eq(CONFIG_TYPE_STRING))).thenReturn(finalDecryptedData);

        // Act
        configurationService.partialUpdateConfiguration(ConfigurationType.NOTIFICATION, CONFIG_KEY, patchRequest);
        Optional<JsonNode> retrieved = configurationService.getConfigurationData(ConfigurationType.NOTIFICATION, CONFIG_KEY);

        // Assert
        assertTrue(retrieved.isPresent());
        assertEquals("newsecret456", retrieved.get().get("smtp_password").asText());
        assertFalse(retrieved.get().get("active").asBoolean());

        verify(fieldEncryptionService, atLeastOnce()).decryptSensitiveFields(eq(initialEncryptedData), eq(CONFIG_TYPE_STRING));
        verify(fieldEncryptionService, atLeastOnce()).encryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING), eq(false));
    }

    // --- RESTORED TEST ---
    @Test
    void testMixedEncryptionScenarios() throws Exception {
        // Arrange
        ObjectNode testData = objectMapper.createObjectNode();
        testData.put("service_name", "Email Service");
        testData.put("smtp_password", "secret123");
        testData.put("api_token", "token_abc123");

        ObjectNode encryptedData = objectMapper.createObjectNode();
        encryptedData.put("service_name", "Email Service");
        encryptedData.set("smtp_password", objectMapper.createObjectNode().put("$crypto", "encrypted_password_data"));
        encryptedData.set("api_token", objectMapper.createObjectNode().put("$crypto", "encrypted_token_data"));

        Configuration savedConfig = new Configuration(CONFIG_TYPE_STRING, CONFIG_KEY);
        savedConfig.setConfigData(encryptedData);

        // Mocks
        when(fieldEncryptionService.encryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING), eq(false))).thenReturn(encryptedData);
        when(fieldEncryptionService.containsEncryptedFields(any(JsonNode.class))).thenReturn(true);
        when(fieldEncryptionService.decryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING))).thenReturn(testData);
        when(configurationRepository.findByConfigTypeAndConfigKey(CONFIG_TYPE_STRING, CONFIG_KEY))
            .thenReturn(Optional.empty()) // First call for save
            .thenReturn(Optional.of(savedConfig)); // Second call for retrieve
        when(configurationRepository.save(any(Configuration.class))).thenReturn(savedConfig);

        // Act
        Configuration saved = configurationService.saveConfiguration(ConfigurationType.NOTIFICATION, CONFIG_KEY, testData, "config", false);
        Optional<JsonNode> retrieved = configurationService.getConfigurationData(ConfigurationType.NOTIFICATION, CONFIG_KEY);

        // Assert
        assertTrue(hasEncryptedField(saved.getConfigData(), "smtp_password"));
        assertTrue(retrieved.isPresent());
        assertEquals("secret123", retrieved.get().get("smtp_password").asText());
        assertEquals("token_abc123", retrieved.get().get("api_token").asText());

        verify(fieldEncryptionService, atLeastOnce()).encryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING), eq(false));
        verify(fieldEncryptionService, atLeastOnce()).decryptSensitiveFields(any(), eq(CONFIG_TYPE_STRING));
    }

    // --- RESTORED TEST ---
    @Test
    void testEncryptionServiceInteraction() {
        // This test verifies that the mock setup itself is working as expected.
        // It's a "meta-test" to ensure our assumptions about the mocks are correct.
        
        // Arrange
        ObjectNode encryptedData = objectMapper.createObjectNode();
        encryptedData.set("password", objectMapper.createObjectNode().put("$crypto", "encrypted_data"));

        // Mocks
        when(fieldEncryptionService.isSensitiveField("password")).thenReturn(true);
        when(fieldEncryptionService.isSensitiveField("username")).thenReturn(false);
        when(fieldEncryptionService.containsEncryptedFields(encryptedData)).thenReturn(true);

        // Act & Assert
        assertTrue(fieldEncryptionService.isSensitiveField("password"));
        assertFalse(fieldEncryptionService.isSensitiveField("username"));
        assertTrue(fieldEncryptionService.containsEncryptedFields(encryptedData));

        // Verify that the mocked methods were called as expected by the assertions above
        verify(fieldEncryptionService, times(1)).isSensitiveField("password");
        verify(fieldEncryptionService, times(1)).isSensitiveField("username");
        verify(fieldEncryptionService, times(1)).containsEncryptedFields(encryptedData);
    }

    /**
     * Helper method to check if a field is encrypted using the $crypto format.
     */
    private boolean hasEncryptedField(JsonNode data, String fieldName) {
        if (data == null || !data.has(fieldName)) return false;
        JsonNode field = data.get(fieldName);
        return field != null && field.isObject() && field.has("$crypto");
    }
}