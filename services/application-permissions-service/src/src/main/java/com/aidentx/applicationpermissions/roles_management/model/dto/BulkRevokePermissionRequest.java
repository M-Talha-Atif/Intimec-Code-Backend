package com.aidentx.applicationpermissions.roles_management.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * DTO for bulk permission revocation operations.
 * 
 * <p>This request allows revoking multiple permissions from a role in a single
 * atomic transaction. All revocations must succeed, or the entire operation
 * will be rolled back to maintain data consistency.</p>
 * 
 * <p>Use cases:</p>
 * <ul>
 *   <li>Rapid role cleanup (revoke 10+ permissions at once)</li>
 *   <li>Role template adjustments (remove standard permission sets)</li>
 *   <li>Compliance-driven permission removal</li>
 * </ul>
 * 
 * <p>Example request:</p>
 * <pre>
 * {
 *   "actionIds": [
 *     "uuid1",
 *     "uuid2",
 *     "uuid3"
 *   ]
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class BulkRevokePermissionRequest {
    
    @NotEmpty(message = "Action IDs list cannot be empty")
    @Size(min = 1, max = 100, message = "Action IDs must contain between 1 and 100 items")
    private List<UUID> actionIds;
    
    // Constructors
    public BulkRevokePermissionRequest() {}
    
    public BulkRevokePermissionRequest(List<UUID> actionIds) {
        this.actionIds = actionIds;
    }
    
    // Getters and Setters
    public List<UUID> getActionIds() {
        return actionIds;
    }
    
    public void setActionIds(List<UUID> actionIds) {
        this.actionIds = actionIds;
    }
    
    /**
     * Get the number of actions to revoke.
     * 
     * @return count of action IDs
     */
    public int getRevocationCount() {
        return actionIds != null ? actionIds.size() : 0;
    }
    
    @Override
    public String toString() {
        return "BulkRevokePermissionRequest{" +
                "revocationCount=" + getRevocationCount() +
                '}';
    }
}
