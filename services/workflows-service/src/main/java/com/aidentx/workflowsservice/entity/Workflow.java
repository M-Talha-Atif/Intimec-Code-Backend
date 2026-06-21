package com.aidentx.workflowsservice.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Workflow Entity - Main workflow definitions
 * Stores workflow graph (nodes + edges) as JSONB for flexibility
 */
@Entity
@Table(name = "workflows", indexes = {
    @Index(name = "idx_workflows_status", columnList = "status"),
    @Index(name = "idx_workflows_category", columnList = "category"),
    @Index(name = "idx_workflows_created_by", columnList = "created_by"),
    @Index(name = "idx_workflows_template", columnList = "source_template_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "workflows_name_version_unique", columnNames = {"name", "version"})
})
@Check(constraints = "risk_score >= 0 AND risk_score <= 10")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private String version = "2.0";

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    // Metadata
    @Enumerated(EnumType.STRING)
    private WorkflowCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type")
    private TriggerType triggerType;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "event_config")
    private Map<String, Object> eventConfig;

    @Column(name = "cron_schedule")
    private String cronSchedule;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "application_scope")
    @Builder.Default
    private List<String> applicationScope = List.of();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = List.of();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "compliance_frameworks")
    @Builder.Default
    private List<String> complianceFrameworks = List.of();

    // Graph data (nodes and edges stored as JSONB)
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> nodes = List.of();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> edges = List.of();

    // Template relationship
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "template_metadata")
    private Map<String, Object> templateMetadata;

    @Column(name = "source_template_id")
    private UUID sourceTemplateId;

    @Column(name = "is_connected_to_template")
    @Builder.Default
    private Boolean isConnectedToTemplate = false;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    @Column(name = "drift_detected")
    @Builder.Default
    private Boolean driftDetected = false;

    @Column(name = "upstream_version")
    private String upstreamVersion;

    // Risk & compliance
    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "has_time_logic")
    @Builder.Default
    private Boolean hasTimeLogic = false;

    @Column(name = "has_sod_violation")
    @Builder.Default
    private Boolean hasSodViolation = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_sensitivity")
    private RoleSensitivity roleSensitivity;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_compliance")
    private PolicyCompliance policyCompliance;

    // Permissions
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Boolean> permissions = Map.of(
        "canEdit", true,
        "canDelete", true,
        "canExecute", true,
        "canShare", false
    );

    // Audit fields
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "last_modified_by")
    private UUID lastModifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        validateRiskScore();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
        validateRiskScore();
    }

    // Validation method to ensure risk score is in valid range
    private void validateRiskScore() {
        if (riskScore != null && (riskScore < 0 || riskScore > 10)) {
            throw new IllegalArgumentException("Risk score must be between 0 and 10");
        }
    }

    // Enums
    public enum WorkflowStatus {
        DRAFT, ACTIVE, DEPRECATED
    }

    public enum WorkflowCategory {
        JOINER_MOVER_LEAVER("joiner-mover-leaver"),
        ACCESS_REQUEST("access-request"),
        CERTIFICATION("certification"),
        REVOCATION("revocation"),
        EXCEPTION("exception"),
        AI_RISK("ai-risk");

        private final String value;
        WorkflowCategory(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    public enum TriggerType {
        EVENT_BASED("event-based"),
        TIME_BASED("time-based"),
        MANUAL("manual");

        private final String value;
        TriggerType(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    public enum RoleSensitivity {
        LOW, MEDIUM, HIGH
    }

    public enum PolicyCompliance {
        COMPLIANT, WARNING, VIOLATION
    }
}
