package com.aidentx.llmservice.controller;

import com.aidentx.llmservice.dto.ApiKeyRequest;
import com.aidentx.llmservice.dto.LlmRequest;
import com.aidentx.llmservice.dto.LlmResponse;
import com.aidentx.llmservice.model.Provider;
import com.aidentx.llmservice.model.enums.ProviderType;
import com.aidentx.llmservice.repository.ProviderRepository;
import com.aidentx.llmservice.service.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * REST Controller for LLM service operations.
 */
@RestController
@RequestMapping("/api/v1/llm")
@CrossOrigin(origins = "*") // Configure as needed for your frontend
public class LlmController {

    private static final Logger logger = LoggerFactory.getLogger(LlmController.class);

    private final LlmService llmService;
    private final ProviderRepository providerRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public LlmController(LlmService llmService, ProviderRepository providerRepository) {
        this.llmService = llmService;
        this.providerRepository = providerRepository;
    }
    /**
     * get All Providers
     */
    @GetMapping("/providers")
    public ResponseEntity<JsonNode> getAllProviders(
        //@RequestHeader("Authorization") String authHeader
        ) {
        logger.info("Fetching all providers");
        try {
            var providers = ProviderType.values();
            var response = objectMapper.createObjectNode();
            response.put("success", true);
            response.set("providers", objectMapper.valueToTree(providers));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching providers", e);
            var errorResponse = objectMapper.createObjectNode();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to fetch providers: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    /**
     * Save provider API key endpoint.
     */
    @PostMapping("/providers")
    public ResponseEntity<JsonNode> saveProvider(@Valid @RequestBody ApiKeyRequest apiKeyRequest) {
        logger.info("Saving provider API key for provider: {}", apiKeyRequest.getProvider());
        
        try {
            // Check if provider with same type
            if (providerRepository.existsByProvider(apiKeyRequest.getProvider())){
                // Update existing provider's API key
                providerRepository.updateApiKeyByProvider(apiKeyRequest.getProvider(), apiKeyRequest.getApiKey());
                JsonNode response = objectMapper.createObjectNode()
                        .put("success", true)
                        .put("message", "Provider with this API key already updated")
                        .put("provider", apiKeyRequest.getProvider().name());
                return ResponseEntity.ok(response);
            }
            
            // Create and save new provider
            Provider provider = new Provider(apiKeyRequest.getProvider(), apiKeyRequest.getApiKey());
            Provider savedProvider = providerRepository.save(provider);
            
            JsonNode response = objectMapper.createObjectNode()
                    .put("success", true)
                    .put("message", "Provider API key saved successfully")
                    .put("provider", savedProvider.getProvider().name())
                    .put("id", savedProvider.getId());
            
            logger.info("Successfully saved provider API key for provider: {}", apiKeyRequest.getProvider());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error saving provider API key for provider: {}", apiKeyRequest.getProvider(), e);
            
            JsonNode errorResponse = objectMapper.createObjectNode()
                    .put("success", false)
                    .put("error", "Failed to save provider API key: " + e.getMessage())
                    .put("provider", apiKeyRequest.getProvider().name());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get provider API key by provider type.
     */
    @GetMapping("/providers/{providerType}")
    public ResponseEntity<JsonNode> getProviderApiKey(@PathVariable ProviderType providerType) {
        logger.info("Retrieving API key for provider: {}", providerType);
        
        try {
            Optional<Provider> providerOptional = providerRepository.findByProvider(providerType);
            
            if (providerOptional.isPresent()) {
                Provider provider = providerOptional.get();
                JsonNode response = objectMapper.createObjectNode()
                        .put("success", true)
                        .put("provider", provider.getProvider().name())
                        .put("id", provider.getId());
                
                logger.info("Successfully retrieved API key for provider: {}", providerType);
                return ResponseEntity.ok(response);
            } else {
                JsonNode notFoundResponse = objectMapper.createObjectNode()
                        .put("success", false)
                        .put("message", "No API key found for provider: " + providerType.name())
                        .put("provider", providerType.name());
                
                logger.warn("No API key found for provider: {}", providerType);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notFoundResponse);
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving API key for provider: {}", providerType, e);
            
            JsonNode errorResponse = objectMapper.createObjectNode()
                    .put("success", false)
                    .put("error", "Failed to retrieve API key: " + e.getMessage())
                    .put("provider", providerType.name());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Process an LLM request.
     *
     * @param request the validated LLM request
     * @return the LLM response
     */
    @PostMapping("/process")
    public ResponseEntity<LlmResponse> processLlmRequest(@Valid @RequestBody LlmRequest request) {
        logger.info("Received LLM request for provider: {}", request.getProvider());
        
        try {
            LlmResponse response = llmService.processRequest(request);
            
            if (response.isSuccess()) {
                logger.info("Successfully processed LLM request for provider: {}", request.getProvider());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("LLM request failed for provider: {}, error: {}", 
                           request.getProvider(), response.getErrorMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error processing LLM request for provider: {}", 
                        request.getProvider(), e);
            
            LlmResponse errorResponse = new LlmResponse(
                "Internal server error: " + e.getMessage(), 
                request.getProvider()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    /*
     * Legacy API key saving endpoint - kept for backwards compatibility.
     * Routes to the new provider saving endpoint.
     */
    @PostMapping("/api-keys")
    @PutMapping("/api-keys")
    public ResponseEntity<JsonNode> saveApiKeyLegacy(@RequestBody ApiKeyRequest apiKeyRequest) {
        logger.info("Legacy API key endpoint called, redirecting to new provider endpoint");
        return saveProvider(apiKeyRequest);
    }

    /**
     * Health check endpoint.
     *
         * @return simple health status
     */
    @GetMapping("/test")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("LLM Service is running");
    }

    @PostMapping("/test-provider")
    public ResponseEntity<LlmResponse> testProvider(@Valid @RequestBody LlmRequest request) {
        logger.info("Testing provider connectivity for: {}", request.getProvider());

        // Basic validation: API key required for provider connectivity test

        Optional<Provider> providerOpt = providerRepository.findByProvider(request.getProvider());
        if (providerOpt.isEmpty()) {
            LlmResponse errorResponse = new LlmResponse("No API key found for provider: " + request.getProvider(), request.getProvider());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        request.setApiKey(providerOpt.get().getApiKey());

        // Use a short test prompt if none provided
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            request.setPrompt("Ping");
        }

        try {
            LlmResponse response = llmService.processRequest(request);
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                // provider returned an error/failed response -> surface as 502
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(response);
            }
        } catch (Exception e) {
            logger.error("Error testing provider {}", request.getProvider(), e);
            LlmResponse errorResponse = new LlmResponse("Error testing provider: " + e.getMessage(), request.getProvider());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}