package dao;

import config.DatabaseConnection;
import model.ETicket;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ETicketDAO {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_USED = "USED";
    private static final String STATUS_EXPIRED = "EXPIRED";

    public ETicket getTicketById(int ticketId) {
        if (ticketId <= 0) {
            System.out.println("TicketID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE TicketID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, ticketId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapETicket(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTicketById");
        }

        return null;
    }

    public ETicket getTicketByBookingId(int bookingId) {
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
                    return mapETicket(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTicketByBookingId");
        }

        return null;
    }

    public ETicket getTicketByCode(String ticketCode) {
        String cleanTicketCode = cleanString(ticketCode);

        if (cleanTicketCode == null) {
            System.out.println("TicketCode khong duoc de trong.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE TicketCode = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanTicketCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapETicket(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTicketByCode");
        }

        return null;
    }

    public List<ETicket> getTicketsByUserId(int userId) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return new ArrayList<>();
        }

        String sql = """
                SELECT
                    et.TicketID,
                    et.BookingID,
                    et.TicketCode,
                    et.QRCode,
                    et.TicketStatus,
                    et.IssuedDate,
                    et.ExpiryDate,
                    et.CreatedAt
                FROM E_TICKETS et
                JOIN BOOKINGS b ON b.BookingID = et.BookingID
                WHERE b.UserID = ?
                ORDER BY et.IssuedDate DESC, et.TicketID DESC
                """;

        return queryTicketList(
                sql,
                ps -> ps.setInt(1, userId),
                "getTicketsByUserId"
        );
    }

    public List<ETicket> getActiveTicketsByUserId(int userId) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return new ArrayList<>();
        }

        String sql = """
                SELECT
                    et.TicketID,
                    et.BookingID,
                    et.TicketCode,
                    et.QRCode,
                    et.TicketStatus,
                    et.IssuedDate,
                    et.ExpiryDate,
                    et.CreatedAt
                FROM E_TICKETS et
                JOIN BOOKINGS b ON b.BookingID = et.BookingID
                WHERE b.UserID = ?
                  AND et.TicketStatus = 'ACTIVE'
                  AND (et.ExpiryDate IS NULL OR et.ExpiryDate > GETDATE())
                ORDER BY et.IssuedDate DESC, et.TicketID DESC
                """;

        return queryTicketList(
                sql,
                ps -> ps.setInt(1, userId),
                "getActiveTicketsByUserId"
        );
    }

    public List<ETicket> getTicketsByStatus(String ticketStatus) {
        String status = normalizeTicketStatus(ticketStatus);

        if (status == null) {
            System.out.println("TicketStatus chi chap nhan ACTIVE, USED, EXPIRED.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE TicketStatus = ?
                ORDER BY IssuedDate DESC, TicketID DESC
                """);

        return queryTicketList(
                sql,
                ps -> ps.setString(1, status),
                "getTicketsByStatus"
        );
    }

    public List<ETicket> getExpiredActiveTickets() {
        String sql = buildSelectSql("""
                WHERE TicketStatus = 'ACTIVE'
                  AND ExpiryDate IS NOT NULL
                  AND ExpiryDate <= GETDATE()
                ORDER BY ExpiryDate ASC, TicketID ASC
                """);

        return queryTicketList(sql, null, "getExpiredActiveTickets");
    }

    public boolean markTicketUsed(String ticketCode) {
        String cleanTicketCode = cleanString(ticketCode);

        if (cleanTicketCode == null) {
            System.out.println("TicketCode khong hop le.");
            return false;
        }

        if (!isTicketValid(cleanTicketCode)) {
            System.out.println("Ticket khong hop le, da het han hoac khong ACTIVE.");
            return false;
        }

        String sql = """
                UPDATE E_TICKETS
                SET TicketStatus = 'USED'
                WHERE TicketCode = ?
                  AND TicketStatus = 'ACTIVE'
                  AND (ExpiryDate IS NULL OR ExpiryDate > GETDATE())
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanTicketCode);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong cap nhat duoc ticket sang USED.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "markTicketUsed");
        }

        return false;
    }

    public boolean markTicketUsedById(int ticketId) {
        if (ticketId <= 0) {
            System.out.println("TicketID khong hop le.");
            return false;
        }

        ETicket ticket = getTicketById(ticketId);

        if (ticket == null) {
            System.out.println("Khong tim thay ticket.");
            return false;
        }

        return markTicketUsed(ticket.getTicketCode());
    }

    public boolean expireTicketByBookingId(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return false;
        }

        String sql = """
                UPDATE E_TICKETS
                SET TicketStatus = 'EXPIRED'
                WHERE BookingID = ?
                  AND TicketStatus = 'ACTIVE'
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong co ticket ACTIVE de expire.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "expireTicketByBookingId");
        }

        return false;
    }

    public int expireOldActiveTickets() {
        String sql = """
                UPDATE E_TICKETS
                SET TicketStatus = 'EXPIRED'
                WHERE TicketStatus = 'ACTIVE'
                  AND ExpiryDate IS NOT NULL
                  AND ExpiryDate <= GETDATE()
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            return ps.executeUpdate();

        } catch (SQLException e) {
            handleException(e, "expireOldActiveTickets");
        }

        return 0;
    }

    public boolean isTicketValid(String ticketCode) {
        String cleanTicketCode = cleanString(ticketCode);

        if (cleanTicketCode == null) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM E_TICKETS
                WHERE TicketCode = ?
                  AND TicketStatus = 'ACTIVE'
                  AND (ExpiryDate IS NULL OR ExpiryDate > GETDATE())
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanTicketCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total") > 0;
                }
            }

        } catch (SQLException e) {
            handleException(e, "isTicketValid");
        }

        return false;
    }

    /*
     * Vé được SQL trigger tự tạo sau khi payment SUCCESS.
     * Hàm này chỉ dùng để kiểm tra booking đã có vé chưa.
     */
    public boolean hasTicketForBooking(int bookingId) {
        if (bookingId <= 0) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM E_TICKETS
                WHERE BookingID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, bookingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total") > 0;
                }
            }

        } catch (SQLException e) {
            handleException(e, "hasTicketForBooking");
        }

        return false;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    TicketID,
                    BookingID,
                    TicketCode,
                    QRCode,
                    TicketStatus,
                    IssuedDate,
                    ExpiryDate,
                    CreatedAt
                FROM E_TICKETS
                """ + condition;
    }

    private List<ETicket> queryTicketList(String sql,
                                          SqlSetter setter,
                                          String methodName) {
        List<ETicket> tickets = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tickets.add(mapETicket(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return tickets;
    }

    private ETicket mapETicket(ResultSet rs) throws SQLException {
        ETicket ticket = new ETicket();

        ticket.setTicketId(rs.getInt("TicketID"));
        ticket.setBookingId(rs.getInt("BookingID"));
        ticket.setTicketCode(rs.getString("TicketCode"));
        ticket.setQrCode(rs.getString("QRCode"));
        ticket.setTicketStatus(rs.getString("TicketStatus"));

        Timestamp issuedDate = rs.getTimestamp("IssuedDate");
        if (issuedDate != null) {
            ticket.setIssuedDate(issuedDate.toLocalDateTime());
        }

        Timestamp expiryDate = rs.getTimestamp("ExpiryDate");
        if (expiryDate != null) {
            ticket.setExpiryDate(expiryDate.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            ticket.setCreatedAt(createdAt.toLocalDateTime());
        }

        return ticket;
    }

    private String normalizeTicketStatus(String ticketStatus) {
        String status = cleanString(ticketStatus);

        if (status == null) {
            return null;
        }

        status = status.toUpperCase();

        if (status.equals(STATUS_ACTIVE)
                || status.equals(STATUS_USED)
                || status.equals(STATUS_EXPIRED)) {
            return status;
        }

        return null;
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
            System.out.println("Loi " + methodName + ": BookingID khong ton tai hoac vi pham khoa ngoai.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": TicketCode hoac BookingID da ton tai.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang E_TICKETS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot trong database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang E_TICKETS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }
}
