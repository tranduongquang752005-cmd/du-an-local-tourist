package dao;

import config.DatabaseConnection;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RevenueDAO {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    public BigDecimal getTotalRevenue(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = """
                SELECT ISNULL(SUM(Amount), 0) AS Total
                FROM PAYMENTS
                WHERE PaymentStatus = 'SUCCESS'
                  AND CAST(PaymentDate AS DATE) BETWEEN ? AND ?
                """;

        return queryBigDecimal(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(range.fromDate));
                    ps.setDate(2, Date.valueOf(range.toDate));
                },
                "getTotalRevenue"
        );
    }

    public BigDecimal getTotalRefund(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = """
                SELECT ISNULL(SUM(RefundAmount), 0) AS Total
                FROM REFUNDS
                WHERE RefundStatus = 'SUCCESS'
                  AND CAST(RefundDate AS DATE) BETWEEN ? AND ?
                """;

        return queryBigDecimal(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(range.fromDate));
                    ps.setDate(2, Date.valueOf(range.toDate));
                },
                "getTotalRefund"
        );
    }

    public BigDecimal getNetRevenue(LocalDate fromDate, LocalDate toDate) {
        BigDecimal totalRevenue = getTotalRevenue(fromDate, toDate);
        BigDecimal totalRefund = getTotalRefund(fromDate, toDate);
        return totalRevenue.subtract(totalRefund);
    }

    public int countBookingsByStatus(String status, LocalDate fromDate, LocalDate toDate) {
        String normalizedStatus = normalizeBookingStatus(status);

        if (normalizedStatus == null) {
            System.out.println("Status booking chi chap nhan PENDING, PAID, COMPLETED, CANCELLED.");
            return 0;
        }

        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS
                WHERE Status = ?
                  AND CAST(BookingDate AS DATE) BETWEEN ? AND ?
                """;

        return queryInt(
                sql,
                ps -> {
                    ps.setString(1, normalizedStatus);
                    ps.setDate(2, Date.valueOf(range.fromDate));
                    ps.setDate(3, Date.valueOf(range.toDate));
                },
                "countBookingsByStatus"
        );
    }

    public int countSuccessfulPayments(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = """
                SELECT COUNT(*) AS Total
                FROM PAYMENTS
                WHERE PaymentStatus = 'SUCCESS'
                  AND CAST(PaymentDate AS DATE) BETWEEN ? AND ?
                """;

        return queryInt(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(range.fromDate));
                    ps.setDate(2, Date.valueOf(range.toDate));
                },
                "countSuccessfulPayments"
        );
    }

    public int countSuccessfulRefunds(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = """
                SELECT COUNT(*) AS Total
                FROM REFUNDS
                WHERE RefundStatus = 'SUCCESS'
                  AND CAST(RefundDate AS DATE) BETWEEN ? AND ?
                """;

        return queryInt(
                sql,
                ps -> {
                    ps.setDate(1, Date.valueOf(range.fromDate));
                    ps.setDate(2, Date.valueOf(range.toDate));
                },
                "countSuccessfulRefunds"
        );
    }

    public RevenueSummary getRevenueSummary(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        RevenueSummary summary = new RevenueSummary();

        summary.setFromDate(range.fromDate);
        summary.setToDate(range.toDate);

        BigDecimal totalRevenue = getTotalRevenue(range.fromDate, range.toDate);
        BigDecimal totalRefund = getTotalRefund(range.fromDate, range.toDate);

        summary.setTotalRevenue(totalRevenue);
        summary.setTotalRefund(totalRefund);
        summary.setNetRevenue(totalRevenue.subtract(totalRefund));

        summary.setPendingBookingCount(countBookingsByStatus(STATUS_PENDING, range.fromDate, range.toDate));
        summary.setPaidBookingCount(countBookingsByStatus(STATUS_PAID, range.fromDate, range.toDate));
        summary.setCompletedBookingCount(countBookingsByStatus(STATUS_COMPLETED, range.fromDate, range.toDate));
        summary.setCancelledBookingCount(countBookingsByStatus(STATUS_CANCELLED, range.fromDate, range.toDate));

        summary.setSuccessfulPaymentCount(countSuccessfulPayments(range.fromDate, range.toDate));
        summary.setSuccessfulRefundCount(countSuccessfulRefunds(range.fromDate, range.toDate));

        return summary;
    }

    public List<TourRevenueRow> getRevenueByTour(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = """
                SELECT
                    t.TourID,
                    t.TourName,
                    COUNT(DISTINCT b.BookingID) AS BookingCount,
                    ISNULL(SUM(p.Amount), 0) AS Revenue
                FROM TOURS t
                JOIN BOOKINGS b ON b.TourID = t.TourID
                JOIN PAYMENTS p ON p.BookingID = b.BookingID
                WHERE p.PaymentStatus = 'SUCCESS'
                  AND CAST(p.PaymentDate AS DATE) BETWEEN ? AND ?
                GROUP BY t.TourID, t.TourName
                ORDER BY Revenue DESC, BookingCount DESC, t.TourID ASC
                """;

        List<TourRevenueRow> rows = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setDate(1, Date.valueOf(range.fromDate));
            ps.setDate(2, Date.valueOf(range.toDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TourRevenueRow row = new TourRevenueRow();

                    row.setTourId(rs.getInt("TourID"));
                    row.setTourName(rs.getString("TourName"));
                    row.setBookingCount(rs.getInt("BookingCount"));
                    row.setRevenue(getBigDecimalOrZero(rs, "Revenue"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getRevenueByTour");
        }

        return rows;
    }

    public List<MonthlyRevenueRow> getMonthlyRevenue(int year) {
        if (year < 2000 || year > 2100) {
            System.out.println("Year khong hop le.");
            return new ArrayList<>();
        }

        String sql = """
                SELECT
                    MONTH(PaymentDate) AS RevenueMonth,
                    COUNT(DISTINCT BookingID) AS PaidBookingCount,
                    ISNULL(SUM(Amount), 0) AS Revenue
                FROM PAYMENTS
                WHERE PaymentStatus = 'SUCCESS'
                  AND YEAR(PaymentDate) = ?
                GROUP BY MONTH(PaymentDate)
                ORDER BY RevenueMonth ASC
                """;

        List<MonthlyRevenueRow> rows = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, year);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MonthlyRevenueRow row = new MonthlyRevenueRow();

                    row.setYear(year);
                    row.setMonth(rs.getInt("RevenueMonth"));
                    row.setPaidBookingCount(rs.getInt("PaidBookingCount"));
                    row.setRevenue(getBigDecimalOrZero(rs, "Revenue"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getMonthlyRevenue");
        }

        return rows;
    }

    public List<DailyRevenueRow> getDailyRevenue(LocalDate fromDate, LocalDate toDate) {
        DateRange range = normalizeDateRange(fromDate, toDate);

        String sql = """
                SELECT
                    CAST(PaymentDate AS DATE) AS RevenueDate,
                    COUNT(DISTINCT BookingID) AS PaidBookingCount,
                    ISNULL(SUM(Amount), 0) AS Revenue
                FROM PAYMENTS
                WHERE PaymentStatus = 'SUCCESS'
                  AND CAST(PaymentDate AS DATE) BETWEEN ? AND ?
                GROUP BY CAST(PaymentDate AS DATE)
                ORDER BY RevenueDate ASC
                """;

        List<DailyRevenueRow> rows = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setDate(1, Date.valueOf(range.fromDate));
            ps.setDate(2, Date.valueOf(range.toDate));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DailyRevenueRow row = new DailyRevenueRow();

                    Date revenueDate = rs.getDate("RevenueDate");
                    if (revenueDate != null) {
                        row.setRevenueDate(revenueDate.toLocalDate());
                    }

                    row.setPaidBookingCount(rs.getInt("PaidBookingCount"));
                    row.setRevenue(getBigDecimalOrZero(rs, "Revenue"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getDailyRevenue");
        }

        return rows;
    }

    public List<PaymentRevenueRow> getRecentSuccessfulPayments(int limit) {
        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    p.PaymentID,
                    p.BookingID,
                    p.Amount,
                    p.PaymentMethod,
                    p.TransactionID,
                    p.PaymentDate,
                    b.UserID,
                    t.TourID,
                    t.TourName
                FROM PAYMENTS p
                JOIN BOOKINGS b ON b.BookingID = p.BookingID
                JOIN TOURS t ON t.TourID = b.TourID
                WHERE p.PaymentStatus = 'SUCCESS'
                ORDER BY p.PaymentDate DESC, p.PaymentID DESC
                """;

        List<PaymentRevenueRow> rows = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, validLimit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PaymentRevenueRow row = new PaymentRevenueRow();

                    row.setPaymentId(rs.getInt("PaymentID"));
                    row.setBookingId(rs.getInt("BookingID"));
                    row.setAmount(getBigDecimalOrZero(rs, "Amount"));
                    row.setPaymentMethod(rs.getString("PaymentMethod"));
                    row.setTransactionId(rs.getString("TransactionID"));

                    Timestamp paymentDate = rs.getTimestamp("PaymentDate");
                    if (paymentDate != null) {
                        row.setPaymentDate(paymentDate.toLocalDateTime());
                    }

                    row.setUserId(rs.getInt("UserID"));
                    row.setTourId(rs.getInt("TourID"));
                    row.setTourName(rs.getString("TourName"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getRecentSuccessfulPayments");
        }

        return rows;
    }

    public List<RefundRevenueRow> getRecentSuccessfulRefunds(int limit) {
        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    r.RefundID,
                    r.BookingID,
                    r.RefundAmount,
                    r.RefundMethod,
                    r.TransactionID,
                    r.RefundDate,
                    b.UserID,
                    t.TourID,
                    t.TourName
                FROM REFUNDS r
                JOIN BOOKINGS b ON b.BookingID = r.BookingID
                JOIN TOURS t ON t.TourID = b.TourID
                WHERE r.RefundStatus = 'SUCCESS'
                ORDER BY r.RefundDate DESC, r.RefundID DESC
                """;

        List<RefundRevenueRow> rows = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, validLimit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RefundRevenueRow row = new RefundRevenueRow();

                    row.setRefundId(rs.getInt("RefundID"));
                    row.setBookingId(rs.getInt("BookingID"));
                    row.setRefundAmount(getBigDecimalOrZero(rs, "RefundAmount"));
                    row.setRefundMethod(rs.getString("RefundMethod"));
                    row.setTransactionId(rs.getString("TransactionID"));

                    Timestamp refundDate = rs.getTimestamp("RefundDate");
                    if (refundDate != null) {
                        row.setRefundDate(refundDate.toLocalDateTime());
                    }

                    row.setUserId(rs.getInt("UserID"));
                    row.setTourId(rs.getInt("TourID"));
                    row.setTourName(rs.getString("TourName"));

                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getRecentSuccessfulRefunds");
        }

        return rows;
    }

    private BigDecimal queryBigDecimal(String sql,
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
                    BigDecimal value = rs.getBigDecimal("Total");
                    return value == null ? BigDecimal.ZERO : value;
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return BigDecimal.ZERO;
    }

    private int queryInt(String sql,
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

    private BigDecimal getBigDecimalOrZero(ResultSet rs, String columnLabel) throws SQLException {
        BigDecimal value = rs.getBigDecimal(columnLabel);
        return value == null ? BigDecimal.ZERO : value;
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

    private String normalizeBookingStatus(String status) {
        if (status == null) {
            return null;
        }

        String value = status.trim().toUpperCase();

        if (STATUS_PENDING.equals(value)
                || STATUS_PAID.equals(value)
                || STATUS_COMPLETED.equals(value)
                || STATUS_CANCELLED.equals(value)) {
            return value;
        }

        return null;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }

        return Math.min(limit, 100);
    }

    private void handleException(SQLException e, String methodName) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();

        if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang PAYMENTS/REFUNDS/BOOKINGS/TOURS.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang/view lien quan den revenue.");
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

    public static class RevenueSummary {
        private LocalDate fromDate;
        private LocalDate toDate;
        private BigDecimal totalRevenue;
        private BigDecimal totalRefund;
        private BigDecimal netRevenue;
        private int pendingBookingCount;
        private int paidBookingCount;
        private int completedBookingCount;
        private int cancelledBookingCount;
        private int successfulPaymentCount;
        private int successfulRefundCount;

        public LocalDate getFromDate() {
            return fromDate;
        }

        public void setFromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
        }

        public LocalDate getToDate() {
            return toDate;
        }

        public void setToDate(LocalDate toDate) {
            this.toDate = toDate;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public void setTotalRevenue(BigDecimal totalRevenue) {
            this.totalRevenue = totalRevenue;
        }

        public BigDecimal getTotalRefund() {
            return totalRefund;
        }

        public void setTotalRefund(BigDecimal totalRefund) {
            this.totalRefund = totalRefund;
        }

        public BigDecimal getNetRevenue() {
            return netRevenue;
        }

        public void setNetRevenue(BigDecimal netRevenue) {
            this.netRevenue = netRevenue;
        }

        public int getPendingBookingCount() {
            return pendingBookingCount;
        }

        public void setPendingBookingCount(int pendingBookingCount) {
            this.pendingBookingCount = pendingBookingCount;
        }

        public int getPaidBookingCount() {
            return paidBookingCount;
        }

        public void setPaidBookingCount(int paidBookingCount) {
            this.paidBookingCount = paidBookingCount;
        }

        public int getCompletedBookingCount() {
            return completedBookingCount;
        }

        public void setCompletedBookingCount(int completedBookingCount) {
            this.completedBookingCount = completedBookingCount;
        }

        public int getCancelledBookingCount() {
            return cancelledBookingCount;
        }

        public void setCancelledBookingCount(int cancelledBookingCount) {
            this.cancelledBookingCount = cancelledBookingCount;
        }

        public int getSuccessfulPaymentCount() {
            return successfulPaymentCount;
        }

        public void setSuccessfulPaymentCount(int successfulPaymentCount) {
            this.successfulPaymentCount = successfulPaymentCount;
        }

        public int getSuccessfulRefundCount() {
            return successfulRefundCount;
        }

        public void setSuccessfulRefundCount(int successfulRefundCount) {
            this.successfulRefundCount = successfulRefundCount;
        }

        @Override
        public String toString() {
            return "RevenueSummary{" +
                    "fromDate=" + fromDate +
                    ", toDate=" + toDate +
                    ", totalRevenue=" + totalRevenue +
                    ", totalRefund=" + totalRefund +
                    ", netRevenue=" + netRevenue +
                    ", pendingBookingCount=" + pendingBookingCount +
                    ", paidBookingCount=" + paidBookingCount +
                    ", completedBookingCount=" + completedBookingCount +
                    ", cancelledBookingCount=" + cancelledBookingCount +
                    ", successfulPaymentCount=" + successfulPaymentCount +
                    ", successfulRefundCount=" + successfulRefundCount +
                    '}';
        }
    }

    public static class TourRevenueRow {
        private int tourId;
        private String tourName;
        private int bookingCount;
        private BigDecimal revenue;

        public int getTourId() {
            return tourId;
        }

        public void setTourId(int tourId) {
            this.tourId = tourId;
        }

        public String getTourName() {
            return tourName;
        }

        public void setTourName(String tourName) {
            this.tourName = tourName;
        }

        public int getBookingCount() {
            return bookingCount;
        }

        public void setBookingCount(int bookingCount) {
            this.bookingCount = bookingCount;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        @Override
        public String toString() {
            return "TourRevenueRow{" +
                    "tourId=" + tourId +
                    ", tourName='" + tourName + '\'' +
                    ", bookingCount=" + bookingCount +
                    ", revenue=" + revenue +
                    '}';
        }
    }

    public static class MonthlyRevenueRow {
        private int year;
        private int month;
        private int paidBookingCount;
        private BigDecimal revenue;

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public int getMonth() {
            return month;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public int getPaidBookingCount() {
            return paidBookingCount;
        }

        public void setPaidBookingCount(int paidBookingCount) {
            this.paidBookingCount = paidBookingCount;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        @Override
        public String toString() {
            return "MonthlyRevenueRow{" +
                    "year=" + year +
                    ", month=" + month +
                    ", paidBookingCount=" + paidBookingCount +
                    ", revenue=" + revenue +
                    '}';
        }
    }

    public static class DailyRevenueRow {
        private LocalDate revenueDate;
        private int paidBookingCount;
        private BigDecimal revenue;

        public LocalDate getRevenueDate() {
            return revenueDate;
        }

        public void setRevenueDate(LocalDate revenueDate) {
            this.revenueDate = revenueDate;
        }

        public int getPaidBookingCount() {
            return paidBookingCount;
        }

        public void setPaidBookingCount(int paidBookingCount) {
            this.paidBookingCount = paidBookingCount;
        }

        public BigDecimal getRevenue() {
            return revenue;
        }

        public void setRevenue(BigDecimal revenue) {
            this.revenue = revenue;
        }

        @Override
        public String toString() {
            return "DailyRevenueRow{" +
                    "revenueDate=" + revenueDate +
                    ", paidBookingCount=" + paidBookingCount +
                    ", revenue=" + revenue +
                    '}';
        }
    }

    public static class PaymentRevenueRow {
        private int paymentId;
        private int bookingId;
        private BigDecimal amount;
        private String paymentMethod;
        private String transactionId;
        private LocalDateTime paymentDate;
        private int userId;
        private int tourId;
        private String tourName;

        public int getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(int paymentId) {
            this.paymentId = paymentId;
        }

        public int getBookingId() {
            return bookingId;
        }

        public void setBookingId(int bookingId) {
            this.bookingId = bookingId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public LocalDateTime getPaymentDate() {
            return paymentDate;
        }

        public void setPaymentDate(LocalDateTime paymentDate) {
            this.paymentDate = paymentDate;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public int getTourId() {
            return tourId;
        }

        public void setTourId(int tourId) {
            this.tourId = tourId;
        }

        public String getTourName() {
            return tourName;
        }

        public void setTourName(String tourName) {
            this.tourName = tourName;
        }

        @Override
        public String toString() {
            return "PaymentRevenueRow{" +
                    "paymentId=" + paymentId +
                    ", bookingId=" + bookingId +
                    ", amount=" + amount +
                    ", paymentMethod='" + paymentMethod + '\'' +
                    ", transactionId='" + transactionId + '\'' +
                    ", paymentDate=" + paymentDate +
                    ", userId=" + userId +
                    ", tourId=" + tourId +
                    ", tourName='" + tourName + '\'' +
                    '}';
        }
    }

    public static class RefundRevenueRow {
        private int refundId;
        private int bookingId;
        private BigDecimal refundAmount;
        private String refundMethod;
        private String transactionId;
        private LocalDateTime refundDate;
        private int userId;
        private int tourId;
        private String tourName;

        public int getRefundId() {
            return refundId;
        }

        public void setRefundId(int refundId) {
            this.refundId = refundId;
        }

        public int getBookingId() {
            return bookingId;
        }

        public void setBookingId(int bookingId) {
            this.bookingId = bookingId;
        }

        public BigDecimal getRefundAmount() {
            return refundAmount;
        }

        public void setRefundAmount(BigDecimal refundAmount) {
            this.refundAmount = refundAmount;
        }

        public String getRefundMethod() {
            return refundMethod;
        }

        public void setRefundMethod(String refundMethod) {
            this.refundMethod = refundMethod;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public LocalDateTime getRefundDate() {
            return refundDate;
        }

        public void setRefundDate(LocalDateTime refundDate) {
            this.refundDate = refundDate;
        }

        public int getUserId() {
            return userId;
        }

        public void setUserId(int userId) {
            this.userId = userId;
        }

        public int getTourId() {
            return tourId;
        }

        public void setTourId(int tourId) {
            this.tourId = tourId;
        }

        public String getTourName() {
            return tourName;
        }

        public void setTourName(String tourName) {
            this.tourName = tourName;
        }

        @Override
        public String toString() {
            return "RefundRevenueRow{" +
                    "refundId=" + refundId +
                    ", bookingId=" + bookingId +
                    ", refundAmount=" + refundAmount +
                    ", refundMethod='" + refundMethod + '\'' +
                    ", transactionId='" + transactionId + '\'' +
                    ", refundDate=" + refundDate +
                    ", userId=" + userId +
                    ", tourId=" + tourId +
                    ", tourName='" + tourName + '\'' +
                    '}';
        }
    }
}
