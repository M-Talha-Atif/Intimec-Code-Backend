package com.aidentx.core.responsibilities.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for GET /api/v1/responsibilities/analytics/kpis.
 *
 * <p>Returns the three real KPI values displayed at the top of the
 * Responsibilities Analytics Dashboard. All other KPI cards on the
 * dashboard use data already fetched client-side from individual endpoints
 * (tasks, team, policies, apps) and are computed in the frontend.
 *
 * <p>This endpoint aggregates three separate service calls into one
 * response so the dashboard loads the real KPI numbers in a single
 * round trip.
 *
 * <p>Example response:
 * <pre>
 * {
 *   "totalResponsibilities": 18,
 *   "pendingTasks":           4,
 *   "ownedRoles":             6,
 *   "ownedRolesTotalMembers": 84
 * }
 * </pre>
 *
 * <p>totalResponsibilities = pendingTasks count + all tasks count
 *    + owned roles count -- matching the frontend formula:
 *    tasks.length + internalRoles.length
 */
public class ResponsibilitiesKpisResponse {

    /**
     * Total governance objects under ownership.
     * Computed as: total tasks count + owned internal roles count.
     * Matches the frontend formula: tasks.length + internalRoles.length.
     */
    @JsonProperty("totalResponsibilities")
    private int totalResponsibilities;

    /**
     * Number of workflow approval tasks currently in pending status.
     * Matches: tasks.filter(t => t.status === 'pending').length
     */
    @JsonProperty("pendingTasks")
    private int pendingTasks;

    /**
     * Number of internal roles owned by the current user.
     * Fetched from application-permissions-service via GET /api/v1/roles/owned-by-me.
     * Matches: internalRoles.length
     */
    @JsonProperty("ownedRoles")
    private int ownedRoles;

    /**
     * Total member count across all owned roles.
     * Sum of userCount across all owned internal roles.
     * Matches: internalRoles.reduce((s, r) => s + (r.userCount ?? 0), 0)
     * Used as the sub-label on the Owned Roles KPI card.
     */
    @JsonProperty("ownedRolesTotalMembers")
    private int ownedRolesTotalMembers;

    public ResponsibilitiesKpisResponse() {}

    public ResponsibilitiesKpisResponse(
            int totalResponsibilities,
            int pendingTasks,
            int ownedRoles,
            int ownedRolesTotalMembers) {
        this.totalResponsibilities  = totalResponsibilities;
        this.pendingTasks           = pendingTasks;
        this.ownedRoles             = ownedRoles;
        this.ownedRolesTotalMembers = ownedRolesTotalMembers;
    }

    public int getTotalResponsibilities()                          { return totalResponsibilities; }
    public void setTotalResponsibilities(int totalResponsibilities){ this.totalResponsibilities = totalResponsibilities; }

    public int getPendingTasks()                   { return pendingTasks; }
    public void setPendingTasks(int pendingTasks)  { this.pendingTasks = pendingTasks; }

    public int getOwnedRoles()                     { return ownedRoles; }
    public void setOwnedRoles(int ownedRoles)      { this.ownedRoles = ownedRoles; }

    public int getOwnedRolesTotalMembers()                           { return ownedRolesTotalMembers; }
    public void setOwnedRolesTotalMembers(int ownedRolesTotalMembers){ this.ownedRolesTotalMembers = ownedRolesTotalMembers; }
}