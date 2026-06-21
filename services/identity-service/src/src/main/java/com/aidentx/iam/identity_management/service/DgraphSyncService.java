package com.aidentx.iam.identity_management.service;

import com.aidentx.iam.identity_management.model.entity.Identity;

public interface DgraphSyncService {

    /**
     * Creates or updates an Identity node in Dgraph.
     * 
     * @param identity The Identity (or any of its subclasses) to sync.
     * @return The Dgraph node UID.
     */
    String syncIdentityToGraph(Identity identity);

    /**
     * Removes an Identity node from Dgraph.
     * 
     * @param identity The Identity to remove.
     */
    void removeIdentityFromGraph(Identity identity);
}