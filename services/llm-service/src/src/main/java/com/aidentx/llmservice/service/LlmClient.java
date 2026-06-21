package com.aidentx.llmservice.service;

import com.aidentx.llmservice.dto.LlmRequest;
import com.aidentx.llmservice.dto.LlmResponse;
import reactor.core.publisher.Mono;

/**
 * Interface for LLM provider clients.
 * Each provider (OpenAI, Anthropic, etc.) should implement this interface.
 */
public interface LlmClient {

    /**
     * Call the LLM provider with the given request.
     *
     * @param request the LLM request containing prompt, API key, and other parameters
     * @return a Mono containing the LLM response
     */
    Mono<LlmResponse> callModel(LlmRequest request);

    /**
     * Get the provider type this client supports.
     *
     * @return the provider type
     */
    com.aidentx.llmservice.model.enums.ProviderType getProviderType();

    /**
     * Check if the client is available/healthy.
     *
     * @return true if the client is available
     */
    default boolean isAvailable() {
        return true;
    }
}
