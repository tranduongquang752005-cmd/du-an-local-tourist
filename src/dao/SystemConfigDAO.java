package dao;

import config.DatabaseConnection;
import model.SystemConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class SystemConfigDAO {

    private static final String TYPE_INT = "INT";
    private static final String TYPE_DECIMAL = "DECIMAL";
    private static final String TYPE_BOOL = "BOOL";
    private static final String TYPE_STRING = "STRING";
    private static final String TYPE_JSON = "JSON";

    private static final int MAX_CONFIG_KEY_LENGTH = 100;
    private static final int MAX_CONFIG_VALUE_LENGTH = 1000;
    private static final int MAX_VALUE_TYPE_LENGTH = 20;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    public SystemConfig getConfigByKey(String configKey) {
        String cleanKey = normalizeConfigKey(configKey);

        if (cleanKey == null) {
            System.out.println("ConfigKey khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE ConfigKey = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanKey);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSystemConfig(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getConfigByKey");
        }

        return null;
    }

    public List<SystemConfig> getAllConfigs() {
        String sql = buildSelectSql("""
                ORDER BY ConfigKey ASC
                """);

        return queryConfigList(sql, null, "getAllConfigs");
    }

    public List<SystemConfig> getActiveConfigs() {
        String sql = buildSelectSql("""
                WHERE IsActive = 1
                ORDER BY ConfigKey ASC
                """);

        return queryConfigList(sql, null, "getActiveConfigs");
    }

    public int createConfig(String configKey,
                            String configValue,
                            String valueType,
                            String description,
                            Integer updatedById) {
        ConfigInput input = validateConfigInput(
                configKey,
                configValue,
                valueType,
                description,
                updatedById
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (getConfigByKey(input.configKey) != null) {
            System.out.println("ConfigKey da ton tai.");
            return -1;
        }

        if (input.updatedById != null && !isUserExists(input.updatedById)) {
            System.out.println("UpdatedByID khong ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO SYSTEM_CONFIG
                (
                    ConfigKey,
                    ConfigValue,
                    ValueType,
                    Description,
                    IsActive,
                    UpdatedByID
                )
                VALUES (?, ?, ?, ?, 1, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, input.configKey);
            ps.setString(2, input.configValue);
            ps.setString(3, input.valueType);
            ps.setString(4, input.description);
            setNullableInt(ps, 5, input.updatedById);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                return 1;
            }

        } catch (SQLException e) {
            handleException(e, "createConfig");
        }

        return -1;
    }

    public boolean updateConfigValue(String configKey,
                                     String configValue,
                                     Integer updatedById) {
        String cleanKey = normalizeConfigKey(configKey);

        if (cleanKey == null) {
            System.out.println("ConfigKey khong hop le.");
            return false;
        }

        SystemConfig current = getConfigByKey(cleanKey);

        if (current == null) {
            System.out.println("Khong tim thay config.");
            return false;
        }

        String cleanValue = cleanString(configValue);

        if (!isValidValueByType(cleanValue, current.getValueType())) {
            System.out.println("ConfigValue khong hop le theo ValueType = " + current.getValueType());
            return false;
        }

        if (cleanValue != null && cleanValue.length() > MAX_CONFIG_VALUE_LENGTH) {
            System.out.println("ConfigValue qua dai, toi da 1000 ky tu.");
            return false;
        }

        if (updatedById != null && !isUserExists(updatedById)) {
            System.out.println("UpdatedByID khong ton tai.");
            return false;
        }

        String sql = """
                UPDATE SYSTEM_CONFIG
                SET ConfigValue = ?,
                    UpdatedByID = ?,
                    UpdatedAt = GETDATE()
                WHERE ConfigKey = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanValue);
            setNullableInt(ps, 2, updatedById);
            ps.setString(3, cleanKey);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat config that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateConfigValue");
        }

        return false;
    }

    public boolean updateConfig(String configKey,
                                String configValue,
                                String valueType,
                                String description,
                                boolean active,
                                Integer updatedById) {
        String cleanKey = normalizeConfigKey(configKey);

        if (cleanKey == null) {
            System.out.println("ConfigKey khong hop le.");
            return false;
        }

        if (getConfigByKey(cleanKey) == null) {
            System.out.println("Khong tim thay config.");
            return false;
        }

        ConfigInput input = validateConfigInput(
                cleanKey,
                configValue,
                valueType,
                description,
                updatedById
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        if (input.updatedById != null && !isUserExists(input.updatedById)) {
            System.out.println("UpdatedByID khong ton tai.");
            return false;
        }

        String sql = """
                UPDATE SYSTEM_CONFIG
                SET ConfigValue = ?,
                    ValueType = ?,
                    Description = ?,
                    IsActive = ?,
                    UpdatedByID = ?,
                    UpdatedAt = GETDATE()
                WHERE ConfigKey = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, input.configValue);
            ps.setString(2, input.valueType);
            ps.setString(3, input.description);
            ps.setBoolean(4, active);
            setNullableInt(ps, 5, input.updatedById);
            ps.setString(6, cleanKey);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleException(e, "updateConfig");
        }

        return false;
    }

    public boolean activateConfig(String configKey, Integer updatedById) {
        return updateActiveStatus(configKey, true, updatedById);
    }

    public boolean deactivateConfig(String configKey, Integer updatedById) {
        return updateActiveStatus(configKey, false, updatedById);
    }

    public String getStringValue(String configKey, String defaultValue) {
        SystemConfig config = getActiveConfig(configKey);

        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }

        return config.getConfigValue();
    }

    public int getIntValue(String configKey, int defaultValue) {
        SystemConfig config = getActiveConfig(configKey);

        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(config.getConfigValue().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanValue(String configKey, boolean defaultValue) {
        SystemConfig config = getActiveConfig(configKey);

        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }

        String value = config.getConfigValue().trim().toLowerCase();

        if ("true".equals(value) || "1".equals(value) || "yes".equals(value)) {
            return true;
        }

        if ("false".equals(value) || "0".equals(value) || "no".equals(value)) {
            return false;
        }

        return defaultValue;
    }

    public double getDecimalValue(String configKey, double defaultValue) {
        SystemConfig config = getActiveConfig(configKey);

        if (config == null || config.getConfigValue() == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(config.getConfigValue().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private SystemConfig getActiveConfig(String configKey) {
        String cleanKey = normalizeConfigKey(configKey);

        if (cleanKey == null) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE ConfigKey = ?
                  AND IsActive = 1
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanKey);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSystemConfig(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getActiveConfig");
        }

        return null;
    }

    private boolean updateActiveStatus(String configKey, boolean active, Integer updatedById) {
        String cleanKey = normalizeConfigKey(configKey);

        if (cleanKey == null) {
            System.out.println("ConfigKey khong hop le.");
            return false;
        }

        if (updatedById != null && !isUserExists(updatedById)) {
            System.out.println("UpdatedByID khong ton tai.");
            return false;
        }

        String sql = """
                UPDATE SYSTEM_CONFIG
                SET IsActive = ?,
                    UpdatedByID = ?,
                    UpdatedAt = GETDATE()
                WHERE ConfigKey = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBoolean(1, active);
            setNullableInt(ps, 2, updatedById);
            ps.setString(3, cleanKey);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay config.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateActiveStatus");
        }

        return false;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    ConfigKey,
                    ConfigValue,
                    ValueType,
                    Description,
                    IsActive,
                    UpdatedByID,
                    UpdatedAt
                FROM SYSTEM_CONFIG
                """ + condition;
    }

    private List<SystemConfig> queryConfigList(String sql,
                                               SqlSetter setter,
                                               String methodName) {
        List<SystemConfig> configs = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    configs.add(mapSystemConfig(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return configs;
    }

    private SystemConfig mapSystemConfig(ResultSet rs) throws SQLException {
        SystemConfig config = new SystemConfig();

        config.setConfigKey(rs.getString("ConfigKey"));
        config.setConfigValue(rs.getString("ConfigValue"));
        config.setValueType(rs.getString("ValueType"));
        config.setDescription(rs.getString("Description"));
        config.setActive(rs.getBoolean("IsActive"));

        int updatedById = rs.getInt("UpdatedByID");
        config.setUpdatedById(rs.wasNull() ? null : updatedById);

        Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
        if (updatedAt != null) {
            config.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return config;
    }

    private ConfigInput validateConfigInput(String configKey,
                                            String configValue,
                                            String valueType,
                                            String description,
                                            Integer updatedById) {
        String cleanKey = normalizeConfigKey(configKey);
        String cleanValue = cleanString(configValue);
        String cleanType = normalizeValueType(valueType);
        String cleanDescription = cleanString(description);

        if (cleanKey == null) {
            return ConfigInput.invalid("ConfigKey khong hop le, toi da 100 ky tu.");
        }

        if (cleanValue != null && cleanValue.length() > MAX_CONFIG_VALUE_LENGTH) {
            return ConfigInput.invalid("ConfigValue qua dai, toi da 1000 ky tu.");
        }

        if (cleanType == null) {
            return ConfigInput.invalid("ValueType chi chap nhan INT, DECIMAL, BOOL, STRING, JSON.");
        }

        if (!isValidValueByType(cleanValue, cleanType)) {
            return ConfigInput.invalid("ConfigValue khong hop le theo ValueType = " + cleanType);
        }

        if (cleanDescription != null && cleanDescription.length() > MAX_DESCRIPTION_LENGTH) {
            return ConfigInput.invalid("Description qua dai, toi da 1000 ky tu.");
        }

        if (updatedById != null && updatedById <= 0) {
            return ConfigInput.invalid("UpdatedByID khong hop le.");
        }

        return ConfigInput.valid(
                cleanKey,
                cleanValue,
                cleanType,
                cleanDescription,
                updatedById
        );
    }

    private boolean isValidValueByType(String configValue, String valueType) {
        String type = normalizeValueType(valueType);

        if (type == null) {
            return false;
        }

        if (configValue == null) {
            return true;
        }

        try {
            if (TYPE_INT.equals(type)) {
                Integer.parseInt(configValue);
                return true;
            }

            if (TYPE_DECIMAL.equals(type)) {
                Double.parseDouble(configValue);
                return true;
            }

            if (TYPE_BOOL.equals(type)) {
                String value = configValue.trim().toLowerCase();
                return "true".equals(value)
                        || "false".equals(value)
                        || "1".equals(value)
                        || "0".equals(value)
                        || "yes".equals(value)
                        || "no".equals(value);
            }

            if (TYPE_JSON.equals(type)) {
                String value = configValue.trim();
                return (value.startsWith("{") && value.endsWith("}"))
                        || (value.startsWith("[") && value.endsWith("]"));
            }

            return TYPE_STRING.equals(type);

        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isUserExists(int userId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM USERS
                WHERE UserID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("Total") > 0;
            }

        } catch (SQLException e) {
            handleException(e, "isUserExists");
        }

        return false;
    }

    private String normalizeConfigKey(String configKey) {
        String value = cleanString(configKey);

        if (value == null || value.length() > MAX_CONFIG_KEY_LENGTH) {
            return null;
        }

        return value;
    }

    private String normalizeValueType(String valueType) {
        String value = cleanString(valueType);

        if (value == null || value.length() > MAX_VALUE_TYPE_LENGTH) {
            return null;
        }

        value = value.toUpperCase();

        if (TYPE_INT.equals(value)
                || TYPE_DECIMAL.equals(value)
                || TYPE_BOOL.equals(value)
                || TYPE_STRING.equals(value)
                || TYPE_JSON.equals(value)) {
            return value;
        }

        return null;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void handleException(SQLException e, String methodName) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();

        if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": UpdatedByID khong ton tai hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": ConfigKey da ton tai.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang SYSTEM_CONFIG.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang SYSTEM_CONFIG.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class ConfigInput {
        private final boolean valid;
        private final String message;
        private final String configKey;
        private final String configValue;
        private final String valueType;
        private final String description;
        private final Integer updatedById;

        private ConfigInput(boolean valid,
                            String message,
                            String configKey,
                            String configValue,
                            String valueType,
                            String description,
                            Integer updatedById) {
            this.valid = valid;
            this.message = message;
            this.configKey = configKey;
            this.configValue = configValue;
            this.valueType = valueType;
            this.description = description;
            this.updatedById = updatedById;
        }

        private static ConfigInput valid(String configKey,
                                         String configValue,
                                         String valueType,
                                         String description,
                                         Integer updatedById) {
            return new ConfigInput(
                    true,
                    null,
                    configKey,
                    configValue,
                    valueType,
                    description,
                    updatedById
            );
        }

        private static ConfigInput invalid(String message) {
            return new ConfigInput(
                    false,
                    message,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
