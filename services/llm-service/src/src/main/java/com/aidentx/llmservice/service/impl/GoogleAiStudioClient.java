package com.aidentx.llmservice.service.impl;

import com.aidentx.llmservice.dto.LlmRequest;
import com.aidentx.llmservice.dto.LlmResponse;
import com.aidentx.llmservice.model.enums.ProviderType;
import com.aidentx.llmservice.service.LlmClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Google AI Studio (Gemini) client implementation using OpenAI-compatible API.
 * Uses the Google Generative Language API's OpenAI-compatible endpoint.
 */
@Component
public class GoogleAiStudioClient implements LlmClient {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAiStudioClient.class);
    private static final String DEFAULT_MODEL = "gemini-2.0-flash";
    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";

    @Autowired
    private ObjectMapper objectMapper;

    private final WebClient webClient;

    public GoogleAiStudioClient() {
        this.webClient = WebClient.builder()
            .baseUrl(API_BASE_URL)
            .build();
    }

    @Override
    public Mono<LlmResponse> callModel(LlmRequest request) {
        logger.info("Calling Google AI Studio API for request with model: {}", 
                   request.getModel() != null ? request.getModel() : DEFAULT_MODEL);

        String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;
        String apiKey = request.getApiKey();

        return buildRequest(request, model)
            .flatMap(requestBody -> callGoogleApi(requestBody, apiKey))
            .map(response -> mapToLlmResponse(response, request, model))
            .onErrorResume(this::handleError);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.GOOGLE_AI_STUDIO;
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

    private Mono<JsonNode> callGoogleApi(Map<String, Object> requestBody, String apiKey) {
        return webClient.post()
            .uri("/chat/completions")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(JsonNode.class);
    }

    private LlmResponse mapToLlmResponse(JsonNode geminiResponse, LlmRequest request, String model) {
        LlmResponse response = new LlmResponse();
        response.setProvider(ProviderType.GOOGLE_AI_STUDIO);
        response.setRequestId(UUID.randomUUID().toString());
        response.setModel(model);
        response.setSuccess(true);

        try {
            // Extract content from OpenAI-compatible response format
            if (geminiResponse.has("choices") && geminiResponse.get("choices").isArray() && 
                geminiResponse.get("choices").size() > 0) {
                
                JsonNode choice = geminiResponse.get("choices").get(0);
                if (choice.has("message") && choice.get("message").has("content")) {
                    String content = choice.get("message").get("content").asText();
                    response.setContent(content);
                }
            }
            
            // Extract usage information if available
            if (geminiResponse.has("usage")) {
                JsonNode usage = geminiResponse.get("usage");
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
                response.setErrorMessage("No valid response from Google AI Studio");
            }
            
        } catch (Exception e) {
            logger.error("Error parsing Google AI Studio response", e);
            response.setSuccess(false);
            response.setErrorMessage("Error parsing response: " + e.getMessage());
            response.setContent("Error processing response");
        }

        return response;
    }

    private Mono<LlmResponse> handleError(Throwable error) {
        logger.error("Error calling Google AI Studio API", error);
        
        String errorMessage = "Google AI Studio API error: " + error.getMessage();
        
        LlmResponse errorResponse = new LlmResponse(errorMessage, ProviderType.GOOGLE_AI_STUDIO);
        return Mono.just(errorResponse);
    }
}