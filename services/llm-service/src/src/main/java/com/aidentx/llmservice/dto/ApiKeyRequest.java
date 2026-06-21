package com.aidentx.llmservice.dto;
import java.util.Optional;
import com.aidentx.llmservice.model.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for saving API keys.
 */
public class ApiKeyRequest {
    @NotNull(message = "Provider is required")
    private ProviderType provider;

    private String apiKey;
    
    private String model;

    // Default constructor
    public ApiKeyRequest() {}

    // Constructor with fields
    public ApiKeyRequest(ProviderType provider, String apiKey) {
        this.provider = provider;
        this.apiKey = apiKey;
    }

    // Getters and Setters
    public ProviderType getProvider() {
        return provider;
    }

    public void setProvider(ProviderType provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
