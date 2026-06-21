package com.aidentx.access_portal_service.repository.tile;

import com.aidentx.access_portal_service.entity.tile.TileVisualFilterRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing individual visual filter rules.
 */
@Repository
public interface TileVisualFilterRuleRepository extends JpaRepository<TileVisualFilterRule, UUID> {

    List<TileVisualFilterRule> findByFilterId(UUID filterId);

    void deleteByFilterId(UUID filterId);
}
