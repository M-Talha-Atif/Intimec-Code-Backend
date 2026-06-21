package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.Date;

/**
 * Represents a Non-Employee identity (e.g., contractor, consultant).
 * This entity extends HumanIdentity and adds core attributes specific to
 * non-employees.
 * It is mapped to the 'non_employees' table.
 */
@Entity
@Table(name = "non_employees")
@DiscriminatorValue("NON_EMPLOYEE")
public class NonEmployee extends HumanIdentity {

    @NotBlank
    @Column(name = "contract_id", nullable = false, unique = true)
    private String contractId;

    @Column(name = "vendor_name")
    private String vendorName;

    @Temporal(TemporalType.DATE)
    @Column(name = "contract_end_date")
    private Date contractEndDate;

    // The internal employee who is responsible for this non-employee.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sponsor_id", foreignKey = @ForeignKey(name = "fk_non_employee_sponsor"))
    private Identity sponsor;

    // --- Getters and Setters ---

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }

    public Date getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(Date contractEndDate) {
        this.contractEndDate = contractEndDate;
    }

    public Identity getSponsor() {
        return sponsor;
    }

    public void setSponsor(Identity sponsor) {
        this.sponsor = sponsor;
    }
}