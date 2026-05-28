package dao;

import config.DatabaseConnection;
import model.TransportRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;

import java.util.ArrayList;
import java.util.List;

public class TransportRequestDAO {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final int MAX_PARTNER_NAME_LENGTH = 150;
    private static final int MAX_PHONE_LENGTH = 15;
    private static final int MAX_LOCATION_LENGTH = 500;
    private static final int MAX_NOTE_LENGTH = 1000;

    public int createTransportRequest(int scheduleId,
                                      Integer bookingId,
                                      String partnerName,
                                      String contactPhone,
                                      String pickupLocation,
                                      String dropoffLocation,
                                      int passengerCount,
                                      String note,
                                      Integer createdBy) {
        TransportRequestInput input = validateTransportRequestInput(
                scheduleId,
                bookingId,
                partnerName,
                contactPhone,
                pickupLocation,
                dropoffLocation,
                passengerCount,
                note,
                createdBy
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (!isScheduleExists(input.scheduleId)) {
            System.out.println("ScheduleID khong ton tai.");
            return -1;
        }

        if (input.bookingId != null && !isBookingExists(input.bookingId)) {
            System.out.println("BookingID khong ton tai.");
            return -1;
        }

        if (input.createdBy != null && !isUserExists(input.createdBy)) {
            System.out.println("CreatedBy khong ton tai.");
            return -1;
        }

        if (input.bookingId != null && !isBookingBelongsToSchedule(input.bookingId, input.scheduleId)) {
            System.out.println("Booking khong thuoc ScheduleID nay.");
            return -1;
        }

        String sql = """
                INSERT INTO TRANSPORT_REQUESTS
                (
                    ScheduleID,
                    BookingID,
                    PartnerName,
                    ContactPhone,
                    PickupLocation,
                    DropoffLocation,
                    PassengerCount,
                    Note,
                    Status,
                    CreatedBy
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.scheduleId);
            setNullableInt(ps, 2, input.bookingId);
            ps.setString(3, input.partnerName);
            ps.setString(4, input.contactPhone);
            ps.setString(5, input.pickupLocation);
            ps.setString(6, input.dropoffLocation);
            ps.setInt(7, input.passengerCount);
            ps.setString(8, input.note);
            setNullableInt(ps, 9, input.createdBy);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createTransportRequest");
        }

        return -1;
    }

    public TransportRequest getTransportRequestById(int transportRequestId) {
        if (transportRequestId <= 0) {
            System.out.println("TransportRequestID khong hop le.");
            return null;
        }

        String sql = buildSelectSql("""
                WHERE TransportRequestID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, transportRequestId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTransportRequest(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTransportRequestById");
        }

        return null;
    }

    public List<TransportRequest> getTransportRequestsByScheduleId(int scheduleId) {
        if (scheduleId <= 0) {
            System.out.println("ScheduleID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE ScheduleID = ?
                ORDER BY CreatedAt DESC, TransportRequestID DESC
                """);

        return queryTransportRequestList(
                sql,
                ps -> ps.setInt(1, scheduleId),
                "getTransportRequestsByScheduleId"
        );
    }

    public List<TransportRequest> getTransportRequestsByBookingId(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE BookingID = ?
                ORDER BY CreatedAt DESC, TransportRequestID DESC
                """);

        return queryTransportRequestList(
                sql,
                ps -> ps.setInt(1, bookingId),
                "getTransportRequestsByBookingId"
        );
    }

    public List<TransportRequest> getTransportRequestsByStatus(String status) {
        String normalizedStatus = normalizeStatus(status);

        if (normalizedStatus == null) {
            System.out.println("Status chi chap nhan PENDING, SENT, CONFIRMED, CANCELLED.");
            return new ArrayList<>();
        }

        String sql = buildSelectSql("""
                WHERE Status = ?
                ORDER BY CreatedAt DESC, TransportRequestID DESC
                """);

        return queryTransportRequestList(
                sql,
                ps -> ps.setString(1, normalizedStatus),
                "getTransportRequestsByStatus"
        );
    }

    public List<TransportRequest> getAllTransportRequests() {
        String sql = buildSelectSql("""
                ORDER BY CreatedAt DESC, TransportRequestID DESC
                """);

        return queryTransportRequestList(sql, null, "getAllTransportRequests");
    }

    public List<TransportRequest> getOpenTransportRequests() {
        String sql = buildSelectSql("""
                WHERE Status IN ('PENDING', 'SENT')
                ORDER BY CreatedAt ASC, TransportRequestID ASC
                """);

        return queryTransportRequestList(sql, null, "getOpenTransportRequests");
    }

    public boolean updateTransportRequestStatus(int transportRequestId, String newStatus) {
        if (transportRequestId <= 0) {
            System.out.println("TransportRequestID khong hop le.");
            return false;
        }

        String targetStatus = normalizeStatus(newStatus);

        if (targetStatus == null) {
            System.out.println("Status chi chap nhan PENDING, SENT, CONFIRMED, CANCELLED.");
            return false;
        }

        TransportRequest current = getTransportRequestById(transportRequestId);

        if (current == null) {
            System.out.println("Khong tim thay transport request.");
            return false;
        }

        String oldStatus = normalizeStatus(current.getStatus());

        if (!canChangeStatus(oldStatus, targetStatus)) {
            System.out.println("Khong the doi trang thai tu " + oldStatus + " sang " + targetStatus + ".");
            return false;
        }

        String sql = """
                UPDATE TRANSPORT_REQUESTS
                SET Status = ?,
                    UpdatedAt = GETDATE()
                WHERE TransportRequestID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, targetStatus);
            ps.setInt(2, transportRequestId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat status transport request that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateTransportRequestStatus");
        }

        return false;
    }

    public boolean markAsSent(int transportRequestId) {
        return updateTransportRequestStatus(transportRequestId, STATUS_SENT);
    }

    public boolean confirmTransportRequest(int transportRequestId) {
        return updateTransportRequestStatus(transportRequestId, STATUS_CONFIRMED);
    }

    public boolean cancelTransportRequest(int transportRequestId) {
        return updateTransportRequestStatus(transportRequestId, STATUS_CANCELLED);
    }

    public boolean updatePartnerInfo(int transportRequestId,
                                     String partnerName,
                                     String contactPhone,
                                     String note) {
        String cleanPartnerName = cleanString(partnerName);
        String cleanContactPhone = cleanString(contactPhone);
        String cleanNote = cleanString(note);

        if (transportRequestId <= 0) {
            System.out.println("TransportRequestID khong hop le.");
            return false;
        }

        if (cleanPartnerName != null && cleanPartnerName.length() > MAX_PARTNER_NAME_LENGTH) {
            System.out.println("PartnerName qua dai, toi da 150 ky tu.");
            return false;
        }

        if (cleanContactPhone != null && !isValidPhone(cleanContactPhone)) {
            System.out.println("ContactPhone khong hop le. Chi duoc chua so, do dai 10-15 ky tu.");
            return false;
        }

        if (cleanNote != null && cleanNote.length() > MAX_NOTE_LENGTH) {
            System.out.println("Note qua dai, toi da 1000 ky tu.");
            return false;
        }

        TransportRequest current = getTransportRequestById(transportRequestId);

        if (current == null) {
            System.out.println("Khong tim thay transport request.");
            return false;
        }

        if (!canEditDetail(current.getStatus())) {
            System.out.println("Chi duoc sua partner khi request con PENDING hoac SENT.");
            return false;
        }

        String sql = """
                UPDATE TRANSPORT_REQUESTS
                SET PartnerName = ?,
                    ContactPhone = ?,
                    Note = ?,
                    UpdatedAt = GETDATE()
                WHERE TransportRequestID = ?
                  AND Status IN ('PENDING', 'SENT')
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanPartnerName);
            ps.setString(2, cleanContactPhone);
            ps.setString(3, cleanNote);
            ps.setInt(4, transportRequestId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong cap nhat duoc. Request da CONFIRMED/CANCELLED.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updatePartnerInfo");
        }

        return false;
    }

    public boolean updatePickupDropoff(int transportRequestId,
                                       String pickupLocation,
                                       String dropoffLocation) {
        String cleanPickupLocation = cleanString(pickupLocation);
        String cleanDropoffLocation = cleanString(dropoffLocation);

        if (transportRequestId <= 0) {
            System.out.println("TransportRequestID khong hop le.");
            return false;
        }

        if (cleanPickupLocation != null && cleanPickupLocation.length() > MAX_LOCATION_LENGTH) {
            System.out.println("PickupLocation qua dai, toi da 500 ky tu.");
            return false;
        }

        if (cleanDropoffLocation != null && cleanDropoffLocation.length() > MAX_LOCATION_LENGTH) {
            System.out.println("DropoffLocation qua dai, toi da 500 ky tu.");
            return false;
        }

        TransportRequest current = getTransportRequestById(transportRequestId);

        if (current == null) {
            System.out.println("Khong tim thay transport request.");
            return false;
        }

        if (!canEditDetail(current.getStatus())) {
            System.out.println("Chi duoc sua diem don/tra khi request con PENDING hoac SENT.");
            return false;
        }

        String sql = """
                UPDATE TRANSPORT_REQUESTS
                SET PickupLocation = ?,
                    DropoffLocation = ?,
                    UpdatedAt = GETDATE()
                WHERE TransportRequestID = ?
                  AND Status IN ('PENDING', 'SENT')
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanPickupLocation);
            ps.setString(2, cleanDropoffLocation);
            ps.setInt(3, transportRequestId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong cap nhat duoc. Request da CONFIRMED/CANCELLED.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updatePickupDropoff");
        }

        return false;
    }

    public boolean updatePassengerCount(int transportRequestId, int passengerCount) {
        if (transportRequestId <= 0) {
            System.out.println("TransportRequestID khong hop le.");
            return false;
        }

        if (passengerCount <= 0) {
            System.out.println("PassengerCount phai lon hon 0.");
            return false;
        }

        TransportRequest current = getTransportRequestById(transportRequestId);

        if (current == null) {
            System.out.println("Khong tim thay transport request.");
            return false;
        }

        if (!canEditDetail(current.getStatus())) {
            System.out.println("Chi duoc sua so khach khi request con PENDING hoac SENT.");
            return false;
        }

        String sql = """
                UPDATE TRANSPORT_REQUESTS
                SET PassengerCount = ?,
                    UpdatedAt = GETDATE()
                WHERE TransportRequestID = ?
                  AND Status IN ('PENDING', 'SENT')
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, passengerCount);
            ps.setInt(2, transportRequestId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat PassengerCount that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updatePassengerCount");
        }

        return false;
    }

