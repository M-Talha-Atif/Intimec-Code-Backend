package com.aidentx.llmservice.repository;

import com.aidentx.llmservice.model.Provider;
import com.aidentx.llmservice.model.enums.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    
    /**
     * Find a provider by provider type and API key
     */
    Optional<Provider> findByProviderAndApiKey(ProviderType provider, String apiKey);
    
    /**
     * Find all providers by provider type
     */
    Optional<Provider> findByProvider(ProviderType provider);
    
    /**
     * Check if a provider with given type and API key exists
     */
    boolean existsByProviderAndApiKey(ProviderType provider, String apiKey);

    /**
     * Check if a provider with given type exists
     */
    boolean existsByProvider(ProviderType provider);

    /**
     * Update the API key for a given provider type
     */
    @Modifying
    @Transactional
    @Query("UPDATE Provider p SET p.apiKey = :apiKey WHERE p.provider = :provider")
    int updateApiKeyByProvider(@Param("provider") ProviderType provider, @Param("apiKey") String apiKey);
}
