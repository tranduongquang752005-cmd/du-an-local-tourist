package dao;

import config.DatabaseConnection;
import model.AuditLog;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;
    private static final int MAX_TABLE_NAME_LENGTH = 100;

    public AuditLog getAuditLogById(int auditId) {
        if (auditId <= 0) {
            System.out.println("AuditID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE AuditID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, auditId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAuditLog(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getAuditLogById");
        }

        return null;
    }

    public List<AuditLog> getRecentAuditLogs(int limit) {
        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    AuditID,
                    TableName,
                    RecordID,
                    AuditAction,
                    OldStatus,
                    NewStatus,
                    ChangedByID,
                    ChangedBy,
                    ChangedAt
                FROM AUDIT_LOG
                ORDER BY ChangedAt DESC, AuditID DESC
                """;

        return queryAuditLogList(
                sql,
                ps -> ps.setInt(1, validLimit),
                "getRecentAuditLogs"
        );
    }

    public List<AuditLog> getAuditLogsByTable(String tableName, int limit) {
        String cleanTableName = normalizeTableName(tableName);

        if (cleanTableName == null) {
            System.out.println("TableName khong hop le.");
            return new ArrayList<>();
        }

        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    AuditID,
                    TableName,
                    RecordID,
                    AuditAction,
                    OldStatus,
                    NewStatus,
                    ChangedByID,
                    ChangedBy,
                    ChangedAt
                FROM AUDIT_LOG
                WHERE TableName = ?
                ORDER BY ChangedAt DESC, AuditID DESC
                """;

        return queryAuditLogList(
                sql,
                ps -> {
                    ps.setInt(1, validLimit);
                    ps.setString(2, cleanTableName);
                },
                "getAuditLogsByTable"
        );
    }

    public List<AuditLog> getAuditLogsByRecord(String tableName, int recordId) {
        String cleanTableName = normalizeTableName(tableName);

        if (cleanTableName == null) {
            System.out.println("TableName khong hop le.");
            return new ArrayList<>();
        }

        if (recordId <= 0) {
            System.out.println("RecordID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TableName = ?
                  AND RecordID = ?
                ORDER BY ChangedAt DESC, AuditID DESC
                """);

        return queryAuditLogList(
                sql,
                ps -> {
                    ps.setString(1, cleanTableName);
                    ps.setInt(2, recordId);
                },
                "getAuditLogsByRecord"
        );
    }

    public List<AuditLog> getBookingAuditLogs(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return new ArrayList<>();
        }

        return getAuditLogsByRecord("BOOKINGS", bookingId);
    }

    public List<AuditLog> getAuditLogsByChangedById(int changedById, int limit) {
        if (changedById <= 0) {
            System.out.println("ChangedByID khong hop le.");
            return new ArrayList<>();
        }

        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    AuditID,
                    TableName,
                    RecordID,
                    AuditAction,
                    OldStatus,
                    NewStatus,
                    ChangedByID,
                    ChangedBy,
                    ChangedAt
                FROM AUDIT_LOG
                WHERE ChangedByID = ?
                ORDER BY ChangedAt DESC, AuditID DESC
                """;

        return queryAuditLogList(
                sql,
                ps -> {
                    ps.setInt(1, validLimit);
                    ps.setInt(2, changedById);
                },
                "getAuditLogsByChangedById"
        );
    }

    public List<AuditLog> getAuditLogsByDateRange(LocalDate fromDate,
                                                  LocalDate toDate,
                                                  int limit) {
        DateRange range = normalizeDateRange(fromDate, toDate);
        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    AuditID,
                    TableName,
                    RecordID,
                    AuditAction,
                    OldStatus,
                    NewStatus,
                    ChangedByID,
                    ChangedBy,
                    ChangedAt
                FROM AUDIT_LOG
                WHERE CAST(ChangedAt AS DATE) BETWEEN ? AND ?
                ORDER BY ChangedAt DESC, AuditID DESC
                """;

        return queryAuditLogList(
                sql,
                ps -> {
                    ps.setInt(1, validLimit);
                    ps.setDate(2, Date.valueOf(range.fromDate));
                    ps.setDate(3, Date.valueOf(range.toDate));
                },
                "getAuditLogsByDateRange"
        );
    }

    public int countAuditLogsByTable(String tableName) {
        String cleanTableName = normalizeTableName(tableName);

        if (cleanTableName == null) {
            System.out.println("TableName khong hop le.");
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM AUDIT_LOG
                WHERE TableName = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setString(1, cleanTableName),
                "countAuditLogsByTable"
        );
    }

    public int countAuditLogsByRecord(String tableName, int recordId) {
        String cleanTableName = normalizeTableName(tableName);

        if (cleanTableName == null || recordId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM AUDIT_LOG
                WHERE TableName = ?
                  AND RecordID = ?
                """;

        return queryCount(
                sql,
                ps -> {
                    ps.setString(1, cleanTableName);
                    ps.setInt(2, recordId);
                },
                "countAuditLogsByRecord"
        );
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    AuditID,
                    TableName,
                    RecordID,
                    AuditAction,
                    OldStatus,
                    NewStatus,
                    ChangedByID,
                    ChangedBy,
                    ChangedAt
                FROM AUDIT_LOG
                """ + condition;
    }

    private List<AuditLog> queryAuditLogList(String sql,
                                             SqlSetter setter,
                                             String methodName) {
        List<AuditLog> logs = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapAuditLog(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return logs;
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

    private AuditLog mapAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();

        log.setAuditId(rs.getInt("AuditID"));
        log.setTableName(rs.getString("TableName"));
        log.setRecordId(rs.getInt("RecordID"));
        log.setAuditAction(rs.getString("AuditAction"));
        log.setOldStatus(rs.getString("OldStatus"));
        log.setNewStatus(rs.getString("NewStatus"));

        int changedById = rs.getInt("ChangedByID");
        log.setChangedById(rs.wasNull() ? null : changedById);

        log.setChangedBy(rs.getString("ChangedBy"));

        Timestamp changedAt = rs.getTimestamp("ChangedAt");
        if (changedAt != null) {
            log.setChangedAt(changedAt.toLocalDateTime());
        }

        return log;
    }

    private DateRange normalizeDateRange(LocalDate fromDate, LocalDate toDate) {
        LocalDate validFromDate = fromDate == null ? LocalDate.of(2000, 1, 1) : fromDate;
        LocalDate validToDate = toDate == null ? LocalDate.now() : toDate;

        if (validFromDate.isAfter(validToDate)) {
            LocalDate temp = validFromDate;
            validFromDate = validToDate;
            validToDate = temp;
        }

        return new DateRange(validFromDate, validToDate);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }

        return Math.min(limit, MAX_LIMIT);
    }

    private String normalizeTableName(String tableName) {
        String value = cleanString(tableName);

        if (value == null || value.length() > MAX_TABLE_NAME_LENGTH) {
            return null;
        }

        return value.toUpperCase();
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

        if (errorCode == 208) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang AUDIT_LOG.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang AUDIT_LOG.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Ten bang/view khong ton tai.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class DateRange {
        private final LocalDate fromDate;
        private final LocalDate toDate;

        private DateRange(LocalDate fromDate, LocalDate toDate) {
            this.fromDate = fromDate;
            this.toDate = toDate;
        }
    }
}
