package dao;

import config.DatabaseConnection;
import model.Refund;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RefundDAO {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private static final int MAX_REFUND_METHOD_LENGTH = 50;
    private static final int MAX_TRANSACTION_ID_LENGTH = 100;

    public Refund getRefundById(int refundId) {
        if (refundId <= 0) {
            System.out.println("RefundID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE RefundID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, refundId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRefund(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getRefundById");
        }

        return null;
    }

    public List<Refund> getAllRefunds() {
        String sql = buildSelectSql("""
                ORDER BY CreatedAt DESC, RefundID DESC
                """);

        return queryRefundList(sql, null, "getAllRefunds");
    }

    public List<Refund> getRecentRefunds(int limit) {
        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    RefundID,
                    BookingID,
                    RefundAmount,
                    RefundMethod,
                    RefundStatus,
                    TransactionID,
                    RefundDate,
                    CreatedAt
                FROM REFUNDS
                ORDER BY CreatedAt DESC, RefundID DESC
                """;

        return queryRefundList(
                sql,
                ps -> ps.setInt(1, validLimit),
                "getRecentRefunds"
        );
    }

    public List<Refund> getRefundsByBookingId(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE BookingID = ?
                ORDER BY CreatedAt DESC, RefundID DESC
                """);

        return queryRefundList(
                sql,
                ps -> ps.setInt(1, bookingId),
                "getRefundsByBookingId"
        );
    }

    public List<Refund> getRefundsByStatus(String refundStatus) {
        String status = normalizeRefundStatus(refundStatus);

        if (status == null) {
            System.out.println("RefundStatus chi chap nhan PENDING, SUCCESS, FAILED.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE RefundStatus = ?
                ORDER BY CreatedAt DESC, RefundID DESC
                """);

        return queryRefundList(
                sql,
                ps -> ps.setString(1, status),
                "getRefundsByStatus"
        );
    }

    public List<Refund> getRefundsByDateRange(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = buildSelectSql("""
                WHERE CAST(CreatedAt AS DATE) BETWEEN ? AND ?
                ORDER BY CreatedAt DESC, RefundID DESC
                """);

        return queryRefundList(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(range.fromDate));
                    ps.setDate(2, Date.valueOf(range.toDate));
                },
                "getRefundsByDateRange"
        );
    }

    public int createRefundRequest(int bookingId,
                                   BigDecimal refundAmount,
                                   String refundMethod,
                                   String transactionId) {
        RefundInput input = validateRefundInput(
                bookingId,
                refundAmount,
                refundMethod,
                transactionId
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (!isBookingExists(input.bookingId)) {
            System.out.println("BookingID khong ton tai.");
            return -1;
        }

        String bookingStatus = getBookingStatus(input.bookingId);

        if (!isRefundableBookingStatus(bookingStatus)) {
            System.out.println("Chi tao refund cho booking da CANCELLED.");
            return -1;
        }

        if (hasPendingRefund(input.bookingId)) {
            System.out.println("Booking dang co refund PENDING, khong tao trung.");
            return -1;
        }

        BigDecimal remainingRefundable = getRemainingRefundableAmount(input.bookingId);

        if (input.refundAmount.compareTo(remainingRefundable) > 0) {
            System.out.println("RefundAmount vuot qua so tien con co the hoan. Remaining = " + remainingRefundable);
            return -1;
        }

        String sql = """
                INSERT INTO REFUNDS
                (
                    BookingID,
                    RefundAmount,
                    RefundMethod,
                    RefundStatus,
                    TransactionID
                )
                VALUES (?, ?, ?, 'PENDING', ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.bookingId);
            ps.setBigDecimal(2, input.refundAmount);
            ps.setString(3, input.refundMethod);
            ps.setString(4, input.transactionId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createRefundRequest");
        }

        return -1;
    }

    public boolean completeRefund(int refundId, String transactionId) {
        String cleanTransactionId = cleanString(transactionId);

        if (!isValidTransactionId(cleanTransactionId)) {
            System.out.println("TransactionID qua dai, toi da 100 ky tu.");
            return false;
        }

        return updateRefundStatus(refundId, STATUS_SUCCESS, cleanTransactionId);
    }

    public boolean failRefund(int refundId, String transactionId) {
        String cleanTransactionId = cleanString(transactionId);

        if (!isValidTransactionId(cleanTransactionId)) {
            System.out.println("TransactionID qua dai, toi da 100 ky tu.");
            return false;
        }

        return updateRefundStatus(refundId, STATUS_FAILED, cleanTransactionId);
    }

    /*
     * SQL REFUNDS không có trạng thái PROCESSING.
     * Hàm này giữ để Main/code cũ không bị lỗi compile.
     */
    public boolean markRefundProcessing(int refundId) {
        if (refundId <= 0) {
            System.out.println("RefundID khong hop le.");
            return false;
        }

        Refund current = getRefundById(refundId);

        if (current == null) {
            System.out.println("Khong tim thay refund.");
            return false;
        }

        System.out.println("SQL REFUNDS khong co status PROCESSING, bo qua buoc mark processing.");
        return STATUS_PENDING.equalsIgnoreCase(current.getRefundStatus());
    }

    /*
     * SQL REFUNDS không có trạng thái CANCELLED.
     * Nếu cần hủy refund request thì chuyển sang FAILED để khớp CHECK constraint.
     */
    public boolean cancelRefund(int refundId) {
        System.out.println("SQL REFUNDS khong co status CANCELLED, chuyen refund sang FAILED.");
        return failRefund(refundId, null);
    }

    public boolean updateRefundStatus(int refundId,
                                      String refundStatus,
                                      String transactionId) {
        String status = normalizeRefundStatus(refundStatus);
        String cleanTransactionId = cleanString(transactionId);

        if (refundId <= 0) {
            System.out.println("RefundID khong hop le.");
            return false;
        }

        if (status == null) {
            System.out.println("RefundStatus chi chap nhan PENDING, SUCCESS, FAILED.");
            return false;
        }

        if (!isValidTransactionId(cleanTransactionId)) {
            System.out.println("TransactionID qua dai, toi da 100 ky tu.");
            return false;
        }

        Refund current = getRefundById(refundId);

        if (current == null) {
            System.out.println("Khong tim thay refund.");
            return false;
        }

        String currentStatus = normalizeRefundStatus(current.getRefundStatus());

        if (STATUS_SUCCESS.equals(currentStatus)) {
            System.out.println("Refund da SUCCESS, khong nen sua trang thai.");
            return false;
        }

        if (STATUS_FAILED.equals(currentStatus)) {
            System.out.println("Refund da FAILED, khong nen sua trang thai.");
            return false;
        }

        String sql = """
                UPDATE REFUNDS
                SET RefundStatus = ?,
                    TransactionID = COALESCE(?, TransactionID),
                    RefundDate = CASE
                        WHEN ? = 'SUCCESS' THEN GETDATE()
                        ELSE RefundDate
                    END
                WHERE RefundID = ?
                  AND RefundStatus = 'PENDING'
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, status);
            ps.setString(2, cleanTransactionId);
            ps.setString(3, status);
            ps.setInt(4, refundId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat refund that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateRefundStatus");
        }

        return false;
    }

    public BigDecimal getTotalSuccessfulRefundAmount(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = """
                SELECT ISNULL(SUM(RefundAmount), 0) AS TotalRefund
                FROM REFUNDS
                WHERE RefundStatus = 'SUCCESS'
                  AND CAST(RefundDate AS DATE) BETWEEN ? AND ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setDate(1, Date.valueOf(range.fromDate));
            ps.setDate(2, Date.valueOf(range.toDate));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("TotalRefund");
                    return total == null ? BigDecimal.ZERO : total;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTotalSuccessfulRefundAmount");
        }

        return BigDecimal.ZERO;
    }

    public BigDecimal getTotalSuccessfulRefundByBookingId(int bookingId) {
        if (bookingId <= 0) {
            return BigDecimal.ZERO;
        }

        String sql = """
                SELECT ISNULL(SUM(RefundAmount), 0) AS TotalRefund
                FROM REFUNDS
                WHERE BookingID = ?
                  AND RefundStatus = 'SUCCESS'
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("TotalRefund");
                    return total == null ? BigDecimal.ZERO : total;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTotalSuccessfulRefundByBookingId");
        }

        return BigDecimal.ZERO;
    }

    public BigDecimal getRemainingRefundableAmount(int bookingId) {
        if (bookingId <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal paidAmount = getTotalSuccessfulPaymentAmountByBookingId(bookingId);
        BigDecimal refundedAmount = getTotalSuccessfulRefundByBookingId(bookingId);

        BigDecimal remaining = paidAmount.subtract(refundedAmount);
        return remaining.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remaining;
    }

    public boolean canCreateRefund(int bookingId, BigDecimal refundAmount) {
        if (bookingId <= 0 || !isValidAmount(refundAmount)) {
            return false;
        }

        String bookingStatus = getBookingStatus(bookingId);

        if (!isRefundableBookingStatus(bookingStatus)) {
            return false;
        }

        if (hasPendingRefund(bookingId)) {
            return false;
        }

        BigDecimal remaining = getRemainingRefundableAmount(bookingId);
        return refundAmount.compareTo(remaining) <= 0;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    RefundID,
                    BookingID,
                    RefundAmount,
                    RefundMethod,
                    RefundStatus,
                    TransactionID,
                    RefundDate,
                    CreatedAt
                FROM REFUNDS
                """ + condition;
    }

    private List<Refund> queryRefundList(String sql,
                                         SqlSetter setter,
                                         String methodName) {
        List<Refund> refunds = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    refunds.add(mapRefund(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return refunds;
    }

    private Refund mapRefund(ResultSet rs) throws SQLException {
        Refund refund = new Refund();

        refund.setRefundId(rs.getInt("RefundID"));
        refund.setBookingId(rs.getInt("BookingID"));
        refund.setRefundAmount(rs.getBigDecimal("RefundAmount"));
        refund.setRefundMethod(rs.getString("RefundMethod"));
        refund.setRefundStatus(rs.getString("RefundStatus"));
        refund.setTransactionId(rs.getString("TransactionID"));

        Timestamp refundDate = rs.getTimestamp("RefundDate");
        if (refundDate != null) {
            refund.setRefundDate(refundDate.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            refund.setCreatedAt(createdAt.toLocalDateTime());
        }

        return refund;
    }

    private boolean isBookingExists(int bookingId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS
                WHERE BookingID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, bookingId),
                "isBookingExists"
        ) > 0;
    }

    private String getBookingStatus(int bookingId) {
        String sql = """
                SELECT Status
                FROM BOOKINGS
                WHERE BookingID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return cleanString(rs.getString("Status"));
                }
            }

        } catch (SQLException e) {
            handleException(e, "getBookingStatus");
        }

        return null;
    }

    private BigDecimal getTotalSuccessfulPaymentAmountByBookingId(int bookingId) {
        String sql = """
                SELECT ISNULL(SUM(Amount), 0) AS TotalPaid
                FROM PAYMENTS
                WHERE BookingID = ?
                  AND PaymentStatus = 'SUCCESS'
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("TotalPaid");
                    return total == null ? BigDecimal.ZERO : total;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTotalSuccessfulPaymentAmountByBookingId");
        }

        return BigDecimal.ZERO;
    }

    private boolean hasPendingRefund(int bookingId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM REFUNDS
                WHERE BookingID = ?
                  AND RefundStatus = 'PENDING'
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, bookingId),
                "hasPendingRefund"
        ) > 0;
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

    private RefundInput validateRefundInput(int bookingId,
                                            BigDecimal refundAmount,
                                            String refundMethod,
                                            String transactionId) {
        String cleanRefundMethod = cleanString(refundMethod);
        String cleanTransactionId = cleanString(transactionId);

        if (bookingId <= 0) {
            return RefundInput.invalid("BookingID khong hop le.");
        }

        if (!isValidAmount(refundAmount)) {
            return RefundInput.invalid("RefundAmount phai lon hon 0.");
        }

        if (cleanRefundMethod == null || cleanRefundMethod.length() > MAX_REFUND_METHOD_LENGTH) {
            return RefundInput.invalid("RefundMethod khong hop le, toi da 50 ky tu.");
        }

        if (!isValidTransactionId(cleanTransactionId)) {
            return RefundInput.invalid("TransactionID qua dai, toi da 100 ky tu.");
        }

        return RefundInput.valid(
                bookingId,
                refundAmount,
                cleanRefundMethod,
                cleanTransactionId
        );
    }

    private boolean isRefundableBookingStatus(String bookingStatus) {
        return bookingStatus != null && bookingStatus.trim().equalsIgnoreCase("CANCELLED");
    }

    private String normalizeRefundStatus(String refundStatus) {
        String status = cleanString(refundStatus);

        if (status == null) {
            return null;
        }

        status = status.toUpperCase();

        if (status.equals(STATUS_PENDING)
                || status.equals(STATUS_SUCCESS)
                || status.equals(STATUS_FAILED)) {
            return status;
        }

        return null;
    }

    private boolean isValidTransactionId(String transactionId) {
        return transactionId == null || transactionId.length() <= MAX_TRANSACTION_ID_LENGTH;
    }

    private boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
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
            return 10;
        }

        return Math.min(limit, 100);
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
            System.out.println("Loi " + methodName + ": BookingID khong ton tai hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Du lieu refund bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang REFUNDS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot trong database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang REFUNDS.");
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

    private static class RefundInput {
        private final boolean valid;
        private final String message;
        private final int bookingId;
        private final BigDecimal refundAmount;
        private final String refundMethod;
        private final String transactionId;

        private RefundInput(boolean valid,
                            String message,
                            int bookingId,
                            BigDecimal refundAmount,
                            String refundMethod,
                            String transactionId) {
            this.valid = valid;
            this.message = message;
            this.bookingId = bookingId;
            this.refundAmount = refundAmount;
            this.refundMethod = refundMethod;
            this.transactionId = transactionId;
        }

        private static RefundInput valid(int bookingId,
                                         BigDecimal refundAmount,
                                         String refundMethod,
                                         String transactionId) {
            return new RefundInput(
                    true,
                    null,
                    bookingId,
                    refundAmount,
                    refundMethod,
                    transactionId
            );
        }

        private static RefundInput invalid(String message) {
            return new RefundInput(
                    false,
                    message,
                    0,
                    null,
                    null,
                    null
            );
        }
    }
}
