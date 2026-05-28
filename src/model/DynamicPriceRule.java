package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DynamicPriceRule {
    private int ruleId;
    private String ruleName;
    private String conditionType;
    private BigDecimal modifierPercent;
    private LocalDate startDate;
    private LocalDate endDate;
    private int priority;
    private boolean active;
    private LocalDateTime createdAt;

    public DynamicPriceRule() {
    }


    public DynamicPriceRule(int ruleId, String ruleName, String conditionType, BigDecimal modifierPercent, LocalDate startDate, LocalDate endDate, int priority, boolean active, LocalDateTime createdAt) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.conditionType = conditionType;
        this.modifierPercent = modifierPercent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.priority = priority;
        this.active = active;
        this.createdAt = createdAt;
    }


    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getConditionType() {
        return conditionType;
    }

    public void setConditionType(String conditionType) {
        this.conditionType = conditionType;
    }

    public BigDecimal getModifierPercent() {
        return modifierPercent;
    }

    public void setModifierPercent(BigDecimal modifierPercent) {
        this.modifierPercent = modifierPercent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "DynamicPriceRule{ruleId=" + ruleId +
                ", ruleName=" + ruleName +
                ", conditionType=" + conditionType +
                ", modifierPercent=" + modifierPercent +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", priority=" + priority +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
