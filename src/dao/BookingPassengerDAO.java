package dao;

import config.DatabaseConnection;
import model.BookingPassenger;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class BookingPassengerDAO {

    private static final String BOOKING_STATUS_PENDING = "PENDING";

    private static final String TYPE_ADULT = "ADULT";
    private static final String TYPE_CHILD = "CHILD";
    private static final String TYPE_BABY = "BABY";

    private static final int MAX_PASSENGER_NAME_LENGTH = 100;

    public int addPassenger(int bookingId,
                            String passengerName,
                            String passengerType,
                            BigDecimal price) {
        PassengerInput input = validatePassengerInput(
                bookingId,
                passengerName,
                passengerType,
                price
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

        if (!BOOKING_STATUS_PENDING.equalsIgnoreCase(booking.status)) {
            System.out.println("Chi duoc them hanh khach khi booking con PENDING.");
            return -1;
        }

        if (input.slotsOccupied > 0 && !hasEnoughSlots(input.bookingId, input.slotsOccupied)) {
            System.out.println("Khong du ghe trong de them hanh khach.");
            return -1;
        }

        String sql = """
                INSERT INTO BOOKING_PASSENGERS
                (
                    BookingID,
                    PassengerName,
                    PassengerType,
                    Price,
                    SlotsOccupied
                )
                VALUES (?, ?, ?, ?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.bookingId);
            ps.setString(2, input.passengerName);
            ps.setString(3, input.passengerType);
            ps.setBigDecimal(4, input.price);
            ps.setInt(5, input.slotsOccupied);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "addPassenger");
        }

        return -1;
    }

    public boolean updatePassenger(int passengerId,
                                   String passengerName,
                                   String passengerType,
                                   BigDecimal price) {
        if (passengerId <= 0) {
            System.out.println("PassengerID khong hop le.");
            return false;
        }

        BookingPassenger current = getPassengerById(passengerId);

        if (current == null) {
            System.out.println("Khong tim thay passenger.");
            return false;
        }

        BookingSnapshot booking = getBookingSnapshot(current.getBookingId());

        if (booking == null || !BOOKING_STATUS_PENDING.equalsIgnoreCase(booking.status)) {
            System.out.println("Chi duoc sua hanh khach khi booking con PENDING.");
            return false;
        }

        PassengerInput input = validatePassengerInput(
                current.getBookingId(),
                passengerName,
                passengerType,
                price
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        int slotDiff = input.slotsOccupied - current.getSlotsOccupied();

        if (slotDiff > 0 && !hasEnoughSlots(current.getBookingId(), slotDiff)) {
            System.out.println("Khong du ghe trong de doi loai hanh khach.");
            return false;
        }

        String sql = """
                UPDATE BOOKING_PASSENGERS
                SET PassengerName = ?,
                    PassengerType = ?,
                    Price = ?,
                    SlotsOccupied = ?
                WHERE PassengerID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, input.passengerName);
            ps.setString(2, input.passengerType);
            ps.setBigDecimal(3, input.price);
            ps.setInt(4, input.slotsOccupied);
            ps.setInt(5, passengerId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat passenger that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updatePassenger");
        }

        return false;
    }

    public boolean deletePassenger(int passengerId) {
        if (passengerId <= 0) {
            System.out.println("PassengerID khong hop le.");
            return false;
        }

        BookingPassenger current = getPassengerById(passengerId);

        if (current == null) {
            System.out.println("Khong tim thay passenger.");
            return false;
        }

        BookingSnapshot booking = getBookingSnapshot(current.getBookingId());

        if (booking == null || !BOOKING_STATUS_PENDING.equalsIgnoreCase(booking.status)) {
            System.out.println("Chi duoc xoa hanh khach khi booking con PENDING.");
            return false;
        }

        if (current.getSlotsOccupied() > 0
                && countOccupiedPassengersByBookingId(current.getBookingId()) <= 1) {
            System.out.println("Booking phai con it nhat 1 hanh khach co chiem ghe.");
            return false;
        }

        String sql = """
                DELETE FROM BOOKING_PASSENGERS
                WHERE PassengerID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, passengerId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Xoa passenger that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deletePassenger");
        }

        return false;
    }

    public BookingPassenger getPassengerById(int passengerId) {
        if (passengerId <= 0) {
            System.out.println("PassengerID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE PassengerID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, passengerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapBookingPassenger(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getPassengerById");
        }

        return null;
    }

    public List<BookingPassenger> getPassengersByBookingId(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE BookingID = ?
                ORDER BY PassengerID ASC
                """);

        return queryPassengerList(
                sql,
                ps -> ps.setInt(1, bookingId),
                "getPassengersByBookingId"
        );
    }

    public List<BookingPassenger> getPassengersByType(int bookingId, String passengerType) {
        String type = normalizePassengerType(passengerType);

        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return new ArrayList<>();
        }

        if (type == null) {
            System.out.println("PassengerType chi chap nhan ADULT, CHILD, BABY.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE BookingID = ?
                  AND PassengerType = ?
                ORDER BY PassengerID ASC
                """);

        return queryPassengerList(
                sql,
                ps -> {
                    ps.setInt(1, bookingId);
                    ps.setString(2, type);
                },
                "getPassengersByType"
        );
    }

    public int countPassengersByBookingId(int bookingId) {
        if (bookingId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKING_PASSENGERS
                WHERE BookingID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, bookingId),
                "countPassengersByBookingId"
        );
    }

    public int countOccupiedSlotsByBookingId(int bookingId) {
        if (bookingId <= 0) {
            return 0;
        }

        String sql = """
                SELECT ISNULL(SUM(SlotsOccupied), 0) AS Total
                FROM BOOKING_PASSENGERS
                WHERE BookingID = ?
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, bookingId),
                "countOccupiedSlotsByBookingId"
        );
    }

    public BigDecimal getTotalPassengerPriceByBookingId(int bookingId) {
        if (bookingId <= 0) {
            return BigDecimal.ZERO;
        }

        String sql = """
                SELECT ISNULL(SUM(Price), 0) AS Total
                FROM BOOKING_PASSENGERS
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
            handleException(e, "getTotalPassengerPriceByBookingId");
        }

        return BigDecimal.ZERO;
    }

    public boolean hasEnoughSlots(int bookingId, int slotsToAdd) {
        if (bookingId <= 0 || slotsToAdd <= 0) {
            return false;
        }

        String sql = """
                SELECT
                    ts.AvailableSlots,
                    ts.BookedSlots
                FROM BOOKINGS b
                JOIN TOUR_SCHEDULES ts ON ts.ScheduleID = b.ScheduleID
                WHERE b.BookingID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int availableSlots = rs.getInt("AvailableSlots");
                    int bookedSlots = rs.getInt("BookedSlots");
                    return bookedSlots + slotsToAdd <= availableSlots;
                }
            }

        } catch (SQLException e) {
            handleException(e, "hasEnoughSlots");
        }

        return false;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    PassengerID,
                    BookingID,
                    PassengerName,
                    PassengerType,
                    Price,
                    SlotsOccupied,
                    CreatedAt
                FROM BOOKING_PASSENGERS
                """ + condition;
    }

    private List<BookingPassenger> queryPassengerList(String sql,
                                                      SqlSetter setter,
                                                      String methodName) {
        List<BookingPassenger> passengers = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    passengers.add(mapBookingPassenger(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return passengers;
    }

    private BookingPassenger mapBookingPassenger(ResultSet rs) throws SQLException {
        BookingPassenger passenger = new BookingPassenger();

        passenger.setPassengerId(rs.getInt("PassengerID"));
        passenger.setBookingId(rs.getInt("BookingID"));
        passenger.setPassengerName(rs.getString("PassengerName"));
        passenger.setPassengerType(rs.getString("PassengerType"));
        passenger.setPrice(rs.getBigDecimal("Price"));
        passenger.setSlotsOccupied(rs.getInt("SlotsOccupied"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            passenger.setCreatedAt(createdAt.toLocalDateTime());
        }

        return passenger;
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

    private int countOccupiedPassengersByBookingId(int bookingId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKING_PASSENGERS
                WHERE BookingID = ?
                  AND SlotsOccupied > 0
                """;

        return queryCount(
                sql,
                ps -> ps.setInt(1, bookingId),
                "countOccupiedPassengersByBookingId"
        );
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

    private PassengerInput validatePassengerInput(int bookingId,
                                                  String passengerName,
                                                  String passengerType,
                                                  BigDecimal price) {
        String cleanName = cleanString(passengerName);
        String type = normalizePassengerType(passengerType);
        int slotsOccupied = getSlotsOccupiedByType(type);

        if (bookingId <= 0) {
            return PassengerInput.invalid("BookingID khong hop le.");
        }

        if (cleanName == null || cleanName.length() > MAX_PASSENGER_NAME_LENGTH) {
            return PassengerInput.invalid("PassengerName khong hop le, toi da 100 ky tu.");
        }

        if (type == null) {
            return PassengerInput.invalid("PassengerType chi chap nhan ADULT, CHILD, BABY.");
        }

        if (!isValidPriceByType(type, price)) {
            return PassengerInput.invalid("Price khong hop le theo PassengerType.");
        }

        return PassengerInput.valid(
                bookingId,
                cleanName,
                type,
                price,
                slotsOccupied
        );
    }

    private String normalizePassengerType(String passengerType) {
        String type = cleanString(passengerType);

        if (type == null) {
            return null;
        }

        type = type.toUpperCase();

        if (TYPE_ADULT.equals(type)
                || TYPE_CHILD.equals(type)
                || TYPE_BABY.equals(type)) {
            return type;
        }

        return null;
    }

    private int getSlotsOccupiedByType(String passengerType) {
        if (TYPE_BABY.equals(passengerType)) {
            return 0;
        }

        return 1;
    }

    private boolean isValidPriceByType(String passengerType, BigDecimal price) {
        if (price == null) {
            return false;
        }

        if (TYPE_BABY.equals(passengerType)) {
            return price.compareTo(BigDecimal.ZERO) >= 0;
        }

        return price.compareTo(BigDecimal.ZERO) > 0;
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

        if (errorCode == 50001) {
            System.out.println("Loi " + methodName + ": Khong du ghe trong.");
        } else if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": BookingID khong ton tai hoac vi pham khoa ngoai/CHECK constraint.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang BOOKING_PASSENGERS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": PassengerType, Price hoac SlotsOccupied khong hop le.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": PassengerName qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang BOOKING_PASSENGERS.");
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

    private static class PassengerInput {
        private final boolean valid;
        private final String message;
        private final int bookingId;
        private final String passengerName;
        private final String passengerType;
        private final BigDecimal price;
        private final int slotsOccupied;

        private PassengerInput(boolean valid,
                               String message,
                               int bookingId,
                               String passengerName,
                               String passengerType,
                               BigDecimal price,
                               int slotsOccupied) {
            this.valid = valid;
            this.message = message;
            this.bookingId = bookingId;
            this.passengerName = passengerName;
            this.passengerType = passengerType;
            this.price = price;
            this.slotsOccupied = slotsOccupied;
        }

        private static PassengerInput valid(int bookingId,
                                            String passengerName,
                                            String passengerType,
                                            BigDecimal price,
                                            int slotsOccupied) {
            return new PassengerInput(
                    true,
                    null,
                    bookingId,
                    passengerName,
                    passengerType,
                    price,
                    slotsOccupied
            );
        }

        private static PassengerInput invalid(String message) {
            return new PassengerInput(
                    false,
                    message,
                    0,
                    null,
                    null,
                    null,
                    0
            );
        }
    }
}
