package com.aidentx.iam.access_management.model.dto.response;

import java.math.BigDecimal;

/**
 * Single access KPI card value returned to the frontend.
 *
 * <p>Delta fields are nullable because this first implementation computes live
 * current-state KPIs only. Once a snapshot table or scheduled job is introduced,
 * the same DTO can carry previous-period deltas without changing the API shape.</p>
 */
public class AccessKpiMetricDto {

    private BigDecimal value;
    private String unit;
    private BigDecimal delta;
    private String deltaDirection;
    private String periodLabel;

    public AccessKpiMetricDto() {
    }

    public AccessKpiMetricDto(BigDecimal value,
                              String unit,
                              BigDecimal delta,
                              String deltaDirection,
                              String periodLabel) {
        this.value = value;
        this.unit = unit;
        this.delta = delta;
        this.deltaDirection = deltaDirection;
        this.periodLabel = periodLabel;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getDelta() {
        return delta;
    }

    public void setDelta(BigDecimal delta) {
        this.delta = delta;
    }

    public String getDeltaDirection() {
        return deltaDirection;
    }

    public void setDeltaDirection(String deltaDirection) {
        this.deltaDirection = deltaDirection;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public void setPeriodLabel(String periodLabel) {
        this.periodLabel = periodLabel;
    }
}

