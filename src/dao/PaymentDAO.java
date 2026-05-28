package dao;

import config.DatabaseConnection;
import model.Payment;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    private static final int MAX_PAYMENT_METHOD_LENGTH = 50;
    private static final int MAX_TRANSACTION_ID_LENGTH = 100;

    public int payBooking(int bookingId,
                          BigDecimal amount,
                          String paymentMethod,
                          String transactionId) {
        PaymentInput input = validatePaymentInput(
                bookingId,
                amount,
                paymentMethod,
                transactionId
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        BookingPaymentSnapshot booking = getBookingPaymentSnapshot(input.bookingId);

        if (booking == null) {
            System.out.println("Booking khong ton tai.");
            return -1;
        }

        if (!STATUS_PENDING.equalsIgnoreCase(booking.status)) {
            System.out.println("Chi thanh toan duoc booking co trang thai PENDING.");
            System.out.println("Trang thai hien tai = " + booking.status);
            return -1;
        }

        if (booking.finalPrice == null || booking.finalPrice.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("FinalPrice khong hop le.");
            return -1;
        }

        /*
         * SQL trigger cho phép dung sai 1 đồng.
         * Java cũng kiểm tra trước để báo lỗi dễ hiểu hơn.
         */
        if (input.amount.subtract(booking.finalPrice).abs().compareTo(BigDecimal.ONE) > 0) {
            System.out.println("So tien thanh toan khong khop FinalPrice.");
            System.out.println("Amount     = " + input.amount);
            System.out.println("FinalPrice = " + booking.finalPrice);
            return -1;
        }

        if (hasSuccessfulPayment(input.bookingId)) {
            System.out.println("Booking nay da co payment SUCCESS.");
            return -1;
        }

        String sql = """
                INSERT INTO PAYMENTS
                (
                    BookingID,
                    Amount,
                    PaymentMethod,
                    TransactionID,
                    PaymentStatus
                )
                VALUES (?, ?, ?, ?, 'SUCCESS')
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.bookingId);
            ps.setBigDecimal(2, input.amount);
            ps.setString(3, input.paymentMethod);
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
            handleException(e, "payBooking");
        }

        return -1;
    }

    public int createPendingPayment(int bookingId,
                                    BigDecimal amount,
                                    String paymentMethod,
                                    String transactionId) {
        PaymentInput input = validatePaymentInput(
                bookingId,
                amount,
                paymentMethod,
                transactionId
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        BookingPaymentSnapshot booking = getBookingPaymentSnapshot(input.bookingId);

        if (booking == null) {
            System.out.println("Booking khong ton tai.");
            return -1;
        }

        if (!STATUS_PENDING.equalsIgnoreCase(booking.status)) {
            System.out.println("Chi tao payment PENDING cho booking PENDING.");
            return -1;
        }

        if (hasSuccessfulPayment(input.bookingId)) {
            System.out.println("Booking nay da co payment SUCCESS.");
            return -1;
        }

        String sql = """
                INSERT INTO PAYMENTS
                (
                    BookingID,
                    Amount,
                    PaymentMethod,
                    TransactionID,
                    PaymentStatus
                )
                VALUES (?, ?, ?, ?, 'PENDING')
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.bookingId);
            ps.setBigDecimal(2, input.amount);
            ps.setString(3, input.paymentMethod);
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
            handleException(e, "createPendingPayment");
        }

        return -1;
    }

    public boolean markPaymentSuccess(int paymentId) {
        if (paymentId <= 0) {
            System.out.println("PaymentID khong hop le.");
            return false;
        }

        Payment current = getPaymentById(paymentId);

        if (current == null) {
            System.out.println("Khong tim thay payment.");
            return false;
        }

        if (STATUS_SUCCESS.equalsIgnoreCase(current.getPaymentStatus())) {
            System.out.println("Payment da SUCCESS.");
            return true;
        }

        if (!STATUS_PENDING.equalsIgnoreCase(current.getPaymentStatus())) {
            System.out.println("Chi chuyen payment PENDING sang SUCCESS.");
            return false;
        }

        BookingPaymentSnapshot booking = getBookingPaymentSnapshot(current.getBookingId());

        if (booking == null || !STATUS_PENDING.equalsIgnoreCase(booking.status)) {
            System.out.println("Booking khong con PENDING, khong the thanh toan.");
            return false;
        }

        if (current.getAmount() == null
                || booking.finalPrice == null
                || current.getAmount().subtract(booking.finalPrice).abs().compareTo(BigDecimal.ONE) > 0) {
            System.out.println("So tien payment khong khop FinalPrice.");
            return false;
        }

        if (hasSuccessfulPayment(current.getBookingId())) {
            System.out.println("Booking nay da co payment SUCCESS.");
            return false;
        }

        String sql = """
                UPDATE PAYMENTS
                SET PaymentStatus = 'SUCCESS',
                    PaymentDate = GETDATE()
                WHERE PaymentID = ?
                  AND PaymentStatus = 'PENDING'
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, paymentId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat payment SUCCESS that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "markPaymentSuccess");
        }

        return false;
    }

    public boolean markPaymentFailed(int paymentId) {
        if (paymentId <= 0) {
            System.out.println("PaymentID khong hop le.");
            return false;
        }

        Payment current = getPaymentById(paymentId);

        if (current == null) {
            System.out.println("Khong tim thay payment.");
            return false;
        }

        if (!STATUS_PENDING.equalsIgnoreCase(current.getPaymentStatus())) {
            System.out.println("Chi chuyen payment PENDING sang FAILED.");
            return false;
        }

        String sql = """
                UPDATE PAYMENTS
                SET PaymentStatus = 'FAILED',
                    PaymentDate = GETDATE()
                WHERE PaymentID = ?
                  AND PaymentStatus = 'PENDING'
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, paymentId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat payment FAILED that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "markPaymentFailed");
        }

        return false;
    }

    public Payment getPaymentById(int paymentId) {
        if (paymentId <= 0) {
            System.out.println("PaymentID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE PaymentID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, paymentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPayment(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getPaymentById");
        }

        return null;
    }

    public Payment getPaymentByBookingId(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return null;
        }

        String sql = """
                SELECT TOP 1
                    PaymentID,
                    BookingID,
                    Amount,
                    PaymentMethod,
                    TransactionID,
                    PaymentStatus,
                    PaymentDate,
                    CreatedAt
                FROM PAYMENTS
                WHERE BookingID = ?
                ORDER BY PaymentDate DESC, PaymentID DESC
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapPayment(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getPaymentByBookingId");
        }

        return null;
    }

    public List<Payment> getPaymentsByBookingId(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE BookingID = ?
                ORDER BY PaymentDate DESC, PaymentID DESC
                """);

        return queryPaymentList(
                sql,
                ps -> ps.setInt(1, bookingId),
                "getPaymentsByBookingId"
        );
    }

    public List<Payment> getPaymentsByStatus(String paymentStatus) {
        String status = normalizePaymentStatus(paymentStatus);

        if (status == null) {
            System.out.println("PaymentStatus chi chap nhan PENDING, SUCCESS, FAILED.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE PaymentStatus = ?
                ORDER BY PaymentDate DESC, PaymentID DESC
                """);

        return queryPaymentList(
                sql,
                ps -> ps.setString(1, status),
                "getPaymentsByStatus"
        );
    }

    public List<Payment> getRecentPayments(int limit) {
        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    PaymentID,
                    BookingID,
                    Amount,
                    PaymentMethod,
                    TransactionID,
                    PaymentStatus,
                    PaymentDate,
                    CreatedAt
                FROM PAYMENTS
                ORDER BY PaymentDate DESC, PaymentID DESC
                """;

        return queryPaymentList(
                sql,
                ps -> ps.setInt(1, validLimit),
                "getRecentPayments"
        );
    }

    public boolean hasSuccessfulPayment(int bookingId) {
        if (bookingId <= 0) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM PAYMENTS
                WHERE BookingID = ?
                  AND PaymentStatus = 'SUCCESS'
                """;

        return queryInt(
                sql,
                ps -> ps.setInt(1, bookingId),
                "hasSuccessfulPayment"
        ) > 0;
    }

    private BookingPaymentSnapshot getBookingPaymentSnapshot(int bookingId) {
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
                    BookingPaymentSnapshot snapshot = new BookingPaymentSnapshot();
                    snapshot.finalPrice = rs.getBigDecimal("FinalPrice");
                    snapshot.status = rs.getString("Status");
                    return snapshot;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getBookingPaymentSnapshot");
        }

        return null;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    PaymentID,
                    BookingID,
                    Amount,
                    PaymentMethod,
                    TransactionID,
                    PaymentStatus,
                    PaymentDate,
                    CreatedAt
                FROM PAYMENTS
                """ + condition;
    }

    private List<Payment> queryPaymentList(String sql,
                                           SqlSetter setter,
                                           String methodName) {
        List<Payment> payments = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(mapPayment(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return payments;
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

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();

        payment.setPaymentId(rs.getInt("PaymentID"));
        payment.setBookingId(rs.getInt("BookingID"));
        payment.setAmount(rs.getBigDecimal("Amount"));
        payment.setPaymentMethod(rs.getString("PaymentMethod"));
        payment.setTransactionId(rs.getString("TransactionID"));
        payment.setPaymentStatus(rs.getString("PaymentStatus"));

        Timestamp paymentDate = rs.getTimestamp("PaymentDate");
        if (paymentDate != null) {
            payment.setPaymentDate(paymentDate.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            payment.setCreatedAt(createdAt.toLocalDateTime());
        }

        return payment;
    }

    private PaymentInput validatePaymentInput(int bookingId,
                                              BigDecimal amount,
                                              String paymentMethod,
                                              String transactionId) {
        String cleanPaymentMethod = cleanString(paymentMethod);
        String cleanTransactionId = cleanString(transactionId);

        if (bookingId <= 0) {
            return PaymentInput.invalid("BookingID khong hop le.");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentInput.invalid("So tien thanh toan phai lon hon 0.");
        }

        if (cleanPaymentMethod == null || cleanPaymentMethod.length() > MAX_PAYMENT_METHOD_LENGTH) {
            return PaymentInput.invalid("PaymentMethod khong hop le, toi da 50 ky tu.");
        }

        if (cleanTransactionId != null && cleanTransactionId.length() > MAX_TRANSACTION_ID_LENGTH) {
            return PaymentInput.invalid("TransactionID qua dai, toi da 100 ky tu.");
        }

        return PaymentInput.valid(
                bookingId,
                amount,
                cleanPaymentMethod,
                cleanTransactionId
        );
    }

    private String normalizePaymentStatus(String paymentStatus) {
        String status = cleanString(paymentStatus);

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

        if (errorCode == 50040) {
            System.out.println("Loi " + methodName + ": So tien thanh toan khong khop FinalPrice.");
        } else if (errorCode == 50041) {
            System.out.println("Loi " + methodName + ": Khong the thanh toan booking da huy hoac da hoan thanh.");
        } else if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": BookingID khong ton tai hoac vi pham khoa ngoai.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Booking nay da co payment SUCCESS hoac transaction bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang PAYMENTS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang PAYMENTS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class BookingPaymentSnapshot {
        private BigDecimal finalPrice;
        private String status;
    }

    private static class PaymentInput {
        private final boolean valid;
        private final String message;
        private final int bookingId;
        private final BigDecimal amount;
        private final String paymentMethod;
        private final String transactionId;

        private PaymentInput(boolean valid,
                             String message,
                             int bookingId,
                             BigDecimal amount,
                             String paymentMethod,
                             String transactionId) {
            this.valid = valid;
            this.message = message;
            this.bookingId = bookingId;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.transactionId = transactionId;
        }

        private static PaymentInput valid(int bookingId,
                                          BigDecimal amount,
                                          String paymentMethod,
                                          String transactionId) {
            return new PaymentInput(
                    true,
                    null,
                    bookingId,
                    amount,
                    paymentMethod,
                    transactionId
            );
        }

        private static PaymentInput invalid(String message) {
            return new PaymentInput(
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
