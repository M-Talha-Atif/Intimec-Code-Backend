package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.Date;

/**
 * Represents an Employee identity.
 * This entity extends HumanIdentity and adds core attributes specific to
 * employees.
 * It is mapped to the 'employees' table.
 */
@Entity
@Table(name = "employees")
@DiscriminatorValue("EMPLOYEE") // This value is stored in the Identity's identity_type column
public class Employee extends HumanIdentity {

    // The primary key is inherited from the base Identity class.
    // JPA will automatically handle the one-to-one mapping using the same ID.

    @NotBlank
    @Column(name = "employee_id", nullable = false, unique = true)
    private String employeeId;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "department")
    private String department;

    // This is a polymorphic relationship. It can point to any Identity record,
    // regardless of its specific type (e.g., another Employee).
    @ManyToOne
    @JoinColumn(name = "manager_id", foreignKey = @ForeignKey(name = "fk_employee_manager"))
    private Identity manager;

    @Temporal(TemporalType.DATE)
    @Column(name = "hire_date")
    private Date hireDate;

    @Temporal(TemporalType.DATE)
    @Column(name = "termination_date")
    private Date terminationDate;

    /**
     * Raw manager reference from the connector (e.g., employeeId, DN, username).
     * Stored during import and resolved to the actual manager Identity entity
     * in a post-import resolution pass.
     */
    @Column(name = "manager_ref", length = 512)
    private String managerRef;

    // --- Getters and Setters ---

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Identity getManager() {
        return manager;
    }

    public void setManager(Identity manager) {
        this.manager = manager;
    }


    public Date getHireDate() {
        return hireDate;
    }

    public void setHireDate(Date hireDate) {
        this.hireDate = hireDate;
    }

    public Date getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(Date terminationDate) {
        this.terminationDate = terminationDate;
    }

    public String getManagerRef() {
        return managerRef;
    }

    public void setManagerRef(String managerRef) {
        this.managerRef = managerRef;
    }
}
