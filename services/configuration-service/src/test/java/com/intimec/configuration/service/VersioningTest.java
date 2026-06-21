package com.aidentx.authservice.service.Configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidentx.authservice.model.dto.Encryption.ConfigurationPatchRequest;
import com.aidentx.authservice.model.entity.Configuration.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that configuration versioning logic works correctly.
 * This tests the entity behavior without requiring full Spring context.
 */
public class VersioningTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testConfigurationVersioningBehavior() throws Exception {
        // Test that version starts at 1 for new configurations
        Configuration config = new Configuration("database", "versioning_test");
        assertEquals(1, config.getVersion()); // Should start at version 1

        // Test manual version increment (simulating service behavior)
        config.setVersion(config.getVersion() + 1);
        assertEquals(2, config.getVersion());

        // Test that we can set config data and version independently
        JsonNode testData = objectMapper.readTree("{\"host\": \"localhost\", \"port\": 5432}");
        config.setConfigData(testData);
        assertEquals(2, config.getVersion()); // Version should remain the same

        // Test incrementing version again
        config.setVersion(config.getVersion() + 1);
        assertEquals(3, config.getVersion());

        // Test that the data is preserved
        assertEquals("localhost", config.getConfigData().get("host").asText());
        assertEquals(5432, config.getConfigData().get("port").asInt());
    }

    @Test
    public void testVersionFieldSettersAndGetters() {
        Configuration config = new Configuration();

        // Test default version
        assertEquals(1, config.getVersion());

        // Test setting custom version
        config.setVersion(5);
        assertEquals(5, config.getVersion());

        // Test incrementing
        config.setVersion(config.getVersion() + 1);
        assertEquals(6, config.getVersion());
    }

    @Test
    public void testConfigurationPatchRequestVersioning() {
        // Test that patch request can be created and processed
        ConfigurationPatchRequest patchRequest = new ConfigurationPatchRequest();
        patchRequest.setActive(false);

        assertNotNull(patchRequest);
        assertFalse(patchRequest.getActive());

        // Test setting data
        patchRequest.setData("{\"timeout\": 60000}");
        assertEquals("{\"timeout\": 60000}", patchRequest.getData());
    }
}