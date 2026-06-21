package com.aidentx.llmservice.service.impl;

import com.aidentx.llmservice.dto.LlmRequest;
import com.aidentx.llmservice.dto.LlmResponse;
import com.aidentx.llmservice.model.LlmCallLog;
import com.aidentx.llmservice.model.Provider;
import com.aidentx.llmservice.repository.LlmCallLogRepository;
import com.aidentx.llmservice.repository.ProviderRepository;
import com.aidentx.llmservice.service.LlmClient;
import com.aidentx.llmservice.service.LlmClientFactory;
import com.aidentx.llmservice.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.time.Instant;

/**
 * Implementation of LlmService that orchestrates calls to different LLM providers.
 */
@Service
public class LlmServiceImpl implements LlmService {

    private static final Logger logger = LoggerFactory.getLogger(LlmServiceImpl.class);
    
    private final LlmClientFactory clientFactory;
    private final ProviderRepository providerRepository;
    private final LlmCallLogRepository llmCallLogRepository;

    public LlmServiceImpl(LlmClientFactory clientFactory, 
                         ProviderRepository providerRepository,
                         LlmCallLogRepository llmCallLogRepository) {
        this.clientFactory = clientFactory;
        this.providerRepository = providerRepository;
        this.llmCallLogRepository = llmCallLogRepository;
    }

    @Override
    public LlmResponse processRequest(LlmRequest request) {
        logger.info("Processing LLM request for provider: {}", request.getProvider());
        
        Instant startTime = Instant.now();
        Provider providerEntity = null;
        LlmCallLog callLog = null;
        
        try {
            // Resolve or create provider entity if API key is provided
            Optional<Provider> providerOpt = providerRepository.findByProvider(request.getProvider());
            providerEntity = providerOpt.orElseThrow(() -> 
                new IllegalStateException("Provider not found: " + request.getProvider())
           ); // get Api key from request if provided, else use stored key
            // Create call log entry
            callLog = new LlmCallLog(providerEntity, request.getProvider(), request.getPrompt());
            callLog.setStatus("STARTED");
            callLog = llmCallLogRepository.save(callLog);
            
            request.setApiKey(providerEntity.getApiKey());
            // Get the appropriate client from factory
            LlmClient client = clientFactory.getClient(request.getProvider());
            
            // Call the client and block for the result (converting from reactive)
            LlmResponse response = client.callModel(request).block();
            
            // Calculate response time
            long responseTimeMs = Instant.now().toEpochMilli() - startTime.toEpochMilli();
            
            // Update call log with response details
            if (response != null) {
                callLog.setResponseContent(response.getContent());
                callLog.setResponseTimeMs(responseTimeMs);
                callLog.setStatus(response.isSuccess() ? "SUCCESS" : "ERROR");
                if (!response.isSuccess()) {
                    callLog.setErrorMessage(response.getErrorMessage());
                }
                // Set token usage if available (you may need to add this to LlmResponse)
                // callLog.setTokensUsed(response.getTokensUsed());
            } else {
                callLog.setStatus("ERROR");
                callLog.setErrorMessage("No response received from provider");
                callLog.setResponseTimeMs(responseTimeMs);
            }
            
            // Save updated call log
            llmCallLogRepository.save(callLog);
            
            if (response != null && response.isSuccess()) {
                logger.info("Successfully processed LLM request for provider: {} with request ID: {}", 
                           request.getProvider(), response.getRequestId());
            } else {
                logger.warn("LLM request failed for provider: {}, error: {}", 
                           request.getProvider(), 
                           response != null ? response.getErrorMessage() : "Unknown error");
            }
            
            return response;
            
        } catch (Exception e) {
            logger.error("Error processing LLM request for provider: {}", request.getProvider(), e);
            
            // Update call log with error if it was created
            if (callLog != null) {
                try {
                    callLog.setStatus("ERROR");
                    callLog.setErrorMessage("Failed to process request: " + e.getMessage());
                    callLog.setResponseTimeMs(Instant.now().toEpochMilli() - startTime.toEpochMilli());
                    llmCallLogRepository.save(callLog);
                } catch (Exception logException) {
                    logger.error("Failed to update call log with error details", logException);
                }
            }
            
            // Return error response
            LlmResponse errorResponse = new LlmResponse(
                "Failed to process request: " + e.getMessage(),
                request.getProvider()
            );
            return errorResponse;
        }
    }
}