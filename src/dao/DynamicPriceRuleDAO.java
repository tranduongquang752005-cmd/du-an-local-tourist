package dao;

import config.DatabaseConnection;
import model.DynamicPriceRule;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DynamicPriceRuleDAO {

    private static final String TYPE_HOLIDAY = "HOLIDAY";
    private static final String TYPE_WEEKEND = "WEEKEND";
    private static final String TYPE_LOW_STOCK = "LOW_STOCK";
    private static final String TYPE_FUEL_SURGE = "FUEL_SURGE";

    private static final int MAX_RULE_NAME_LENGTH = 100;
    private static final int MAX_CONDITION_TYPE_LENGTH = 50;

    /*
     * LOW_STOCK được hiểu là lịch còn ít chỗ.
     * Ngưỡng tối ưu: lấy số lớn hơn giữa 3 chỗ và 20% tổng số chỗ.
     * Ví dụ:
     * - AvailableSlots = 30 => threshold = 6
     * - AvailableSlots = 10 => threshold = 3
     */
    private static final int MIN_LOW_STOCK_THRESHOLD = 3;
    private static final BigDecimal LOW_STOCK_PERCENT_THRESHOLD = new BigDecimal("0.20");

    public int createRule(String ruleName,
                          String conditionType,
                          BigDecimal modifierPercent,
                          LocalDate startDate,
                          LocalDate endDate,
                          int priority) {
        RuleInput input = validateRuleInput(
                ruleName,
                conditionType,
                modifierPercent,
                startDate,
                endDate,
                priority
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (getRuleByName(input.ruleName) != null) {
            System.out.println("RuleName da ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO DYNAMIC_PRICE_RULES
                (
                    RuleName,
                    ConditionType,
                    ModifierPercent,
                    StartDate,
                    EndDate,
                    Priority,
                    IsActive
                )
                VALUES (?, ?, ?, ?, ?, ?, 1)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setString(1, input.ruleName);
            ps.setString(2, input.conditionType);
            ps.setBigDecimal(3, input.modifierPercent);
            setNullableDate(ps, 4, input.startDate);
            setNullableDate(ps, 5, input.endDate);
            ps.setInt(6, input.priority);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createRule");
        }

        return -1;
    }

    public boolean updateRule(int ruleId,
                              String ruleName,
                              String conditionType,
                              BigDecimal modifierPercent,
                              LocalDate startDate,
                              LocalDate endDate,
                              int priority) {
        if (ruleId <= 0) {
            System.out.println("RuleID khong hop le.");
            return false;
        }

        DynamicPriceRule current = getRuleById(ruleId);

        if (current == null) {
            System.out.println("Khong tim thay dynamic price rule.");
            return false;
        }

        RuleInput input = validateRuleInput(
                ruleName,
                conditionType,
                modifierPercent,
                startDate,
                endDate,
                priority
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        DynamicPriceRule duplicated = getRuleByName(input.ruleName);

        if (duplicated != null && duplicated.getRuleId() != ruleId) {
            System.out.println("RuleName da ton tai o rule khac.");
            return false;
        }

        String sql = """
                UPDATE DYNAMIC_PRICE_RULES
                SET RuleName = ?,
                    ConditionType = ?,
                    ModifierPercent = ?,
                    StartDate = ?,
                    EndDate = ?,
                    Priority = ?
                WHERE RuleID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, input.ruleName);
            ps.setString(2, input.conditionType);
            ps.setBigDecimal(3, input.modifierPercent);
            setNullableDate(ps, 4, input.startDate);
            setNullableDate(ps, 5, input.endDate);
            ps.setInt(6, input.priority);
            ps.setInt(7, ruleId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat dynamic price rule that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateRule");
        }

        return false;
    }

    public boolean activateRule(int ruleId) {
        return updateActiveStatus(ruleId, true);
    }

    public boolean deactivateRule(int ruleId) {
        return updateActiveStatus(ruleId, false);
    }

    public boolean deleteRule(int ruleId) {
        if (ruleId <= 0) {
            System.out.println("RuleID khong hop le.");
            return false;
        }

        String sql = """
                DELETE FROM DYNAMIC_PRICE_RULES
                WHERE RuleID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, ruleId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay dynamic price rule de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteRule");
        }

        return false;
    }

    public DynamicPriceRule getRuleById(int ruleId) {
        if (ruleId <= 0) {
            System.out.println("RuleID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE RuleID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, ruleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRule(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getRuleById");
        }

        return null;
    }

    public DynamicPriceRule getRuleByName(String ruleName) {
        String cleanName = cleanString(ruleName);

        if (cleanName == null) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE RuleName = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRule(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getRuleByName");
        }

        return null;
    }

    public List<DynamicPriceRule> getAllRules() {
        String sql = buildSelectSql("""
                ORDER BY IsActive DESC, Priority ASC, RuleID ASC
                """);

        return queryRuleList(sql, null, "getAllRules");
    }

    public List<DynamicPriceRule> getActiveRules() {
        String sql = buildSelectSql("""
                WHERE IsActive = 1
                ORDER BY Priority ASC, RuleID ASC
                """);

        return queryRuleList(sql, null, "getActiveRules");
    }

    /*
     * Lấy rule còn hiệu lực theo ngày.
     * StartDate NULL = không giới hạn ngày bắt đầu.
     * EndDate NULL = không giới hạn ngày kết thúc.
     *
     * Hàm này chỉ lọc theo ngày, chưa quyết định WEEKEND/LOW_STOCK có áp dụng hay không.
     * Việc áp dụng thật sự nằm ở calculateFinalPrice... để tránh áp sai LOW_STOCK.
     */
    public List<DynamicPriceRule> getActiveRulesByDate(LocalDate targetDate) {
        LocalDate validDate = targetDate == null ? LocalDate.now() : targetDate;

        String sql = buildSelectSql("""
                WHERE IsActive = 1
                  AND (StartDate IS NULL OR StartDate <= ?)
                  AND (EndDate IS NULL OR EndDate >= ?)
                ORDER BY Priority ASC, RuleID ASC
                """);

        return queryRuleList(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(validDate));
                    ps.setDate(2, Date.valueOf(validDate));
                },
                "getActiveRulesByDate"
        );
    }

    public List<DynamicPriceRule> getRulesByConditionType(String conditionType) {
        String cleanType = normalizeConditionType(conditionType);

        if (cleanType == null) {
            System.out.println("ConditionType khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE ConditionType = ?
                ORDER BY IsActive DESC, Priority ASC, RuleID ASC
                """);

        return queryRuleList(
                sql,
                ps -> ps.setString(1, cleanType),
                "getRulesByConditionType"
        );
    }

    /*
     * Tính giá theo ngày, không có schedule context.
     * - HOLIDAY/FUEL_SURGE: áp dụng nếu ngày hợp lệ.
     * - WEEKEND: chỉ áp dụng nếu targetDate là thứ 7/chủ nhật.
     * - LOW_STOCK: không áp dụng vì không biết schedule còn bao nhiêu chỗ.
     */
    public BigDecimal calculateFinalPrice(BigDecimal basePrice, LocalDate targetDate) {
        return calculateFinalPriceInternal(basePrice, targetDate, null);
    }

    /*
     * Tính giá tối ưu cho lịch khởi hành cụ thể.
     * Hàm này nên được service dùng khi tính giá booking/schedule thực tế.
     */
    public BigDecimal calculateFinalPriceForSchedule(BigDecimal basePrice,
                                                     int scheduleId,
                                                     LocalDate targetDate) {
        if (scheduleId <= 0) {
            System.out.println("ScheduleID khong hop le.");
            return normalizeMoney(basePrice);
        }

        ScheduleSnapshot snapshot = getScheduleSnapshot(scheduleId);

        if (snapshot == null) {
            System.out.println("Khong tim thay schedule de tinh gia dong.");
            return normalizeMoney(basePrice);
        }

        LocalDate validDate = targetDate == null ? snapshot.scheduleDate : targetDate;

        if (validDate == null) {
            validDate = LocalDate.now();
        }

        return calculateFinalPriceInternal(basePrice, validDate, snapshot);
    }

    /*
     * Chỉ tính tổng phần trăm modifier áp dụng, dùng để debug/test hoặc service cần hiển thị chi tiết.
     */
    public BigDecimal calculateAppliedModifierPercent(LocalDate targetDate, Integer scheduleId) {
        LocalDate validDate = targetDate == null ? LocalDate.now() : targetDate;
        ScheduleSnapshot snapshot = scheduleId == null ? null : getScheduleSnapshot(scheduleId);

        BigDecimal totalModifierPercent = BigDecimal.ZERO;

        for (DynamicPriceRule rule : getActiveRulesByDate(validDate)) {
            if (isRuleApplicable(rule, validDate, snapshot) && rule.getModifierPercent() != null) {
                totalModifierPercent = totalModifierPercent.add(rule.getModifierPercent());
            }
        }

        return totalModifierPercent.setScale(2, RoundingMode.HALF_UP);
    }

    public int countActiveRules() {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM DYNAMIC_PRICE_RULES
                WHERE IsActive = 1
                """;

        return queryCount(sql, null, "countActiveRules");
    }

    private BigDecimal calculateFinalPriceInternal(BigDecimal basePrice,
                                                   LocalDate targetDate,
                                                   ScheduleSnapshot snapshot) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDate validDate = targetDate == null ? LocalDate.now() : targetDate;
        BigDecimal totalModifierPercent = BigDecimal.ZERO;

        for (DynamicPriceRule rule : getActiveRulesByDate(validDate)) {
            if (isRuleApplicable(rule, validDate, snapshot) && rule.getModifierPercent() != null) {
                totalModifierPercent = totalModifierPercent.add(rule.getModifierPercent());
            }
        }

        BigDecimal multiplier = BigDecimal.ONE.add(
                totalModifierPercent.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP)
        );

        BigDecimal finalPrice = basePrice.multiply(multiplier);

        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return finalPrice.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isRuleApplicable(DynamicPriceRule rule,
                                     LocalDate targetDate,
                                     ScheduleSnapshot snapshot) {
        if (rule == null || !rule.isActive()) {
            return false;
        }

        String type = normalizeConditionType(rule.getConditionType());

        if (type == null) {
            return false;
        }

        if (!isDateInRuleRange(rule, targetDate)) {
            return false;
        }

        if (TYPE_WEEKEND.equals(type)) {
            return isWeekend(targetDate);
        }

        if (TYPE_LOW_STOCK.equals(type)) {
            return isLowStock(snapshot);
        }

        if (TYPE_HOLIDAY.equals(type) || TYPE_FUEL_SURGE.equals(type)) {
            return true;
        }

        /*
         * Trường hợp sau này thêm conditionType mới:
         * Không tự áp dụng để tránh tăng/giảm giá sai nghiệp vụ.
         */
        return false;
    }

    private boolean isDateInRuleRange(DynamicPriceRule rule, LocalDate targetDate) {
        if (rule == null || targetDate == null) {
            return false;
        }

        LocalDate startDate = rule.getStartDate();
        LocalDate endDate = rule.getEndDate();

        boolean afterStart = startDate == null || !targetDate.isBefore(startDate);
        boolean beforeEnd = endDate == null || !targetDate.isAfter(endDate);

        return afterStart && beforeEnd;
    }

    private boolean isWeekend(LocalDate targetDate) {
        if (targetDate == null) {
            return false;
        }

        DayOfWeek day = targetDate.getDayOfWeek();
        return DayOfWeek.SATURDAY.equals(day) || DayOfWeek.SUNDAY.equals(day);
    }

    private boolean isLowStock(ScheduleSnapshot snapshot) {
        if (snapshot == null || snapshot.availableSlots <= 0) {
            return false;
        }

        int remainingSlots = snapshot.availableSlots - snapshot.bookedSlots;
        int percentThreshold = new BigDecimal(snapshot.availableSlots)
                .multiply(LOW_STOCK_PERCENT_THRESHOLD)
                .setScale(0, RoundingMode.CEILING)
                .intValue();

        int lowStockThreshold = Math.max(MIN_LOW_STOCK_THRESHOLD, percentThreshold);

        return remainingSlots > 0 && remainingSlots <= lowStockThreshold;
    }

    private ScheduleSnapshot getScheduleSnapshot(int scheduleId) {
        String sql = """
                SELECT
                    ScheduleDate,
                    AvailableSlots,
                    BookedSlots
                FROM TOUR_SCHEDULES
                WHERE ScheduleID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, scheduleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ScheduleSnapshot snapshot = new ScheduleSnapshot();

                    Date scheduleDate = rs.getDate("ScheduleDate");
                    if (scheduleDate != null) {
                        snapshot.scheduleDate = scheduleDate.toLocalDate();
                    }

                    snapshot.availableSlots = rs.getInt("AvailableSlots");
                    snapshot.bookedSlots = rs.getInt("BookedSlots");

                    return snapshot;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getScheduleSnapshot");
        }

        return null;
    }

    private boolean updateActiveStatus(int ruleId, boolean active) {
        if (ruleId <= 0) {
            System.out.println("RuleID khong hop le.");
            return false;
        }

        String sql = """
                UPDATE DYNAMIC_PRICE_RULES
                SET IsActive = ?
                WHERE RuleID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBoolean(1, active);
            ps.setInt(2, ruleId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay dynamic price rule.");
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
                    RuleID,
                    RuleName,
                    ConditionType,
                    ModifierPercent,
                    StartDate,
                    EndDate,
                    Priority,
                    IsActive,
                    CreatedAt
                FROM DYNAMIC_PRICE_RULES
                """ + condition;
    }

    private List<DynamicPriceRule> queryRuleList(String sql,
                                                 SqlSetter setter,
                                                 String methodName) {
        List<DynamicPriceRule> rules = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rules.add(mapRule(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return rules;
    }

    private DynamicPriceRule mapRule(ResultSet rs) throws SQLException {
        DynamicPriceRule rule = new DynamicPriceRule();

        rule.setRuleId(rs.getInt("RuleID"));
        rule.setRuleName(rs.getString("RuleName"));
        rule.setConditionType(rs.getString("ConditionType"));
        rule.setModifierPercent(rs.getBigDecimal("ModifierPercent"));

        Date startDate = rs.getDate("StartDate");
        if (startDate != null) {
            rule.setStartDate(startDate.toLocalDate());
        }

        Date endDate = rs.getDate("EndDate");
        if (endDate != null) {
            rule.setEndDate(endDate.toLocalDate());
        }

        rule.setPriority(rs.getInt("Priority"));
        rule.setActive(rs.getBoolean("IsActive"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            rule.setCreatedAt(createdAt.toLocalDateTime());
        }

        return rule;
    }

    private RuleInput validateRuleInput(String ruleName,
                                        String conditionType,
                                        BigDecimal modifierPercent,
                                        LocalDate startDate,
                                        LocalDate endDate,
                                        int priority) {
        String cleanName = cleanString(ruleName);
        String cleanType = normalizeConditionType(conditionType);

        if (cleanName == null || cleanName.length() > MAX_RULE_NAME_LENGTH) {
            return RuleInput.invalid("RuleName khong hop le, toi da 100 ky tu.");
        }

        if (cleanType == null) {
            return RuleInput.invalid("ConditionType khong hop le.");
        }

        if (modifierPercent == null) {
            return RuleInput.invalid("ModifierPercent khong duoc null.");
        }

        if (modifierPercent.compareTo(new BigDecimal("-100")) < 0
                || modifierPercent.compareTo(new BigDecimal("500")) > 0) {
            return RuleInput.invalid("ModifierPercent nen nam trong khoang -100 den 500.");
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return RuleInput.invalid("StartDate khong duoc lon hon EndDate.");
        }

        if (priority < 0 || priority > 1000) {
            return RuleInput.invalid("Priority phai tu 0 den 1000.");
        }

        return RuleInput.valid(
                cleanName,
                cleanType,
                modifierPercent.setScale(2, RoundingMode.HALF_UP),
                startDate,
                endDate,
                priority
        );
    }

    private String normalizeConditionType(String conditionType) {
        String cleanType = cleanString(conditionType);

        if (cleanType == null || cleanType.length() > MAX_CONDITION_TYPE_LENGTH) {
            return null;
        }

        cleanType = cleanType.toUpperCase();

        if (TYPE_HOLIDAY.equals(cleanType)
                || TYPE_WEEKEND.equals(cleanType)
                || TYPE_LOW_STOCK.equals(cleanType)
                || TYPE_FUEL_SURGE.equals(cleanType)) {
            return cleanType;
        }

        return null;
    }

    private int queryCount(String sql,
                           SqlSetter setter,
                           String methodName) {
        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total");
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return 0;
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private void setNullableDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(value));
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

        if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": RuleName bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang DYNAMIC_PRICE_RULES.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu rule vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": RuleName/ConditionType qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang DYNAMIC_PRICE_RULES hoac TOUR_SCHEDULES.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class ScheduleSnapshot {
        private LocalDate scheduleDate;
        private int availableSlots;
        private int bookedSlots;
    }

    private static class RuleInput {
        private final boolean valid;
        private final String message;
        private final String ruleName;
        private final String conditionType;
        private final BigDecimal modifierPercent;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final int priority;

        private RuleInput(boolean valid,
                          String message,
                          String ruleName,
                          String conditionType,
                          BigDecimal modifierPercent,
                          LocalDate startDate,
                          LocalDate endDate,
                          int priority) {
            this.valid = valid;
            this.message = message;
            this.ruleName = ruleName;
            this.conditionType = conditionType;
            this.modifierPercent = modifierPercent;
            this.startDate = startDate;
            this.endDate = endDate;
            this.priority = priority;
        }

        private static RuleInput valid(String ruleName,
                                       String conditionType,
                                       BigDecimal modifierPercent,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       int priority) {
            return new RuleInput(
                    true,
                    null,
                    ruleName,
                    conditionType,
                    modifierPercent,
                    startDate,
                    endDate,
                    priority
            );
        }

        private static RuleInput invalid(String message) {
            return new RuleInput(
                    false,
                    message,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0
            );
        }
    }
}
