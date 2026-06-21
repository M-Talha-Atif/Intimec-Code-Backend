package com.aidentx.encryption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.encryption.service.EncryptionService;
import com.aidentx.encryption.service.FieldEncryptionService;
import com.aidentx.encryption.service.KeyManagementService;
import com.aidentx.encryption.service.impl.FieldEncryptionServiceImpl;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FieldEncryptionService functionality.
 */
public class FieldEncryptionServiceTest {

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private KeyManagementService keyManagementService;

    @Mock
    private UnifiedLoggerFactory loggerFactory; // Mock the logger factory
    
    @Mock
    private IUnifiedLogger logger;
    
    @Mock
    private LogEventBuilder logEventBuilder;

    private FieldEncryptionService fieldEncryptionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        
        // Mock logger factory and logger chain
        when(loggerFactory.getLogger("FieldEncryption", "encryption-fields")).thenReturn(logger);
        when(logger.trace()).thenReturn(logEventBuilder);
        when(logger.debug()).thenReturn(logEventBuilder);
        when(logger.info()).thenReturn(logEventBuilder);
        when(logger.warn()).thenReturn(logEventBuilder);
        when(logger.error(any(Throwable.class))).thenReturn(logEventBuilder);
        when(logEventBuilder.message(anyString())).thenReturn(logEventBuilder);
        when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        when(logEventBuilder.detail(anyString(), any())).thenReturn(logEventBuilder);
        
        // Use the new constructor that accepts the logger factory
        fieldEncryptionService = new FieldEncryptionServiceImpl(
                encryptionService,
                objectMapper,
                loggerFactory
        );

