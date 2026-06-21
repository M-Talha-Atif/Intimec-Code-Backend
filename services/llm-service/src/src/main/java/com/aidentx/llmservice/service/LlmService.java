package com.aidentx.llmservice.service;

import com.aidentx.llmservice.dto.LlmRequest;
import com.aidentx.llmservice.dto.LlmResponse;

/**
 * Service interface for LLM operations.
 */
public interface LlmService {

    /**
     * Process an LLM request and return the response.
     *
     * @param request the LLM request containing provider, API key, and prompt
     * @return the LLM response
     */
    LlmResponse processRequest(LlmRequest request);
}
