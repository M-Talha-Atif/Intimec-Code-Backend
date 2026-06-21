package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Represents a Non-Human identity (e.g., service account, API key).
 * This entity extends the base Identity and adds core attributes specific to
 * non-human entities.
 * It is mapped to the 'non_human_identities' table.
 */
@Entity
@Table(name = "non_human_identities")
@DiscriminatorValue("NON_HUMAN")
public class NonHumanIdentity extends Identity {

    @NotBlank
    @Column(name = "service_name", nullable = false, unique = true)
    private String serviceName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "owner_team")
    private String ownerTeam;

    @Column(name = "risk_level")
    private String riskLevel;

    // --- Getters and Setters ---

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public void setOwnerTeam(String ownerTeam) {
        this.ownerTeam = ownerTeam;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}