package com.aidentx.iam.identity_management.repository;

import com.aidentx.iam.identity_management.model.entity.IdentityShard;
import com.aidentx.iam.identity_management.model.entity.IdentityShardId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IdentityShardRepository
        extends JpaRepository<IdentityShard, IdentityShardId> {

    List<IdentityShard> findByIdentityId(UUID identityId);

    /** Delete all identity shards for a resource (cascade delete on application removal). */
    @Modifying
    @Query("DELETE FROM IdentityShard s WHERE s.resourceId = :resourceId")
    void deleteAllByResourceId(@Param("resourceId") UUID resourceId);
}