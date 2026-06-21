package com.aidentx.iam.identity_management.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aidentx.iam.identity_management.model.entity.Shard;
import com.aidentx.iam.identity_management.model.enums.ShardStatus;

public interface ShardRepository extends JpaRepository<Shard, UUID> {

  /**
   * Find shards by tenantId, resourceId, and objectUid.
   * 
   * @param tenantId
   * @param resourceId
   * @param objectUid
   * @return List of Shards matching the given tenantId, resourceId, and objectUid
   */
  List<Shard> findByTenantIdAndResourceIdAndObjectUid(String tenantId, String resourceId, String objectUid);

  /**
   * Find shards by correlated identity ID.
   * 
   * @param correlatedIdentityId
   * @return List of Shards correlated to the given identity ID
   */
  List<Shard> findByCorrelatedIdentityId(UUID correlatedIdentityId);

  /**
   * Find shards by tenantId and resourceId.
   * 
   * @param tenantId
   * @param resourceId
   * @return List of Shards matching the given tenantId and resourceId
   */
  List<Shard> findByTenantIdAndResourceId(String tenantId, String resourceId);

  /**
   * 
   * Find shards by tenantId and status.
   * 
   * @param tenantId
   * @param status
   * @return List of Shards matching the given tenantId and status
   */
  List<Shard> findByTenantIdAndShardStatus(String tenantId, ShardStatus status);

  /** Delete all shards for a resource within a tenant (cascade delete on application removal). */
  @Modifying
  @Query("DELETE FROM Shard s WHERE s.tenantId = :tenantId AND s.resourceId = :resourceId")
  void deleteAllByTenantIdAndResourceId(@Param("tenantId") String tenantId, @Param("resourceId") String resourceId);

}
