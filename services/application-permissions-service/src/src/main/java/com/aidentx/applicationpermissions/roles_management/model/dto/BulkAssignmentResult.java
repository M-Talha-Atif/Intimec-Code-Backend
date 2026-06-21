package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing the result of a bulk permission assignment operation.
 * 
 * <p>This response provides comprehensive information about the bulk operation:</p>
 * <ul>
 *   <li>Overall success/failure status</li>
 *   <li>Count of successful assignments</li>
 *   <li>List of created permission IDs</li>
 *   <li>Detailed error information if any failures occurred</li>
 *   <li>Execution timestamp for audit trail</li>
 * </ul>
 * 
 * <p>Success response example:</p>
 * <pre>
 * {
 *   "success": true,
 *   "assignedCount": 10,
 *   "totalRequested": 10,
 *   "permissionIds": ["uuid1", "uuid2", ...],
 *   "executionTime": "2026-01-07T10:35:22",
 *   "message": "Successfully assigned 10 permissions"
 * }
 * </pre>
 * 
 * <p>Failure response example:</p>
 * <pre>
 * {
 *   "success": false,
 *   "assignedCount": 0,
 *   "totalRequested": 10,
 *   "permissionIds": [],
 *   "errors": ["Action xyz not found", "Role is inactive"],
 *   "executionTime": "2026-01-07T10:35:22",
 *   "message": "Bulk assignment failed: Transaction rolled back"
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class BulkAssignmentResult {
    
    private boolean success;
    private int assignedCount;
    private int totalRequested;
    private List<UUID> permissionIds;
    private List<String> errors;
    private LocalDateTime executionTime;
    private String message;
    
    // Constructors
    public BulkAssignmentResult() {
        this.permissionIds = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.executionTime = LocalDateTime.now();
    }
    
    public BulkAssignmentResult(boolean success, int assignedCount, int totalRequested, String message) {
        this();
        this.success = success;
        this.assignedCount = assignedCount;
        this.totalRequested = totalRequested;
        this.message = message;
    }
    
    // Static factory methods for common scenarios
    
    /**
     * Create a successful result.
     * 
     * @param permissionIds list of created permission IDs
     * @param totalRequested total number of assignments requested
     * @return success result
     */
    public static BulkAssignmentResult success(List<UUID> permissionIds, int totalRequested) {
        BulkAssignmentResult result = new BulkAssignmentResult();
        result.setSuccess(true);
        result.setAssignedCount(permissionIds.size());
        result.setTotalRequested(totalRequested);
        result.setPermissionIds(permissionIds);
        result.setMessage("Successfully assigned " + permissionIds.size() + " permissions");
        return result;
    }
    
    /**
     * Create a failure result.
     * 
     * @param totalRequested total number of assignments requested
     * @param errors list of error messages
     * @return failure result
     */
    public static BulkAssignmentResult failure(int totalRequested, List<String> errors) {
        BulkAssignmentResult result = new BulkAssignmentResult();
        result.setSuccess(false);
        result.setAssignedCount(0);
        result.setTotalRequested(totalRequested);
        result.setErrors(errors);
        result.setMessage("Bulk assignment failed: " + String.join(", ", errors));
        return result;
    }
    
    /**
     * Create a failure result with single error.
     * 
     * @param totalRequested total number of assignments requested
     * @param error error message
     * @return failure result
     */
    public static BulkAssignmentResult failure(int totalRequested, String error) {
        BulkAssignmentResult result = new BulkAssignmentResult();
        result.setSuccess(false);
        result.setAssignedCount(0);
        result.setTotalRequested(totalRequested);
        result.getErrors().add(error);
        result.setMessage("Bulk assignment failed: " + error);
        return result;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public int getAssignedCount() {
        return assignedCount;
    }
    
    public void setAssignedCount(int assignedCount) {
        this.assignedCount = assignedCount;
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
        return "BulkAssignmentResult{" +
                "success=" + success +
                ", assignedCount=" + assignedCount +
                ", totalRequested=" + totalRequested +
                ", permissionIdsCount=" + permissionIds.size() +
                ", errorsCount=" + errors.size() +
                ", message='" + message + '\'' +
                '}';
    }
}
