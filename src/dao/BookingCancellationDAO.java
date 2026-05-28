package dao;

import config.DatabaseConnection;
import model.BookingCancellation;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class BookingCancellationDAO {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    /*
     * SQL hiện tại chỉ cho phép CancelBy IN ('CUSTOMER','COMPANY').
     * STAFF / MANAGER / SYSTEM là người thao tác ở BE,
     * nhưng khi lưu DB sẽ quy về COMPANY.
     */
    private static final String CANCEL_BY_CUSTOMER = "CUSTOMER";
    private static final String CANCEL_BY_COMPANY = "COMPANY";
    private static final String CANCEL_BY_STAFF = "STAFF";
    private static final String CANCEL_BY_MANAGER = "MANAGER";
    private static final String CANCEL_BY_SYSTEM = "SYSTEM";

    private static final int MAX_CANCEL_REASON_LENGTH = 1000;

    public int cancelBooking(int bookingId,
                             String cancelBy,
                             String cancelReason,
                             BigDecimal refundPercent) {
        CancellationInput input = validateCancellationInput(
                bookingId,
                cancelBy,
                cancelReason,
                refundPercent
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        BookingSnapshot booking = getBookingSnapshot(input.bookingId);

        if (booking == null) {
            System.out.println("BookingID khong ton tai.");
            return -1;
        }

        if (!canCancelStatus(booking.status)) {
            return -1;
        }

        if (hasCancellation(input.bookingId)) {
            System.out.println("Booking da co ban ghi huy, khong huy lan 2.");
            return -1;
        }

        BigDecimal refundAmount = calculateRefundAmount(
                booking.finalPrice,
                input.refundPercent
        );

        String sql = """
                INSERT INTO BOOKING_CANCELLATIONS
                (
                    BookingID,
                    CancelBy,
                    CancelReason,
                    RefundPercent,
                    RefundAmount
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        /*
         * Không tự UPDATE BOOKINGS.Status ở Java.
         * SQL trigger trg_SyncBookingCancellation sẽ tự chuyển booking sang CANCELLED
         * sau khi insert thành công vào BOOKING_CANCELLATIONS.
         */
        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.bookingId);
            ps.setString(2, input.cancelBy);
            ps.setString(3, input.cancelReason);
            ps.setBigDecimal(4, input.refundPercent);
            ps.setBigDecimal(5, refundAmount);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "cancelBooking");
        }

        return -1;
    }

    public boolean canCancelBooking(int bookingId) {
        if (bookingId <= 0) {
            return false;
        }

        BookingSnapshot booking = getBookingSnapshot(bookingId);

        if (booking == null) {
            return false;
        }

        if (hasCancellation(bookingId)) {
            return false;
        }

        return canCancelStatus(booking.status);
    }

    public BigDecimal calculateExpectedRefundAmount(int bookingId, BigDecimal refundPercent) {
        if (bookingId <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal validRefundPercent = normalizeRefundPercent(refundPercent);

        if (validRefundPercent == null) {
            return BigDecimal.ZERO;
        }

        BookingSnapshot booking = getBookingSnapshot(bookingId);

        if (booking == null || booking.finalPrice == null) {
            return BigDecimal.ZERO;
        }

        return calculateRefundAmount(booking.finalPrice, validRefundPercent);
    }

    public BookingCancellation getCancellationById(int bookingCancelId) {
        if (bookingCancelId <= 0) {
            System.out.println("BookingCancelID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE BookingCancelID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingCancelId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBookingCancellation(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getCancellationById");
        }

        return null;
    }

    public BookingCancellation getCancellationByBookingId(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE BookingID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBookingCancellation(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getCancellationByBookingId");
        }

        return null;
    }

    public List<BookingCancellation> getAllCancellations() {
        String sql = buildSelectSql("""
                ORDER BY CreatedAt DESC, BookingCancelID DESC
                """);

        return queryCancellationList(sql, null, "getAllCancellations");
    }

    public List<BookingCancellation> getCancellationsByCancelBy(String cancelBy) {
        String normalizedCancelBy = normalizeCancelBy(cancelBy);

        if (normalizedCancelBy == null) {
            System.out.println("CancelBy chi chap nhan CUSTOMER, COMPANY, STAFF, MANAGER, SYSTEM.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE CancelBy = ?
                ORDER BY CreatedAt DESC, BookingCancelID DESC
                """);

        return queryCancellationList(
                sql,
                ps -> ps.setString(1, normalizedCancelBy),
                "getCancellationsByCancelBy"
        );
    }

    public boolean updateCancellationReason(int bookingCancelId, String cancelReason) {
        String cleanReason = cleanString(cancelReason);

        if (bookingCancelId <= 0) {
            System.out.println("BookingCancelID khong hop le.");
            return false;
        }

        if (!isValidCancelReason(cleanReason)) {
            System.out.println("CancelReason khong hop le, toi da 1000 ky tu.");
            return false;
        }

        if (getCancellationById(bookingCancelId) == null) {
            System.out.println("Khong tim thay cancellation.");
            return false;
        }

        String sql = """
                UPDATE BOOKING_CANCELLATIONS
                SET CancelReason = ?
                WHERE BookingCancelID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanReason);
            ps.setInt(2, bookingCancelId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat cancellation reason that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateCancellationReason");
        }

        return false;
    }

    public boolean hasCancellation(int bookingId) {
        if (bookingId <= 0) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKING_CANCELLATIONS
                WHERE BookingID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, bookingId),
                "hasCancellation"
        ) > 0;
    }

    private BookingSnapshot getBookingSnapshot(int bookingId) {
        String sql = """
                SELECT
                    FinalPrice,
                    Status
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
                    BookingSnapshot snapshot = new BookingSnapshot();
                    snapshot.finalPrice = rs.getBigDecimal("FinalPrice");
                    snapshot.status = rs.getString("Status");
                    return snapshot;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getBookingSnapshot");
        }

        return null;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    BookingCancelID,
                    BookingID,
                    CancelBy,
                    CancelReason,
                    RefundPercent,
                    RefundAmount,
                    CreatedAt
                FROM BOOKING_CANCELLATIONS
                """ + condition;
    }

    private List<BookingCancellation> queryCancellationList(String sql,
                                                            SqlSetter setter,
                                                            String methodName) {
        List<BookingCancellation> cancellations = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cancellations.add(mapBookingCancellation(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return cancellations;
    }

    private BookingCancellation mapBookingCancellation(ResultSet rs) throws SQLException {
        BookingCancellation cancellation = new BookingCancellation();

        cancellation.setBookingCancelId(rs.getInt("BookingCancelID"));
        cancellation.setBookingId(rs.getInt("BookingID"));
        cancellation.setCancelBy(rs.getString("CancelBy"));
        cancellation.setCancelReason(rs.getString("CancelReason"));
        cancellation.setRefundPercent(rs.getBigDecimal("RefundPercent"));
        cancellation.setRefundAmount(rs.getBigDecimal("RefundAmount"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            cancellation.setCreatedAt(createdAt.toLocalDateTime());
        }

        return cancellation;
    }

    private CancellationInput validateCancellationInput(int bookingId,
                                                        String cancelBy,
                                                        String cancelReason,
                                                        BigDecimal refundPercent) {
        String normalizedCancelBy = normalizeCancelBy(cancelBy);
        String cleanReason = cleanString(cancelReason);
        BigDecimal validRefundPercent = normalizeRefundPercent(refundPercent);

        if (bookingId <= 0) {
            return CancellationInput.invalid("BookingID khong hop le.");
        }

        if (normalizedCancelBy == null) {
            return CancellationInput.invalid("CancelBy chi chap nhan CUSTOMER, COMPANY, STAFF, MANAGER, SYSTEM.");
        }

        if (!isValidCancelReason(cleanReason)) {
            return CancellationInput.invalid("CancelReason khong hop le, toi da 1000 ky tu.");
        }

        if (validRefundPercent == null) {
            return CancellationInput.invalid("RefundPercent phai tu 0 den 100.");
        }

        return CancellationInput.valid(
                bookingId,
                normalizedCancelBy,
                cleanReason,
                validRefundPercent
        );
    }

    private String normalizeCancelBy(String cancelBy) {
        String value = cleanString(cancelBy);

        if (value == null) {
            return null;
        }

        value = value.toUpperCase();

        if (CANCEL_BY_CUSTOMER.equals(value)) {
            return CANCEL_BY_CUSTOMER;
        }

        if (CANCEL_BY_COMPANY.equals(value)
                || CANCEL_BY_STAFF.equals(value)
                || CANCEL_BY_MANAGER.equals(value)
                || CANCEL_BY_SYSTEM.equals(value)) {
            return CANCEL_BY_COMPANY;
        }

        return null;
    }

    private BigDecimal normalizeRefundPercent(BigDecimal refundPercent) {
        if (refundPercent == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        if (refundPercent.compareTo(BigDecimal.ZERO) < 0
                || refundPercent.compareTo(new BigDecimal("100")) > 0) {
            return null;
        }

        return refundPercent.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRefundAmount(BigDecimal finalPrice, BigDecimal refundPercent) {
        if (finalPrice == null || refundPercent == null) {
            return BigDecimal.ZERO;
        }

        return finalPrice
                .multiply(refundPercent)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private boolean canCancelStatus(String bookingStatus) {
        String status = cleanString(bookingStatus);

        if (status == null) {
            return false;
        }

        status = status.toUpperCase();

        if (STATUS_PENDING.equals(status) || STATUS_PAID.equals(status)) {
            return true;
        }

        if (STATUS_COMPLETED.equals(status)) {
            System.out.println("Booking da COMPLETED, khong the huy.");
            return false;
        }

        if (STATUS_CANCELLED.equals(status)) {
            System.out.println("Booking da CANCELLED, khong the huy lan nua.");
            return false;
        }

        System.out.println("Trang thai booking khong ho tro huy: " + status);
        return false;
    }

    private boolean isValidCancelReason(String cancelReason) {
        return cancelReason != null
                && !cancelReason.trim().isEmpty()
                && cancelReason.length() <= MAX_CANCEL_REASON_LENGTH;
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

        if (errorCode == 50044) {
            System.out.println("Loi " + methodName + ": RefundAmount vuot qua FinalPrice.");
        } else if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": BookingID khong ton tai hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Booking nay da co ban ghi huy.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang BOOKING_CANCELLATIONS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang BOOKING_CANCELLATIONS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class BookingSnapshot {
        private BigDecimal finalPrice;
        private String status;
    }

    private static class CancellationInput {
        private final boolean valid;
        private final String message;
        private final int bookingId;
        private final String cancelBy;
        private final String cancelReason;
        private final BigDecimal refundPercent;

        private CancellationInput(boolean valid,
                                  String message,
                                  int bookingId,
                                  String cancelBy,
                                  String cancelReason,
                                  BigDecimal refundPercent) {
            this.valid = valid;
            this.message = message;
            this.bookingId = bookingId;
            this.cancelBy = cancelBy;
            this.cancelReason = cancelReason;
            this.refundPercent = refundPercent;
        }

        private static CancellationInput valid(int bookingId,
                                               String cancelBy,
                                               String cancelReason,
                                               BigDecimal refundPercent) {
            return new CancellationInput(
                    true,
                    null,
                    bookingId,
                    cancelBy,
                    cancelReason,
                    refundPercent
            );
        }

        private static CancellationInput invalid(String message) {
            return new CancellationInput(
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
