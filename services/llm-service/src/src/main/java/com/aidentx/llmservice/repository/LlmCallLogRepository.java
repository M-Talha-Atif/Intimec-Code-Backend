package com.aidentx.llmservice.repository;

import com.aidentx.llmservice.model.LlmCallLog;
import com.aidentx.llmservice.model.enums.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LlmCallLogRepository extends JpaRepository<LlmCallLog, Long> {
    
    /**
     * Find all call logs by provider type
     */
    List<LlmCallLog> findByProviderType(ProviderType providerType);
    
    /**
     * Find call logs within a date range
     */
    List<LlmCallLog> findByCreatedAtBetween(Instant startDate, Instant endDate);
    
    /**
     * Find call logs by status
     */
    List<LlmCallLog> findByStatus(String status);
}
