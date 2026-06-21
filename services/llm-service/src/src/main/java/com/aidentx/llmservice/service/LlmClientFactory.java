package com.aidentx.llmservice.service;

import com.aidentx.llmservice.exception.ProviderNotFoundException;
import com.aidentx.llmservice.model.enums.ProviderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory for creating LLM clients based on provider type.
 */
@Component
public class LlmClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(LlmClientFactory.class);
    
    private final Map<ProviderType, LlmClient> clientMap;

    public LlmClientFactory(List<LlmClient> clients) {
        this.clientMap = clients.stream()
                .collect(Collectors.toMap(
                    LlmClient::getProviderType,
                    Function.identity()
                ));
        
        logger.info("Initialized LlmClientFactory with {} providers: {}", 
                   clientMap.size(), 
                   clientMap.keySet());
    }

    /**
     * Get the appropriate LLM client for the given provider type.
     *
     * @param providerType the provider type
     * @return the LLM client
     * @throws ProviderNotFoundException if no client is found for the provider
     */
    public LlmClient getClient(ProviderType providerType) {
        LlmClient client = clientMap.get(providerType);
        
        if (client == null) {
            logger.error("No client found for provider: {}", providerType);
            throw new ProviderNotFoundException("No client available for provider: " + providerType);
        }
        
        if (!client.isAvailable()) {
            logger.warn("Client for provider {} is not available", providerType);
            throw new ProviderNotFoundException("Provider " + providerType + " is currently unavailable");
        }
        
        logger.debug("Retrieved client for provider: {}", providerType);
        return client;
    }

    /**
     * Check if a client is available for the given provider type.
     *
     * @param providerType the provider type
     * @return true if a client is available
     */
    public boolean hasClient(ProviderType providerType) {
        LlmClient client = clientMap.get(providerType);
        return client != null && client.isAvailable();
    }

    /**
     * Get all available provider types.
     *
     * @return list of available provider types
     */
    public List<ProviderType> getAvailableProviders() {
        return clientMap.entrySet().stream()
                .filter(entry -> entry.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
