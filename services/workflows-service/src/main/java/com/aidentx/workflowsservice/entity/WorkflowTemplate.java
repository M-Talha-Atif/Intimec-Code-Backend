package com.aidentx.workflowsservice.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WorkflowTemplate Entity - Reusable workflow templates
 * Supports compliance frameworks (NIST, ISO 27001, SOC 2)
 */
@Entity
@Table(name = "workflow_templates", indexes = {
    @Index(name = "idx_templates_category", columnList = "category"),
    @Index(name = "idx_templates_framework", columnList = "framework")
}, uniqueConstraints = {
    @UniqueConstraint(name = "workflow_templates_name_unique", columnNames = {"name"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String category;

    @Column
    private String framework; // 'NIST', 'ISO 27001', 'SOC 2', etc.

    @Enumerated(EnumType.STRING)
    private Complexity complexity;

    // Template content
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> nodes = List.of();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Map<String, Object>> edges = List.of();

    // Metadata
    @Column(nullable = false)
    @Builder.Default
    private String version = "1.0";

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = List.of();

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(precision = 2, scale = 1)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column
    @Builder.Default
    private Integer downloads = 0;

    @Column
    @Builder.Default
    private Integer steps = 0;

    @Column(name = "estimated_time")
    private String estimatedTime;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "compliance_frameworks")
    @Builder.Default
    private List<String> complianceFrameworks = List.of();

    // Prerequisites & variables
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> prerequisites = List.of();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> variables = Map.of();

    // Audit fields
    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "is_system_template")
    @Builder.Default
    private Boolean isSystemTemplate = false;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public enum Complexity {
        SIMPLE, MEDIUM, COMPLEX
    }
}
