package com.aidentx.authservice.model.entity;

import com.aidentx.authservice.utils.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing AI-driven security metrics for a user.
 * <p>
 * This class maps to the <code>user_ai_metrics</code> table and stores AI-generated risk scores,
 * behavioral profiles, anomaly detection flags, and analysis results for each user. These metrics
 * can be used for advanced security monitoring, anomaly detection, and adaptive authentication.
 * </p>
 *
 * <p>Typical usage involves updating or querying AI metrics for a user to inform security decisions
 * or provide insights in the admin UI.</p>
 *
 * @author aidentx
 * @since 1.0
 */
@Entity
@Table(name = "user_ai_metrics")
public class UserAiMetrics {

    /**
     * Unique identifier for the AI metrics record.
     */
    @Id
    private UUID id;

    /**
     * The user associated with these AI metrics.
     */
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * AI-generated risk score for the user (e.g., 0.0000 to 1.0000).
     */
    @Column(name = "ai_risk_score", precision = 5, scale = 4)
    private BigDecimal aiRiskScore;

    /**
     * AI-generated behavioral profile for the user, stored as JSON.
     */
    @Column(name = "ai_behavior_profile", columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode aiBehaviorProfile;

    /**
     * Indicates whether an AI-detected anomaly is present for the user.
     */
    @Column(name = "ai_anomaly_detected")
    private Boolean aiAnomalyDetected;

    /**
     * Additional AI analysis results, stored as JSON.
     */
    @Column(name = "ai_analysis", columnDefinition = "jsonb")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode aiAnalysis;

    /**
     * Default constructor for frameworks that require it.
     */
    public UserAiMetrics() {
    }

    /**
     * Constructs a UserAiMetrics with all fields specified.
     *
     * @param id unique identifier for the AI metrics record
     * @param user the user associated with these metrics
     * @param aiRiskScore AI-generated risk score
     * @param aiBehaviorProfile AI-generated behavioral profile as JSON
     * @param aiAnomalyDetected whether an anomaly was detected
     * @param aiAnalysis additional AI analysis as JSON
     */
    public UserAiMetrics(UUID id, User user, BigDecimal aiRiskScore, JsonNode aiBehaviorProfile,
                         Boolean aiAnomalyDetected, JsonNode aiAnalysis) {
        this.id = id;
        this.user = user;
        this.aiRiskScore = aiRiskScore;
        this.aiBehaviorProfile = aiBehaviorProfile;
        this.aiAnomalyDetected = aiAnomalyDetected;
        this.aiAnalysis = aiAnalysis;
    }

    // Getters and setters

    /**
     * Gets the unique identifier for the AI metrics record.
     *
     * @return the metrics record ID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Sets the unique identifier for the AI metrics record.
     *
     * @param id the metrics record ID
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * Gets the user associated with these AI metrics.
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user associated with these AI metrics.
     *
     * @param user the user
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Gets the AI-generated risk score for the user.
     *
     * @return the risk score
     */
    public BigDecimal getAiRiskScore() {
        return aiRiskScore;
    }

    /**
     * Sets the AI-generated risk score for the user.
     *
     * @param aiRiskScore the risk score
     */
    public void setAiRiskScore(BigDecimal aiRiskScore) {
        this.aiRiskScore = aiRiskScore;
    }

    /**
     * Gets the AI-generated behavioral profile for the user as JSON.
     *
     * @return the behavioral profile as a JsonNode
     */
    public JsonNode getAiBehaviorProfile() {
        return aiBehaviorProfile;
    }

    /**
     * Sets the AI-generated behavioral profile for the user as JSON.
     *
     * @param aiBehaviorProfile the behavioral profile as a JsonNode
     */
    public void setAiBehaviorProfile(JsonNode aiBehaviorProfile) {
        this.aiBehaviorProfile = aiBehaviorProfile;
    }

    /**
     * Gets whether an AI-detected anomaly is present for the user.
     *
     * @return true if an anomaly was detected, false otherwise
     */
    public Boolean getAiAnomalyDetected() {
        return aiAnomalyDetected;
    }

    /**
     * Sets whether an AI-detected anomaly is present for the user.
     *
     * @param aiAnomalyDetected true if an anomaly was detected, false otherwise
     */
    public void setAiAnomalyDetected(Boolean aiAnomalyDetected) {
        this.aiAnomalyDetected = aiAnomalyDetected;
    }

    /**
     * Gets additional AI analysis results as JSON.
     *
     * @return the AI analysis as a JsonNode
     */
    public JsonNode getAiAnalysis() {
        return aiAnalysis;
    }

    /**
     * Sets additional AI analysis results as JSON.
     *
     * @param aiAnalysis the AI analysis as a JsonNode
     */
    public void setAiAnalysis(JsonNode aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }
}
