package com.aidentx.authservice.service.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidentx.authservice.model.dto.Encryption.ConfigurationPatchRequest;
import com.aidentx.authservice.model.entity.Configuration.Configuration;
import com.aidentx.configuration.ConfigurationType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test class for the PATCH functionality of ConfigurationService
 */
public class ConfigurationPatchTest {

    @Mock
    private ConfigurationService configurationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testPatchInlineConfiguration() throws Exception {
        // Arrange
        String configType = "sso";
        String configKey = "test-patch-config";

        // Mock updated configuration  
        Configuration updatedConfig = new Configuration(configType, configKey);
        JsonNode updatedData = objectMapper.readTree("{\"active\": true, \"entityId\": \"https://test.com\", \"timeout\": 60000, \"newField\": \"test\"}");
        updatedConfig.setConfigData(updatedData);

        ConfigurationPatchRequest patchRequest = new ConfigurationPatchRequest();
        patchRequest.setActive(true);
        patchRequest.setData(objectMapper.readTree("{\"timeout\": 60000, \"newField\": \"test\"}"));

        // Mock service behavior
        when(configurationService.partialUpdateConfiguration(
                ConfigurationType.SSO, configKey, patchRequest))
                .thenReturn(updatedConfig);

        // Act
        Configuration result = configurationService.partialUpdateConfiguration(
                ConfigurationType.SSO, configKey, patchRequest);

        // Assert
        assertNotNull(result);
        JsonNode resultData = result.getConfigData();
        assertNotNull(resultData);
        assertTrue(resultData.has("active"));
        assertTrue(resultData.get("active").asBoolean());
        assertTrue(resultData.has("entityId"));
        assertEquals("https://test.com", resultData.get("entityId").asText());
        assertTrue(resultData.has("timeout"));
        assertEquals(60000, resultData.get("timeout").asInt());
        assertTrue(resultData.has("newField"));
        assertEquals("test", resultData.get("newField").asText());

        verify(configurationService).partialUpdateConfiguration(ConfigurationType.SSO, configKey, patchRequest);
    }

    @Test
    public void testPatchActiveFlag() throws Exception {
        // Arrange
        String configType = "database";
        String configKey = "test-active-config";

        Configuration updatedConfig = new Configuration(configType, configKey);
        JsonNode updatedData = objectMapper.readTree("{\"active\": false, \"entityId\": \"https://test.com\", \"enabled\": true}");
        updatedConfig.setConfigData(updatedData);

        ConfigurationPatchRequest patchRequest = new ConfigurationPatchRequest();
        patchRequest.setActive(false);

        when(configurationService.partialUpdateConfiguration(
                ConfigurationType.DATABASE, configKey, patchRequest))
                .thenReturn(updatedConfig);

        // Act
        Configuration result = configurationService.partialUpdateConfiguration(
                ConfigurationType.DATABASE, configKey, patchRequest);

        // Assert
        JsonNode resultData = result.getConfigData();
        assertTrue(resultData.has("active"));
        assertFalse(resultData.get("active").asBoolean());
        assertTrue(resultData.has("entityId"));
        assertTrue(resultData.has("enabled"));

        verify(configurationService).partialUpdateConfiguration(ConfigurationType.DATABASE, configKey, patchRequest);
    }

    @Test
    public void testPatchNonExistentConfiguration() {
        // Arrange
        ConfigurationPatchRequest patchRequest = new ConfigurationPatchRequest();
        patchRequest.setActive(true);

        when(configurationService.partialUpdateConfiguration(
                ConfigurationType.SSO, "non-existent-config", patchRequest))
                .thenThrow(new IllegalArgumentException("Configuration not found: sso/non-existent-config"));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            configurationService.partialUpdateConfiguration(
                    ConfigurationType.SSO, "non-existent-config", patchRequest);
        });

        verify(configurationService).partialUpdateConfiguration(ConfigurationType.SSO, "non-existent-config", patchRequest);
    }
}