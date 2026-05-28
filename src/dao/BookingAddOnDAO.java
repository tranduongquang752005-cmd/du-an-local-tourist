package dao;

import config.DatabaseConnection;
import model.BookingAddOn;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class BookingAddOnDAO {

    private static final String BOOKING_STATUS_PENDING = "PENDING";

    public int addBookingAddOn(int bookingId, int addOnId, int quantity) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return -1;
        }

        if (addOnId <= 0) {
            System.out.println("AddOnID khong hop le.");
            return -1;
        }

        if (quantity <= 0) {
            System.out.println("Quantity phai lon hon 0.");
            return -1;
        }

        BookingSnapshot booking = getBookingSnapshot(bookingId);

        if (booking == null) {
            System.out.println("BookingID khong ton tai.");
            return -1;
        }

        if (!BOOKING_STATUS_PENDING.equalsIgnoreCase(booking.status)) {
            System.out.println("Chi duoc them add-on khi booking con PENDING.");
            return -1;
        }

        AddOnSnapshot addOn = getActiveAddOnSnapshot(addOnId);

        if (addOn == null) {
            System.out.println("AddOnID khong ton tai hoac dang bi tat.");
            return -1;
        }

        BookingAddOn duplicated = getBookingAddOnByBookingAndAddOn(bookingId, addOnId);

        if (duplicated != null) {
            System.out.println("Booking da co add-on nay. Hay dung updateQuantity().");
            return -1;
        }

        String sql = """
                INSERT INTO BOOKING_ADD_ONS
                (
                    BookingID,
                    AddOnID,
                    Quantity,
                    Price
                )
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, bookingId);
            ps.setInt(2, addOnId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, addOn.price);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "addBookingAddOn");
        }

        return -1;
    }

    public boolean updateQuantity(int bookingAddOnId, int quantity) {
        if (bookingAddOnId <= 0) {
            System.out.println("BookingAddOnID khong hop le.");
            return false;
        }

        if (quantity <= 0) {
            System.out.println("Quantity phai lon hon 0.");
            return false;
        }

        BookingAddOn current = getBookingAddOnById(bookingAddOnId);

        if (current == null) {
            System.out.println("Khong tim thay booking add-on.");
            return false;
        }

        BookingSnapshot booking = getBookingSnapshot(current.getBookingId());

        if (booking == null || !BOOKING_STATUS_PENDING.equalsIgnoreCase(booking.status)) {
            System.out.println("Chi duoc sua add-on khi booking con PENDING.");
            return false;
        }

        String sql = """
                UPDATE BOOKING_ADD_ONS
                SET Quantity = ?
                WHERE BookingAddOnID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, quantity);
            ps.setInt(2, bookingAddOnId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat quantity that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateQuantity");
        }

        return false;
    }

    public boolean deleteBookingAddOn(int bookingAddOnId) {
        if (bookingAddOnId <= 0) {
            System.out.println("BookingAddOnID khong hop le.");
            return false;
        }

        BookingAddOn current = getBookingAddOnById(bookingAddOnId);

        if (current == null) {
            System.out.println("Khong tim thay booking add-on.");
            return false;
        }

        BookingSnapshot booking = getBookingSnapshot(current.getBookingId());

        if (booking == null || !BOOKING_STATUS_PENDING.equalsIgnoreCase(booking.status)) {
            System.out.println("Chi duoc xoa add-on khi booking con PENDING.");
            return false;
        }

        String sql = """
                DELETE FROM BOOKING_ADD_ONS
                WHERE BookingAddOnID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingAddOnId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Xoa booking add-on that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteBookingAddOn");
        }

        return false;
    }

    public BookingAddOn getBookingAddOnById(int bookingAddOnId) {
        if (bookingAddOnId <= 0) {
            System.out.println("BookingAddOnID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE ba.BookingAddOnID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingAddOnId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBookingAddOn(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getBookingAddOnById");
        }

        return null;
    }

    public BookingAddOn getBookingAddOnByBookingAndAddOn(int bookingId, int addOnId) {
        if (bookingId <= 0 || addOnId <= 0) {
            return null;
        }

        String sql = buildSelectSql("""
                WHERE ba.BookingID = ?
                  AND ba.AddOnID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);
            ps.setInt(2, addOnId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBookingAddOn(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getBookingAddOnByBookingAndAddOn");
        }

        return null;
    }

    public List<BookingAddOn> getAddOnsByBookingId(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE ba.BookingID = ?
                ORDER BY ba.BookingAddOnID ASC
                """);

        return queryBookingAddOnList(
                sql,
                ps -> ps.setInt(1, bookingId),
                "getAddOnsByBookingId"
        );
    }

    public BigDecimal getTotalAddOnAmountByBookingId(int bookingId) {
        if (bookingId <= 0) {
            return BigDecimal.ZERO;
        }

        String sql = """
                SELECT ISNULL(SUM(Quantity * Price), 0) AS Total
                FROM BOOKING_ADD_ONS
                WHERE BookingID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal total = rs.getBigDecimal("Total");
                    return total == null ? BigDecimal.ZERO : total;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTotalAddOnAmountByBookingId");
        }

        return BigDecimal.ZERO;
    }

    public int countAddOnsByBookingId(int bookingId) {
        if (bookingId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKING_ADD_ONS
                WHERE BookingID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, bookingId),
                "countAddOnsByBookingId"
        );
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    ba.BookingAddOnID,
                    ba.BookingID,
                    ba.AddOnID,
                    ba.Quantity,
                    ba.Price,
                    ba.CreatedAt,
                    ao.AddOnName
                FROM BOOKING_ADD_ONS ba
                JOIN ADD_ONS ao ON ao.AddOnID = ba.AddOnID
                """ + condition;
    }

    private List<BookingAddOn> queryBookingAddOnList(String sql,
                                                     SqlSetter setter,
                                                     String methodName) {
        List<BookingAddOn> bookingAddOns = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    bookingAddOns.add(mapBookingAddOn(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return bookingAddOns;
    }

    private BookingAddOn mapBookingAddOn(ResultSet rs) throws SQLException {
        BookingAddOn bookingAddOn = new BookingAddOn();

        bookingAddOn.setBookingAddOnId(rs.getInt("BookingAddOnID"));
        bookingAddOn.setBookingId(rs.getInt("BookingID"));
        bookingAddOn.setAddOnId(rs.getInt("AddOnID"));
        bookingAddOn.setQuantity(rs.getInt("Quantity"));
        bookingAddOn.setPrice(rs.getBigDecimal("Price"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            bookingAddOn.setCreatedAt(createdAt.toLocalDateTime());
        }

        trySetAddOnName(bookingAddOn, rs.getString("AddOnName"));

        return bookingAddOn;
    }

    /*
     * Một số model BookingAddOn cũ không có addOnName.
     * Nếu model của bạn có setAddOnName(String), hàm này sẽ set được qua reflection.
     * Nếu không có thì bỏ qua, không ảnh hưởng compile.
     */
    private void trySetAddOnName(BookingAddOn bookingAddOn, String addOnName) {
        try {
            bookingAddOn.getClass()
                    .getMethod("setAddOnName", String.class)
                    .invoke(bookingAddOn, addOnName);
        } catch (ReflectiveOperationException e) {
            // Model khong co field addOnName thi bo qua.
        }
    }

    private BookingSnapshot getBookingSnapshot(int bookingId) {
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
                    BookingSnapshot snapshot = new BookingSnapshot();
                    snapshot.status = rs.getString("Status");
                    return snapshot;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getBookingSnapshot");
        }

        return null;
    }

    private AddOnSnapshot getActiveAddOnSnapshot(int addOnId) {
        String sql = """
                SELECT Price
                FROM ADD_ONS
                WHERE AddOnID = ?
                  AND IsActive = 1
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, addOnId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    AddOnSnapshot snapshot = new AddOnSnapshot();
                    snapshot.price = rs.getBigDecimal("Price");
                    return snapshot;
                }
            }

        } catch (SQLException e) {
            handleException(e, "getActiveAddOnSnapshot");
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

    private void handleException(SQLException e, String methodName) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();

        if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": BookingID/AddOnID khong ton tai hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Booking da co add-on nay hoac du lieu bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang BOOKING_ADD_ONS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Quantity/Price khong hop le.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang BOOKING_ADD_ONS hoac ADD_ONS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class BookingSnapshot {
        private String status;
    }

    private static class AddOnSnapshot {
        private BigDecimal price;
    }
}
