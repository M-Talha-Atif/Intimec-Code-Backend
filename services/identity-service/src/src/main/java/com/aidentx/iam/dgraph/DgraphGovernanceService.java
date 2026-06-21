package com.aidentx.iam.dgraph;

import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.role_management.model.entity.Role;

public interface DgraphGovernanceService {

    /**
     * Initializes the complete IGA schema in Dgraph, including all types and
     * predicates.
     * This method is idempotent and safe to run on startup.
     */
    void initializeSchema();

    /**
     * Creates or updates a BusinessRole node in Dgraph.
     * 
     * @param role The Role to sync.
     * @return The Dgraph node UID.
     */
    String syncRoleToGraph(Role role);

    /*
     * Syncs an Entitlement to Dgraph.
     */
    String syncEntitlementToGraph(Entitlement entitlement);

}