        // Mock key management service to return a dummy key
        when(keyManagementService.getConfigurationKey(anyString())).thenReturn(new byte[32]);
    }

    @Test
    void testSensitiveFieldDetection() {
        assertTrue(fieldEncryptionService.isSensitiveField("password"));
        assertTrue(fieldEncryptionService.isSensitiveField("secret"));
        assertTrue(fieldEncryptionService.isSensitiveField("api_key"));
        assertTrue(fieldEncryptionService.isSensitiveField("smtp_password"));
        assertTrue(fieldEncryptionService.isSensitiveField("client_secret"));
        assertTrue(fieldEncryptionService.isSensitiveField("access_token"));

        assertFalse(fieldEncryptionService.isSensitiveField("host"));
        assertFalse(fieldEncryptionService.isSensitiveField("port"));
        assertFalse(fieldEncryptionService.isSensitiveField("username"));
        assertFalse(fieldEncryptionService.isSensitiveField("timeout"));
    }

    @Test
    void testFieldLevelEncryption() throws Exception {
        // Create test data with sensitive fields
        ObjectNode testData = objectMapper.createObjectNode();
        testData.put("smtp_host", "smtp.example.com");
        testData.put("smtp_port", 587);
        testData.put("smtp_password", "secret123");
        testData.put("api_key", "sk-1234567890");
        testData.put("from_address", "noreply@example.com");

        // Test field-level encryption
        JsonNode encryptedData = fieldEncryptionService.encryptSensitiveFields(
                testData, "notification", false);

        assertNotNull(encryptedData);

        // Verify non-sensitive fields remain unchanged
        assertEquals("smtp.example.com", encryptedData.get("smtp_host").asText());
        assertEquals(587, encryptedData.get("smtp_port").asInt());
        assertEquals("noreply@example.com", encryptedData.get("from_address").asText());

        // Verify sensitive fields are encrypted (should have $crypto structure)
        assertTrue(hasEncryptedField(encryptedData, "smtp_password"));
        assertTrue(hasEncryptedField(encryptedData, "api_key"));
    }

    @Test
    void testFullConfigEncryption() throws Exception {
        // Mock the encryption service
        when(encryptionService.encrypt(anyString(), anyString())).thenReturn("mock_encrypted_data");

        ObjectNode testData = objectMapper.createObjectNode();
        testData.put("secret", "my-secret");
        testData.put("api_key", "sk-123");

        // Test full encryption
        JsonNode encryptedData = fieldEncryptionService.encryptSensitiveFields(
                testData, "security", true);

        assertNotNull(encryptedData);
        assertTrue(encryptedData.has("encrypted"));
        assertTrue(encryptedData.get("encrypted").asBoolean());
        assertTrue(encryptedData.has("data"));
        assertEquals("mock_encrypted_data", encryptedData.get("data").asText());
    }

    @Test
    void testContainsEncryptedFields() throws Exception {
        // Test data without encryption
        ObjectNode plainData = objectMapper.createObjectNode();
        plainData.put("host", "example.com");
        plainData.put("port", 80);

        assertFalse(fieldEncryptionService.containsEncryptedFields(plainData));

        // Test data with $crypto fields
        ObjectNode fieldEncryptedData = objectMapper.createObjectNode();
        fieldEncryptedData.put("host", "example.com");

        ObjectNode cryptoField = objectMapper.createObjectNode();
        ObjectNode cryptoData = objectMapper.createObjectNode();
        cryptoData.put("type", "x-simple-encryption");
        cryptoField.set("$crypto", cryptoData);
        fieldEncryptedData.set("password", cryptoField);

        assertTrue(fieldEncryptionService.containsEncryptedFields(fieldEncryptedData));
    }

    @Test
    void testDecryptSensitiveFields() throws Exception {
        // Create simple data without actual encrypted fields for this test
        // This test focuses on the structure and behavior, not the crypto implementation
        ObjectNode testData = objectMapper.createObjectNode();
        testData.put("host", "smtp.example.com");
        testData.put("port", 587);
        testData.put("username", "user@example.com");
        testData.put("password", "secret123"); // This will be processed but not actually encrypted in this test

        // Mock the key management service
        when(keyManagementService.getConfigurationKey(anyString())).thenReturn(new byte[16]);

        // Since this is mainly testing that the method doesn't crash and handles normal fields correctly
        JsonNode result = fieldEncryptionService.decryptSensitiveFields(testData, "security");

        assertNotNull(result);
        assertEquals("smtp.example.com", result.get("host").asText());
        assertEquals(587, result.get("port").asInt());
        assertEquals("user@example.com", result.get("username").asText());
        // For a non-encrypted field, it should remain unchanged
        assertEquals("secret123", result.get("password").asText());
    }

    @Test
    void testMixedFieldEncryption() throws Exception {
        // Create configuration with mix of sensitive and non-sensitive fields
        ObjectNode config = objectMapper.createObjectNode();
        config.put("service_name", "Email Service");
        config.put("host", "smtp.example.com");
        config.put("port", 587);
        config.put("username", "user@example.com");
        config.put("password", "secret123");
        config.put("api_key", "sk-abc123");
        config.put("timeout", 30000);
        config.put("ssl_enabled", true);

        // Apply field-level encryption
        JsonNode encrypted = fieldEncryptionService.encryptSensitiveFields(
                config, "notification", false);

        assertNotNull(encrypted);

        // Verify non-sensitive fields are preserved
        assertEquals("Email Service", encrypted.get("service_name").asText());
        assertEquals("smtp.example.com", encrypted.get("host").asText());
        assertEquals(587, encrypted.get("port").asInt());
        assertEquals("user@example.com", encrypted.get("username").asText());
        assertEquals(30000, encrypted.get("timeout").asInt());
        assertTrue(encrypted.get("ssl_enabled").asBoolean());

        // Verify sensitive fields are encrypted
        assertTrue(hasEncryptedField(encrypted, "password"));
        assertTrue(hasEncryptedField(encrypted, "api_key"));
    }

    /**
     * Helper method to check if a field is encrypted using $crypto format
     */
    private boolean hasEncryptedField(JsonNode data, String fieldName) {
        JsonNode field = data.get(fieldName);
        if (field == null || !field.isObject()) {
            return false;
        }

        return field.has("$crypto");
    }
}