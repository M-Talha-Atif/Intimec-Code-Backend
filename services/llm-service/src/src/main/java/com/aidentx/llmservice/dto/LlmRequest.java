package com.aidentx.llmservice.dto;

import com.aidentx.llmservice.model.enums.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for LLM service calls.
 */
public class LlmRequest {

    @NotNull(message = "Provider is required")
    private ProviderType provider;


    private String apiKey;

    private String prompt;

    private String model; // Optional, provider-specific model

    private Integer maxTokens; // Optional token limit

    private Double temperature; // Optional temperature setting

    // Default constructor
    public LlmRequest() {}

    // Constructor with required fields
    public LlmRequest(ProviderType provider, String apiKey, String prompt) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.prompt = prompt;
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

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    @Override
    public String toString() {
        return "LlmRequest{" +
                "provider=" + provider +
                ", apiKey='***'" + // Mask API key in toString
                ", prompt='" + prompt + '\'' +
                ", model='" + model + '\'' +
                ", maxTokens=" + maxTokens +
                ", temperature=" + temperature +
                '}';
    }
}
