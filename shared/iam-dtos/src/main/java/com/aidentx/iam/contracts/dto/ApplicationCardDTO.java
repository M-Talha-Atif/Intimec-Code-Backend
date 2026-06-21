package com.aidentx.iam.contracts.dto;

import java.util.UUID;

/**
 * A lightweight DTO for representing an application in a list or card view.
 */
public class ApplicationCardDTO {
    private UUID id;
    private String name;
    private boolean isAuthoritative;
    private String status; // A field that would be computed, e.g., "ACTIVE", "AGGREGATING"
    private String lifecycleStatus; // e.g., "ACTIVE", "DRAFT"
    private String operationalStatus; // e.g., "IDLE", "AGGREGATING"

    private String iconPath;

    public String geticonPath() {
        return iconPath;
    }

    public void seticonPath(String iconPath) {
        this.iconPath = iconPath;
    }

    // --- MODIFIED CONSTRUCTOR ---
    public ApplicationCardDTO(UUID id, String name, boolean isAuthoritative, String lifecycleStatus,
            String operationalStatus, String iconPath) {
        this.id = id;
        this.name = name;
        this.isAuthoritative = isAuthoritative;
        this.lifecycleStatus = lifecycleStatus;
        this.operationalStatus = operationalStatus;
        this.iconPath = iconPath;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAuthoritative() {
        return isAuthoritative;
    }

    public void setAuthoritative(boolean authoritative) {
        isAuthoritative = authoritative;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(String lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    public String getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(String operationalStatus) {
        this.operationalStatus = operationalStatus;
    }
}