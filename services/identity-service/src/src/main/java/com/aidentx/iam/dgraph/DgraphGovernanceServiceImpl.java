package com.aidentx.iam.dgraph;

import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.role_management.model.entity.Role;
import com.aidentx.shared.dgraph.DgraphService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DgraphGovernanceServiceImpl implements DgraphGovernanceService {

    private final DgraphService dgraphService;

    public DgraphGovernanceServiceImpl(DgraphService dgraphService) {
        this.dgraphService = dgraphService;
    }

    @Override
    public void initializeSchema() {
        String completeSchema = """
                # --- NODE TYPE DEFINITIONS ---
                type Identity {
                    identity.id
                    identity.name
                    identity.email
                    identity.status
                    has_role
                    has_entitlement
                    reports_to
                }
                type BusinessRole {
                    role.id
                    role.name
                    includes
                }
                type TechnicalEntitlement {
                    entitlement.id
                    entitlement.name
                    on_resource
                    grants
                }
                # --- PREDICATE DEFINITIONS & INDEXING ---
                identity.id: string @index(exact) .
                identity.name: string @index(term, fulltext) .
                identity.email: string @index(exact) .
                identity.status: string @index(exact) .
                role.id: string @index(exact) .
                role.name: string @index(term) .
                entitlement.id: string @index(exact) .
                entitlement.name: string @index(term) .
                permission.id: string @index(exact) .
                resource.id: string @index(exact) .
                # --- RELATIONSHIP PREDICATES (THE EDGES) ---
                has_role: [uid] @reverse .
                has_entitlement: [uid] @reverse .
                includes: [uid] @reverse .
                grants: [uid] @reverse .
                on_resource: [uid] @reverse .
                reports_to: uid @reverse .
                associated_with_risk: [uid] @reverse .
                applies_to: [uid] @reverse .
                """;
        dgraphService.applySchema(completeSchema);
    }

    @Override
    public String syncRoleToGraph(Role role) {
        Map<String, Object> roleNode = new HashMap<>();
        if (role.getDgraphUid() != null) {
            roleNode.put("uid", role.getDgraphUid()); // Use existing UID for updates
        }
        roleNode.put("dgraph.type", "BusinessRole");
        roleNode.put("role.id", role.getId().toString());
        roleNode.put("role.name", role.getDisplayName());

        return dgraphService.saveNode(roleNode);
    }

    @Override
    public String syncEntitlementToGraph(Entitlement entitlement) {
        Map<String, Object> entitlementNode = new HashMap<>();
        if (entitlement.getDgraphUid() != null) {
            entitlementNode.put("uid", entitlement.getDgraphUid());
        }
        entitlementNode.put("dgraph.type", "TechnicalEntitlement");
        entitlementNode.put("entitlement.id", entitlement.getId().toString());
        entitlementNode.put("entitlement.name", entitlement.getDisplayName());

        return dgraphService.saveNode(entitlementNode);
    }
}