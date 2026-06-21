package com.aidentx.iam.identity_management.service.impl;

import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.service.DgraphSyncService;
import com.aidentx.shared.dgraph.DgraphService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DgraphSyncServiceImpl implements DgraphSyncService {

    private final DgraphService dgraphService;

    public DgraphSyncServiceImpl(DgraphService dgraphService) {
        this.dgraphService = dgraphService;
    }

    @Override
    public String syncIdentityToGraph(Identity identity) {
        // Step 1: Create a map and populate it with common attributes from the base
        // Identity class.
        Map<String, Object> identityNode = new HashMap<>();

        // Use "uid" for updates, or let Dgraph assign one for creates.
        if (identity.getDgraphNodeId() != null && !identity.getDgraphNodeId().isBlank()) {
            identityNode.put("uid", identity.getDgraphNodeId());
        }

        identityNode.put("dgraph.type", "Identity"); // Base type for all nodes
        identityNode.put("identity.id", identity.getId().toString());
        identityNode.put("identity.tenant_id", identity.getTenantId());
        identityNode.put("identity.status", identity.getStatus().name());

        // Step 2: Use instanceof to check the specific subclass and add type-specific
        // attributes.
        if (identity instanceof HumanIdentity human) {
            // Add attributes common to all human identities
            identityNode.put("identity.username", human.getUsername());
            identityNode.put("identity.email", human.getEmail());
            identityNode.put("identity.display_name", human.getDisplayName());

            // Further specialize for Employee
            if (identity instanceof Employee employee) {
                identityNode.put("dgraph.type", "Identity, Employee"); // Add specific type
                identityNode.put("employee.id", employee.getEmployeeId());
                identityNode.put("employee.job_title", employee.getJobTitle());
                identityNode.put("employee.department", employee.getDepartment());
            }
            // Add another block here for NonEmployee if needed

        } else if (identity instanceof NonHumanIdentity nonHuman) {
            // Add attributes for non-human identities
            identityNode.put("dgraph.type", "Identity, ServiceAccount"); // Use a specific type
            identityNode.put("service.name", nonHuman.getServiceName());
            identityNode.put("service.owner_team", nonHuman.getOwnerTeam());
        }

        // Step 3: Call the generic shared service to save the node.
        return dgraphService.saveNode(identityNode);
    }

    @Override
    public void removeIdentityFromGraph(Identity identity) {
        if (identity.getDgraphNodeId() != null && !identity.getDgraphNodeId().isBlank()) {
            dgraphService.deleteNode(identity.getDgraphNodeId());
        }
    }
}