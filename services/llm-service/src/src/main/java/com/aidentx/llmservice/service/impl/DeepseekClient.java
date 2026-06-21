package com.aidentx.llmservice.service.impl;

import com.aidentx.llmservice.dto.LlmRequest;
import com.aidentx.llmservice.dto.LlmResponse;
import com.aidentx.llmservice.model.enums.ProviderType;
import com.aidentx.llmservice.service.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deepseek client implementation using OpenAI-compatible API.
 * Uses the Deepseek API with OpenAI-compatible endpoint format.
 */
@Component
public class DeepseekClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(DeepseekClient.class);
    private static final String DEFAULT_MODEL = "deepseek/deepseek-r1-distill-llama-70b:free";
    private static final String baseUrl = "https://openrouter.ai/api";

    private final WebClient webClient;

    public DeepseekClient() {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    @Override
    public Mono<LlmResponse> callModel(LlmRequest request) {
        logger.info("Calling Deepseek API for request with model: {}", 
                   request.getModel() != null ? request.getModel() : DEFAULT_MODEL);

        String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;
        String apiKey = request.getApiKey();

        return buildRequest(request, model)
            .flatMap(requestBody -> callDeepseekApi(requestBody, apiKey))
            .map(response -> mapToLlmResponse(response, request, model))
            .onErrorResume(this::handleError);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.DEEPSEEK;
    }

    private Mono<Map<String, Object>> buildRequest(LlmRequest request, String model) {
        return Mono.fromCallable(() -> {
            Map<String, Object> requestBody = new HashMap<>();
            
            // Set the model
            requestBody.put("model", model);
            
            // Build the messages array in OpenAI format
            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", request.getPrompt());
            requestBody.put("messages", List.of(message));
            
            // Add optional parameters if specified
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            return requestBody;
        });
    }

    private Mono<JsonNode> callDeepseekApi(Map<String, Object> requestBody, String apiKey) {
        return webClient.post()
            .uri("/v1/chat/completions")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode.class);
    }

    private LlmResponse mapToLlmResponse(JsonNode deepseekResponse, LlmRequest request, String model) {
        LlmResponse response = new LlmResponse();
        response.setProvider(ProviderType.DEEPSEEK);
        response.setRequestId(UUID.randomUUID().toString());
        response.setModel(model);
        response.setSuccess(true);

        try {
            // Extract content from OpenAI-compatible response format
            if (deepseekResponse.has("choices") && deepseekResponse.get("choices").isArray() && 
                deepseekResponse.get("choices").size() > 0) {
                
                JsonNode choice = deepseekResponse.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    String content = choice.get("message").get("content").asText();
                    response.setContent(content);
                }
            }
            
            // Extract usage information if available
            if (deepseekResponse.has("usage")) {
                JsonNode usage = deepseekResponse.get("usage");
                if (usage.has("prompt_tokens")) {
                    response.setPromptTokens(usage.get("prompt_tokens").asInt());
                }
                if (usage.has("completion_tokens")) {
                    response.setCompletionTokens(usage.get("completion_tokens").asInt());
                }
                if (usage.has("total_tokens")) {
                    response.setTokensUsed(usage.get("total_tokens").asInt());
                }
            }
            
            // If no content was extracted, set error
            if (response.getContent() == null || response.getContent().trim().isEmpty()) {
                response.setContent("No content generated");
                response.setSuccess(false);
                response.setErrorMessage("No valid response from Deepseek");
            }
            
        } catch (Exception e) {
            logger.error("Error parsing Deepseek response", e);
            response.setSuccess(false);
            response.setErrorMessage("Error parsing response: " + e.getMessage());
            response.setContent("Error processing response");
        }

        return response;
    }

    private Mono<LlmResponse> handleError(Throwable error) {
        logger.error("Error calling Deepseek API", error);
        
        String errorMessage = "Deepseek API error: " + error.getMessage();
        
        LlmResponse errorResponse = new LlmResponse(errorMessage, ProviderType.DEEPSEEK);
        return Mono.just(errorResponse);
    }
}