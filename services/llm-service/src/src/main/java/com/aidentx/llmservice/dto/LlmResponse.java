package com.aidentx.llmservice.dto;

import com.aidentx.llmservice.model.enums.ProviderType;
import java.time.LocalDateTime;

/**
 * Response DTO for LLM service calls.
 */
public class LlmResponse {

    private String content;
    private ProviderType provider;
    private String model;
    private Integer tokensUsed;
    private Integer promptTokens;
    private Integer completionTokens;
    private LocalDateTime timestamp;
    private String requestId;
    private boolean success;
    private String errorMessage;

    // Default constructor
    public LlmResponse() {
        this.timestamp = LocalDateTime.now();
        this.success = true;
    }

    // Constructor for successful response
    public LlmResponse(String content, ProviderType provider, String model) {
        this();
        this.content = content;
        this.provider = provider;
        this.model = model;
    }

    // Constructor for error response
    public LlmResponse(String errorMessage, ProviderType provider) {
        this();
        this.errorMessage = errorMessage;
        this.provider = provider;
        this.success = false;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ProviderType getProvider() {
        return provider;
    }

    public void setProvider(ProviderType provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public void setTokensUsed(Integer tokensUsed) {
        this.tokensUsed = tokensUsed;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "LlmResponse{" +
                "content='" + content + '\'' +
                ", provider=" + provider +
                ", model='" + model + '\'' +
                ", tokensUsed=" + tokensUsed +
                ", promptTokens=" + promptTokens +
                ", completionTokens=" + completionTokens +
                ", timestamp=" + timestamp +
                ", requestId='" + requestId + '\'' +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
