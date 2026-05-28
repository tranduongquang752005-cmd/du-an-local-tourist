package model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SystemConfig {
    private String configKey;
    private String configValue;
    private String valueType;
    private String description;
    private boolean active;
    private Integer updatedById;
    private LocalDateTime updatedAt;

    public SystemConfig() {
    }


    public SystemConfig(String configKey, String configValue, String valueType, String description, boolean active, Integer updatedById, LocalDateTime updatedAt) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.valueType = valueType;
        this.description = description;
        this.active = active;
        this.updatedById = updatedById;
        this.updatedAt = updatedAt;
    }


    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(Integer updatedById) {
        this.updatedById = updatedById;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getIntValue() {
        return configValue == null ? null : Integer.parseInt(configValue);
    }

    public Boolean getBoolValue() {
        if (configValue == null) {
            return null;
        }
        return "true".equalsIgnoreCase(configValue) || "1".equals(configValue);
    }

    public BigDecimal getDecimalValue() {
        return configValue == null ? null : new BigDecimal(configValue);
    }

    @Override
    public String toString() {
        return "SystemConfig{configKey=" + configKey +
                ", configValue=" + configValue +
                ", valueType=" + valueType +
                ", description=" + description +
                ", active=" + active +
                ", updatedById=" + updatedById +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
