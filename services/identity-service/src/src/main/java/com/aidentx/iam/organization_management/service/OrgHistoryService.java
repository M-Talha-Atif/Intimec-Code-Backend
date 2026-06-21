package com.aidentx.iam.organization_management.service;

import com.aidentx.iam.organization_management.model.entity.OrgUnitHistory;
import com.aidentx.iam.organization_management.model.enums.OrgEventType;
import com.aidentx.iam.organization_management.repository.OrgUnitHistoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Isolated service for writing org-unit history entries.
 * Uses REQUIRES_NEW so a history-write failure never rolls back the caller's transaction.
 */
@Service
public class OrgHistoryService {

    private final OrgUnitHistoryRepository historyRepository;

    public OrgHistoryService(OrgUnitHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String tenantId, UUID orgUnitId, OrgEventType type,
                       String label, String actor, JsonNode before, JsonNode after) {
        OrgUnitHistory h = new OrgUnitHistory();
        h.setTenantId(tenantId);
        h.setOrgUnitId(orgUnitId);
        h.setEventType(type);
        h.setActionLabel(label);
        h.setActorUsername(actor);
        h.setBeforeState(before);
        h.setAfterState(after);
        historyRepository.save(h);
    }
}
