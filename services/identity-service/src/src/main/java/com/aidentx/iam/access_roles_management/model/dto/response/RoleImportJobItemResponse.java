package com.aidentx.iam.access_roles_management.model.dto.response;

import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemPhase;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemStatus;

import java.time.Instant;
import java.util.UUID;

/** Per-row result response for an access-role import job. */
public class RoleImportJobItemResponse {
    private UUID itemId;
    private int rowNumber;
    private String externalRef;
    private RoleImportItemPhase phase;
    private RoleImportItemStatus status;
    private String displayStatus;
    private UUID roleId;
    private String roleName;
    private String roleType;
    private boolean roleCreated;
    private String errorCode;
    private String errorMessage;
    private String warningMessage;
    private String resultMessage;
    private Instant processedAt;

    public UUID getItemId() { return itemId; }
    public void setItemId(UUID itemId) { this.itemId = itemId; }
    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }
    public RoleImportItemPhase getPhase() { return phase; }
    public void setPhase(RoleImportItemPhase phase) { this.phase = phase; }
    public RoleImportItemStatus getStatus() { return status; }
    public void setStatus(RoleImportItemStatus status) { this.status = status; }
    public String getDisplayStatus() { return displayStatus; }
    public void setDisplayStatus(String displayStatus) { this.displayStatus = displayStatus; }
    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getRoleType() { return roleType; }
    public void setRoleType(String roleType) { this.roleType = roleType; }
    public boolean isRoleCreated() { return roleCreated; }
    public void setRoleCreated(boolean roleCreated) { this.roleCreated = roleCreated; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getWarningMessage() { return warningMessage; }
    public void setWarningMessage(String warningMessage) { this.warningMessage = warningMessage; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}

