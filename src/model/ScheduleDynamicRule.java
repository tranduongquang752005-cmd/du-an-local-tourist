package model;

import java.time.LocalDateTime;

public class ScheduleDynamicRule {
    private int scheduleRuleId;
    private int scheduleId;
    private int ruleId;
    private LocalDateTime createdAt;

    public ScheduleDynamicRule() {
    }


    public ScheduleDynamicRule(int scheduleRuleId, int scheduleId, int ruleId, LocalDateTime createdAt) {
        this.scheduleRuleId = scheduleRuleId;
        this.scheduleId = scheduleId;
        this.ruleId = ruleId;
        this.createdAt = createdAt;
    }


    public int getScheduleRuleId() {
        return scheduleRuleId;
    }

    public void setScheduleRuleId(int scheduleRuleId) {
        this.scheduleRuleId = scheduleRuleId;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ScheduleDynamicRule{scheduleRuleId=" + scheduleRuleId +
                ", scheduleId=" + scheduleId +
                ", ruleId=" + ruleId +
                ", createdAt=" + createdAt +
                '}';
    }
}