    private String buildSelectSql(String condition) {
        return """
                SELECT
                    TransportRequestID,
                    ScheduleID,
                    BookingID,
                    PartnerName,
                    ContactPhone,
                    PickupLocation,
                    DropoffLocation,
                    PassengerCount,
                    Note,
                    Status,
                    CreatedBy,
                    CreatedAt,
                    UpdatedAt
                FROM TRANSPORT_REQUESTS
                """ + condition;
    }

    private List<TransportRequest> queryTransportRequestList(String sql,
                                                             SqlSetter setter,
                                                             String methodName) {
        List<TransportRequest> requests = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    requests.add(mapTransportRequest(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return requests;
    }

    private TransportRequest mapTransportRequest(ResultSet rs) throws SQLException {
        TransportRequest request = new TransportRequest();

        request.setTransportRequestId(rs.getInt("TransportRequestID"));
        request.setScheduleId(rs.getInt("ScheduleID"));

        int bookingId = rs.getInt("BookingID");
        request.setBookingId(rs.wasNull() ? null : bookingId);

        request.setPartnerName(rs.getString("PartnerName"));
        request.setContactPhone(rs.getString("ContactPhone"));
        request.setPickupLocation(rs.getString("PickupLocation"));
        request.setDropoffLocation(rs.getString("DropoffLocation"));
        request.setPassengerCount(rs.getInt("PassengerCount"));
        request.setNote(rs.getString("Note"));
        request.setStatus(rs.getString("Status"));

        int createdBy = rs.getInt("CreatedBy");
        request.setCreatedBy(rs.wasNull() ? null : createdBy);

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            request.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("UpdatedAt");
        if (updatedAt != null) {
            request.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return request;
    }

    private TransportRequestInput validateTransportRequestInput(int scheduleId,
                                                                Integer bookingId,
                                                                String partnerName,
                                                                String contactPhone,
                                                                String pickupLocation,
                                                                String dropoffLocation,
                                                                int passengerCount,
                                                                String note,
                                                                Integer createdBy) {
        String cleanPartnerName = cleanString(partnerName);
        String cleanContactPhone = cleanString(contactPhone);
        String cleanPickupLocation = cleanString(pickupLocation);
        String cleanDropoffLocation = cleanString(dropoffLocation);
        String cleanNote = cleanString(note);

        if (scheduleId <= 0) {
            return TransportRequestInput.invalid("ScheduleID khong hop le.");
        }

        if (bookingId != null && bookingId <= 0) {
            return TransportRequestInput.invalid("BookingID khong hop le.");
        }

        if (createdBy != null && createdBy <= 0) {
            return TransportRequestInput.invalid("CreatedBy khong hop le.");
        }

        if (cleanPartnerName != null && cleanPartnerName.length() > MAX_PARTNER_NAME_LENGTH) {
            return TransportRequestInput.invalid("PartnerName qua dai, toi da 150 ky tu.");
        }

        if (cleanContactPhone != null && !isValidPhone(cleanContactPhone)) {
            return TransportRequestInput.invalid("ContactPhone khong hop le. Chi duoc chua so, do dai 10-15 ky tu.");
        }

        if (cleanPickupLocation != null && cleanPickupLocation.length() > MAX_LOCATION_LENGTH) {
            return TransportRequestInput.invalid("PickupLocation qua dai, toi da 500 ky tu.");
        }

        if (cleanDropoffLocation != null && cleanDropoffLocation.length() > MAX_LOCATION_LENGTH) {
            return TransportRequestInput.invalid("DropoffLocation qua dai, toi da 500 ky tu.");
        }

        if (passengerCount <= 0) {
            return TransportRequestInput.invalid("PassengerCount phai lon hon 0.");
        }

        if (cleanNote != null && cleanNote.length() > MAX_NOTE_LENGTH) {
            return TransportRequestInput.invalid("Note qua dai, toi da 1000 ky tu.");
        }

        return TransportRequestInput.valid(
                scheduleId,
                bookingId,
                cleanPartnerName,
                cleanContactPhone,
                cleanPickupLocation,
                cleanDropoffLocation,
                passengerCount,
                cleanNote,
                createdBy
        );
    }

    private String normalizeStatus(String status) {
        String value = cleanString(status);

        if (value == null) {
            return null;
        }

        value = value.toUpperCase();

        if (STATUS_PENDING.equals(value)
                || STATUS_SENT.equals(value)
                || STATUS_CONFIRMED.equals(value)
                || STATUS_CANCELLED.equals(value)) {
            return value;
        }

        return null;
    }

    private boolean canChangeStatus(String oldStatus, String newStatus) {
        String oldValue = normalizeStatus(oldStatus);
        String newValue = normalizeStatus(newStatus);

        if (oldValue == null || newValue == null) {
            return false;
        }

        if (oldValue.equals(newValue)) {
            return true;
        }

        if (STATUS_CANCELLED.equals(oldValue)) {
            return false;
        }

        if (STATUS_CONFIRMED.equals(oldValue)) {
            return STATUS_CANCELLED.equals(newValue);
        }

        if (STATUS_PENDING.equals(oldValue)) {
            return STATUS_SENT.equals(newValue)
                    || STATUS_CONFIRMED.equals(newValue)
                    || STATUS_CANCELLED.equals(newValue);
        }

        if (STATUS_SENT.equals(oldValue)) {
            return STATUS_CONFIRMED.equals(newValue)
                    || STATUS_CANCELLED.equals(newValue);
        }

        return false;
    }

    private boolean canEditDetail(String status) {
        String value = normalizeStatus(status);
        return STATUS_PENDING.equals(value) || STATUS_SENT.equals(value);
    }

    private boolean isScheduleExists(int scheduleId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOUR_SCHEDULES
                WHERE ScheduleID = ?
                """;

        return queryCount(sql, ps -> ps.setInt(1, scheduleId), "isScheduleExists") > 0;
    }

    private boolean isBookingExists(int bookingId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS
                WHERE BookingID = ?
                """;

        return queryCount(sql, ps -> ps.setInt(1, bookingId), "isBookingExists") > 0;
    }

    private boolean isUserExists(int userId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM USERS
                WHERE UserID = ?
                """;

        return queryCount(sql, ps -> ps.setInt(1, userId), "isUserExists") > 0;
    }

    private boolean isBookingBelongsToSchedule(int bookingId, int scheduleId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS
                WHERE BookingID = ?
                  AND ScheduleID = ?
                """;

        return queryCount(
                sql,
                ps -> {
                    ps.setInt(1, bookingId);
                    ps.setInt(2, scheduleId);
                },
                "isBookingBelongsToSchedule"
        ) > 0;
    }

    private int queryCount(String sql, SqlSetter setter, String methodName) {
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

    private boolean isValidPhone(String phone) {
        return phone != null
                && phone.matches("\\d{10,15}")
                && phone.length() <= MAX_PHONE_LENGTH;
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void handleException(SQLException e, String methodName) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();

        if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": ScheduleID/BookingID/CreatedBy khong ton tai hoac vi pham khoa ngoai.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": Du lieu transport request bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang TRANSPORT_REQUESTS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang TRANSPORT_REQUESTS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class TransportRequestInput {
        private final boolean valid;
        private final String message;
        private final int scheduleId;
        private final Integer bookingId;
        private final String partnerName;
        private final String contactPhone;
        private final String pickupLocation;
        private final String dropoffLocation;
        private final int passengerCount;
        private final String note;
        private final Integer createdBy;

        private TransportRequestInput(boolean valid,
                                      String message,
                                      int scheduleId,
                                      Integer bookingId,
                                      String partnerName,
                                      String contactPhone,
                                      String pickupLocation,
                                      String dropoffLocation,
                                      int passengerCount,
                                      String note,
                                      Integer createdBy) {
            this.valid = valid;
            this.message = message;
            this.scheduleId = scheduleId;
            this.bookingId = bookingId;
            this.partnerName = partnerName;
            this.contactPhone = contactPhone;
            this.pickupLocation = pickupLocation;
            this.dropoffLocation = dropoffLocation;
            this.passengerCount = passengerCount;
            this.note = note;
            this.createdBy = createdBy;
        }

        private static TransportRequestInput valid(int scheduleId,
                                                   Integer bookingId,
                                                   String partnerName,
                                                   String contactPhone,
                                                   String pickupLocation,
                                                   String dropoffLocation,
                                                   int passengerCount,
                                                   String note,
                                                   Integer createdBy) {
            return new TransportRequestInput(
                    true,
                    null,
                    scheduleId,
                    bookingId,
                    partnerName,
                    contactPhone,
                    pickupLocation,
                    dropoffLocation,
                    passengerCount,
                    note,
                    createdBy
            );
        }

        private static TransportRequestInput invalid(String message) {
            return new TransportRequestInput(
                    false,
                    message,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    null,
                    null
            );
        }
    }
}
