package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing the result of a bulk permission revocation operation.
 * 
 * <p>This response provides comprehensive information about the bulk revocation:</p>
 * <ul>
 *   <li>Overall success/failure status</li>
 *   <li>Count of successful revocations</li>
 *   <li>List of revoked permission IDs</li>
 *   <li>Detailed error information if any failures occurred</li>
 *   <li>Execution timestamp for audit trail</li>
 * </ul>
 * 
 * <p>Success response example:</p>
 * <pre>
 * {
 *   "success": true,
 *   "revokedCount": 10,
 *   "totalRequested": 10,
 *   "permissionIds": ["uuid1", "uuid2", ...],
 *   "executionTime": "2026-01-08T20:35:22",
 *   "message": "Successfully revoked 10 permissions"
 * }
 * </pre>
 * 
 * <p>Failure response example:</p>
 * <pre>
 * {
 *   "success": false,
 *   "revokedCount": 0,
 *   "totalRequested": 10,
 *   "permissionIds": [],
 *   "errors": ["Permission not found for action xyz"],
 *   "executionTime": "2026-01-08T20:35:22",
 *   "message": "Bulk revocation failed: Transaction rolled back"
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class BulkRevocationResult {
    
    private boolean success;
    private int revokedCount;
    private int totalRequested;
    private List<UUID> permissionIds;
    private List<String> errors;
    private LocalDateTime executionTime;
    private String message;
    
    // Constructors
    public BulkRevocationResult() {
        this.permissionIds = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.executionTime = LocalDateTime.now();
    }
    
    public BulkRevocationResult(boolean success, int revokedCount, int totalRequested, String message) {
        this();
        this.success = success;
        this.revokedCount = revokedCount;
        this.totalRequested = totalRequested;
        this.message = message;
    }
    
    // Static factory methods
    
    /**
     * Create a successful result.
     * 
     * @param permissionIds list of revoked permission IDs
     * @param totalRequested total number of revocations requested
     * @return success result
     */
    public static BulkRevocationResult success(List<UUID> permissionIds, int totalRequested) {
        BulkRevocationResult result = new BulkRevocationResult();
        result.setSuccess(true);
        result.setRevokedCount(permissionIds.size());
        result.setTotalRequested(totalRequested);
        result.setPermissionIds(permissionIds);
        result.setMessage("Successfully revoked " + permissionIds.size() + " permissions");
        return result;
    }
    
    /**
     * Create a failure result.
     * 
     * @param totalRequested total number of revocations requested
     * @param errorMessage error description
     * @return failure result
     */
    public static BulkRevocationResult failure(int totalRequested, String errorMessage) {
        BulkRevocationResult result = new BulkRevocationResult();
        result.setSuccess(false);
        result.setRevokedCount(0);
        result.setTotalRequested(totalRequested);
        result.getErrors().add(errorMessage);
        result.setMessage("Bulk revocation failed: " + errorMessage);
        return result;
    }
    
    /**
     * Create a failure result with multiple errors.
     * 
     * @param totalRequested total number of revocations requested
     * @param errors list of error messages
     * @return failure result
     */
    public static BulkRevocationResult failure(int totalRequested, List<String> errors) {
        BulkRevocationResult result = new BulkRevocationResult();
        result.setSuccess(false);
        result.setRevokedCount(0);
        result.setTotalRequested(totalRequested);
        result.setErrors(errors);
        result.setMessage("Bulk revocation failed: " + errors.size() + " errors occurred");
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getRevokedCount() {
        return revokedCount;
    }
    
    public void setRevokedCount(int revokedCount) {
        this.revokedCount = revokedCount;
    }
    
    public int getTotalRequested() {
        return totalRequested;
    }
    
    public void setTotalRequested(int totalRequested) {
        this.totalRequested = totalRequested;
    }
    
    public List<UUID> getPermissionIds() {
        return permissionIds;
    }
    
    public void setPermissionIds(List<UUID> permissionIds) {
        this.permissionIds = permissionIds;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
    
    public LocalDateTime getExecutionTime() {
        return executionTime;
    }
    
    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        return "BulkRevocationResult{" +
                "success=" + success +
                ", revokedCount=" + revokedCount +
                ", totalRequested=" + totalRequested +
                ", errors=" + errors.size() +
                '}';
    }
}
